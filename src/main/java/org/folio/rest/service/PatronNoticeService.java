package org.folio.rest.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.FeeFineActionHelper.isAction;
import static org.folio.rest.utils.FeeFineActionHelper.isCharge;
import static org.folio.util.UuidUtil.isUuid;

import java.util.Map;
import java.util.Optional;

import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.PatronNoticeClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.repository.FeeFineRepository;
import org.folio.rest.repository.OwnerRepository;
import org.folio.rest.utils.PatronNoticeBuilder;
import org.folio.util.UuidUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class PatronNoticeService {
  private static final Logger logger = LoggerFactory.getLogger(PatronNoticeService.class);

  private final FeeFineRepository feeFineRepository;
  private final OwnerRepository ownerRepository;
  private final AccountRepository accountRepository;
  private final FeeFineActionRepository feeFineActionRepository;
  private final PatronNoticeClient patronNoticeClient;
  private final UsersClient usersClient;
  private final InventoryClient inventoryClient;

  public PatronNoticeService(Vertx vertx, Map<String, String> okapiHeaders) {
    PostgresClient pgClient = PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders);
    WebClient webClient = WebClient.create(vertx);

    feeFineRepository = new FeeFineRepository(pgClient);
    ownerRepository = new OwnerRepository(pgClient);
    accountRepository = new AccountRepository(pgClient);
    feeFineActionRepository = new FeeFineActionRepository(pgClient);

    patronNoticeClient = new PatronNoticeClient(webClient, okapiHeaders);
    usersClient = new UsersClient(vertx, okapiHeaders);
    inventoryClient = new InventoryClient(webClient, okapiHeaders);
  }

  public void sendPatronNotice(Feefineaction action) {
    createContext(action)
      .compose(this::loadChargeIfMissing)
      .compose(accountRepository::loadAccount)
      .compose(feeFineRepository::loadFeefine)
      .compose(ownerRepository::loadOwner)
      .compose(this::refuseWhenEmptyTemplateId)
      .compose(this::fetchUser)
      .compose(this::fetchItem)
      .compose(this::fetchHolding)
      .compose(this::fetchInstance)
      .compose(this::fetchLocation)
      .map(PatronNoticeBuilder::buildNotice)
      .compose(patronNoticeClient::postPatronNotice)
      .onComplete(this::handleSendPatronNoticeResult);
  }

  private Future<FeeFineNoticeContext> createContext(Feefineaction action) {
    FeeFineNoticeContext context = new FeeFineNoticeContext();

    if (isAction(action)) {
      context = context.withAction(action);
    }
    else if (isCharge(action)) {
      context = context.withCharge(action);
    }

    return succeededFuture(context);
  }

  private Future<FeeFineNoticeContext> loadChargeIfMissing(FeeFineNoticeContext context) {
    final Feefineaction action = context.getAction();

    if (context.getCharge() != null || action == null) {
      return succeededFuture(context);
    }

    return feeFineActionRepository.findChargeForAccount(action.getAccountId())
      .map(context::withCharge);
  }

  private Future<FeeFineNoticeContext> fetchUser(FeeFineNoticeContext context) {
    return usersClient.fetchUserById(context.getUserId())
      .map(context::withUser);
  }

  private Future<FeeFineNoticeContext> fetchItem(FeeFineNoticeContext context) {
    final String itemId = context.getAccount().getItemId();

    if (!isUuid(itemId)) {
      return succeededFuture(context);
    }

    return inventoryClient.getItemById(itemId)
      .map(context::withItem);
  }

  private Future<FeeFineNoticeContext> fetchHolding(FeeFineNoticeContext context) {
    final String holdingsRecordId = Optional.ofNullable(context.getAccount())
      .map(Account::getHoldingsRecordId)
      .filter(UuidUtil::isUuid)
      .orElseGet(() -> Optional.ofNullable(context.getItem())
        .map(Item::getHoldingsRecordId)
        .orElse(null));

    if (!isUuid(holdingsRecordId)) {
      return succeededFuture(context);
    }

    return inventoryClient.getHoldingById(holdingsRecordId)
      .map(context::withHoldingsRecord);
  }

  private Future<FeeFineNoticeContext> fetchInstance(FeeFineNoticeContext context) {
    final String instanceId = Optional.ofNullable(context.getAccount())
      .map(Account::getInstanceId)
      .filter(UuidUtil::isUuid)
      .orElseGet(() -> Optional.ofNullable(context.getHoldingsRecord())
        .map(HoldingsRecord::getInstanceId)
        .orElse(null));

    if (!isUuid(instanceId)) {
      return succeededFuture(context);
    }

    return inventoryClient.getInstanceById(instanceId)
      .map(context::withInstance);
  }

  private Future<FeeFineNoticeContext> fetchLocation(FeeFineNoticeContext context) {
    final Item item = context.getItem();

    if (item == null || !isUuid(item.getEffectiveLocationId())) {
      return succeededFuture(context);
    }

    return inventoryClient.getLocationById(item.getEffectiveLocationId())
      .compose(this::fetchInstitution)
      .compose(this::fetchLibrary)
      .compose(this::fetchCampus)
      .map(context::withEffectiveLocation);
  }

  private Future<Location> fetchInstitution(Location location) {
    final String institutionId = location.getInstitutionId();

    if (!isUuid(institutionId)) {
      return succeededFuture(location);
    }

    return inventoryClient.getInstitutionById(institutionId)
      .map(location::withInstitution);
  }

  private Future<Location> fetchLibrary(Location location) {
    final String libraryId = location.getLibraryId();

    if (!isUuid(libraryId)) {
      return succeededFuture(location);
    }

    return inventoryClient.getLibraryById(libraryId)
      .map(location::withLibrary);
  }

  private Future<Location> fetchCampus(Location location) {
    final String campusId = location.getCampusId();

    if (!isUuid(campusId)) {
      return succeededFuture(location);
    }

    return inventoryClient.getCampusById(campusId)
      .map(location::withCampus);
  }

  private Future<FeeFineNoticeContext> refuseWhenEmptyTemplateId(FeeFineNoticeContext ctx) {
    return ctx.getTemplateId() == null ?
      failedFuture("Template not set") : succeededFuture(ctx);
  }

  private void handleSendPatronNoticeResult(AsyncResult<Void> post) {
    if (post.failed()) {
      logger.error("Patron notice failed to send or template is not set", post.cause());
    } else {
      logger.info("Patron notice has been successfully sent");
    }
  }
}
