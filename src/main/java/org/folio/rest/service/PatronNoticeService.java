package org.folio.rest.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;
import static org.apache.commons.lang3.StringUtils.wrap;
import static org.folio.rest.domain.logs.LogEventPayloadField.DATE;
import static org.folio.rest.domain.logs.LogEventPayloadHelper.setErrorMessage;
import static org.folio.rest.domain.logs.LogEventPayloadHelper.buildNoticeErrorLogEventPayload;
import static org.folio.rest.domain.logs.LogEventPayloadHelper.buildNoticeLogEventPayload;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.NOTICE;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.NOTICE_ERROR;
import static org.folio.rest.utils.FeeFineActionHelper.isAction;
import static org.folio.rest.utils.FeeFineActionHelper.isCharge;
import static org.folio.util.UuidUtil.isUuid;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.PatronNoticeClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.exception.EntityNotFoundException;
import org.folio.rest.exception.InvalidIdException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.LoanType;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.repository.FeeFineRepository;
import org.folio.rest.repository.OwnerRepository;
import org.folio.rest.service.LogEventPublisher.LogEventPayloadType;
import org.folio.rest.utils.PatronNoticeBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class PatronNoticeService {
  private static final Logger logger = LogManager.getLogger(PatronNoticeService.class);
  private static final String ERROR_MESSAGE_WRAPPER = "\"";
  private static final String ERROR_MESSAGE_SEPARATOR = ", ";

  private final LocationService locationService;
  private final FeeFineRepository feeFineRepository;
  private final OwnerRepository ownerRepository;
  private final AccountRepository accountRepository;
  private final FeeFineActionRepository feeFineActionRepository;
  private final PatronNoticeClient patronNoticeClient;
  private final UsersClient usersClient;
  private final InventoryClient inventoryClient;
  private final LogEventPublisher logEventPublisher;

  public PatronNoticeService(Vertx vertx, Map<String, String> okapiHeaders) {
    PostgresClient pgClient = PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders);

    locationService = new LocationService(vertx, okapiHeaders);

    feeFineRepository = new FeeFineRepository(pgClient);
    ownerRepository = new OwnerRepository(pgClient);
    accountRepository = new AccountRepository(pgClient);
    feeFineActionRepository = new FeeFineActionRepository(okapiHeaders, vertx.getOrCreateContext());

    patronNoticeClient = new PatronNoticeClient(vertx, okapiHeaders);
    usersClient = new UsersClient(vertx, okapiHeaders);
    inventoryClient = new InventoryClient(vertx, okapiHeaders);

    logEventPublisher = new LogEventPublisher(vertx, okapiHeaders);
  }

  public Future<Void> sendPatronNotice(Feefineaction action) {
    return succeededFuture(action)
      .map(this::createContext)
      .compose(this::fetchAccount)
      .compose(this::fetchFeeFine)
      .compose(this::fetchOwner)
      .compose(this::sendNoticeWhenTemplateIsSet)
      .onSuccess(this::handleSuccess)
      .onFailure(t -> handleFailure(t, action))
      .compose(this::handleCapturedErrors);
  }

  private FeeFineNoticeContext createContext(Feefineaction action) {
    if (action == null) {
      throw new IllegalArgumentException("Fee/fine action is null");
    }

    if (isAction(action)) {
      return new FeeFineNoticeContext().withAction(action);
    }

    if (isCharge(action)) {
      return new FeeFineNoticeContext().withCharge(action);
    }

    throw new IllegalArgumentException(String.format(
      "Fee/fine action %s object appears to be neither charge nor action. Action type: %s",
      action.getId(), action.getTypeAction()));
  }

  private Future<FeeFineNoticeContext> fetchAccount(FeeFineNoticeContext context) {
    String accountId = ofNullable(context.getPrimaryAction())
      .map(Feefineaction::getAccountId)
      .orElse(null);

    return validateId(accountId, Account.class)
      .compose(accountRepository::getAccountById)
      .compose(account -> failWhenNotFound(account, Account.class, accountId))
      .map(context::withAccount);
  }

  private Future<FeeFineNoticeContext> fetchFeeFine(FeeFineNoticeContext context) {
    String feeFineId = ofNullable(context.getAccount())
      .map(Account::getFeeFineId)
      .orElse(null);

    return validateId(feeFineId, Feefine.class)
      .compose(feeFineRepository::getById)
      .compose(ff -> failWhenNotFound(ff, Feefine.class, feeFineId))
      .map(context::withFeefine);
  }

  public Future<FeeFineNoticeContext> fetchOwner(FeeFineNoticeContext context) {
    if (context.isTemplateSet()) {
      // template is set in fee/fine type, no need to fetch the owner
      return succeededFuture(context);
    }

    String ownerId = ofNullable(context.getFeefine())
      .map(Feefine::getOwnerId)
      .orElse(null);

    return validateId(ownerId, Owner.class)
      .compose(ownerRepository::getById)
      .compose(owner -> failWhenNotFound(owner, Owner.class, ownerId))
      .map(context::withOwner);
  }

  private Future<FeeFineNoticeContext> sendNoticeWhenTemplateIsSet(FeeFineNoticeContext context) {
    if (!context.isTemplateSet()) {
      logger.info("Patron notice template is not set, doing nothing");
      return succeededFuture(context);
    }

    return succeededFuture(context)
      .compose(this::fetchCharge)
      .compose(this::fetchUser)
      .compose(this::fetchItemAndRelatedRecords)
      .map(this::buildLogEventPayload)
      .compose(this::sendPatronNotice);
  }

  private Future<FeeFineNoticeContext> fetchCharge(FeeFineNoticeContext context) {
    if (context.getCharge() != null) {
      return succeededFuture(context);
    }

    String accountId = ofNullable(context.getAction())
      .map(Feefineaction::getAccountId)
      .orElse(null);

    return validateId(accountId, Feefineaction.class)
      .compose(feeFineActionRepository::findChargeForAccount)
      .compose(ff -> failWhenNotFound(ff, Feefineaction.class, null))
      .otherwise(t -> captureError(t, context))
      .map(context::withCharge);
  }

  private Future<FeeFineNoticeContext> fetchUser(FeeFineNoticeContext context) {
    String userId = context.getUserId();

    return validateId(userId, User.class)
      .compose(usersClient::fetchUserById)
      .otherwise(t -> captureError(t, context))
      .map(context::withUser);
  }

  private Future<FeeFineNoticeContext> fetchItemAndRelatedRecords(FeeFineNoticeContext context) {
    if (context.getAccount().getItemId() == null) {
      return succeededFuture(context);
    }

    return succeededFuture(context)
      .compose(this::fetchItem)
      .compose(this::fetchHolding)
      .compose(this::fetchInstance)
      .compose(this::fetchLocation)
      .compose(this::fetchLoanType);
  }

  private Future<FeeFineNoticeContext> fetchItem(FeeFineNoticeContext context) {
    String itemId = ofNullable(context.getAccount())
      .map(Account::getItemId)
      .orElse(null);

    return validateId(itemId, Item.class)
      .compose(inventoryClient::getItemById)
      .otherwise(t -> captureError(t, context))
      .map(context::withItem);
  }

  private Future<FeeFineNoticeContext> fetchHolding(FeeFineNoticeContext context) {
    String holdingsRecordId = ofNullable(context.getAccount())
      .map(Account::getHoldingsRecordId)
      .orElseGet(() -> ofNullable(context.getItem())
        .map(Item::getHoldingsRecordId)
        .orElse(null));

    return validateId(holdingsRecordId, HoldingsRecord.class)
      .compose(inventoryClient::getHoldingById)
      .otherwise(t -> captureError(t, context))
      .map(context::withHoldingsRecord);
  }

  private Future<FeeFineNoticeContext> fetchInstance(FeeFineNoticeContext context) {
    String instanceId = ofNullable(context.getAccount())
      .map(Account::getInstanceId)
      .orElseGet(() -> ofNullable(context.getHoldingsRecord())
        .map(HoldingsRecord::getInstanceId)
        .orElse(null));

    return validateId(instanceId, Instance.class)
      .compose(inventoryClient::getInstanceById)
      .otherwise(t -> captureError(t, context))
      .map(context::withInstance);
  }

  private Future<FeeFineNoticeContext> fetchLocation(FeeFineNoticeContext context) {
    String locationId = ofNullable(context.getItem())
      .map(Item::getEffectiveLocationId)
      .orElse(null);

    return validateId(locationId, Location.class)
      .compose(locationService::getEffectiveLocation)
      .otherwise(t -> captureError(t, context))
      .map(context::withEffectiveLocation);
  }

  private Future<FeeFineNoticeContext> fetchLoanType(FeeFineNoticeContext context) {
    String loanTypeId = ofNullable(context.getItem())
      .map(item -> firstNonBlank(item.getTemporaryLoanTypeId(), item.getPermanentLoanTypeId()))
      .orElse(null);

    return validateId(loanTypeId,LoanType.class)
      .compose(inventoryClient::getLoanTypeById)
      .otherwise(t -> captureError(t, context))
      .map(context::withLoanType);
  }

  private FeeFineNoticeContext buildLogEventPayload(FeeFineNoticeContext context) {
    return context.withLogEventPayload(buildNoticeLogEventPayload(context));
  }

  private Future<FeeFineNoticeContext> sendPatronNotice(FeeFineNoticeContext context) {
    return succeededFuture(context)
      .map(PatronNoticeBuilder::buildNotice)
      .compose(patronNoticeClient::postPatronNotice)
      .map(context);
  }

  private static Future<String> validateId(String id, Class<?> entityType) {
    return isUuid(id)
      ? succeededFuture(id)
      : failedFuture(new InvalidIdException(entityType, id));
  }

  private static <T> Future<T> failWhenNotFound(T entity, Class<?> entityType, String entityId) {
    return entity == null
      ? failedFuture(new EntityNotFoundException(entityType, entityId))
      : succeededFuture(entity);
  }

  private static <T> T captureError(Throwable throwable, FeeFineNoticeContext context) {
    context.getErrors().add(throwable);
    logger.error("An error was captured while preparing patron notice: {}", throwable.getMessage());

    return null;
  }

  private Future<Void> handleCapturedErrors(FeeFineNoticeContext context) {
    if (context.getErrors().isEmpty()) {
      return succeededFuture();
    }

    String errorMessage = "Following errors may result in missing token values: " +
      joinCapturedErrorMessages(context);

    logger.error(errorMessage);
    setErrorMessage(context.getLogEventPayload(), errorMessage);

    return publishLogEvent(context, NOTICE_ERROR);
  }

  private static String joinCapturedErrorMessages(FeeFineNoticeContext context) {
    return context.getErrors().stream()
      .map(Throwable::getMessage)
      .filter(StringUtils::isNotBlank)
      .map(errorMessage -> wrap(errorMessage, ERROR_MESSAGE_WRAPPER))
      .collect(joining(ERROR_MESSAGE_SEPARATOR));
  }

  private Future<Void> publishLogEvent(FeeFineNoticeContext context, LogEventPayloadType eventType) {
    return publishLogEvent(context.getLogEventPayload(), eventType);
  }

  private Future<Void> publishLogEvent(JsonObject logEventPayload, LogEventPayloadType eventType) {
    logEventPayload.put(DATE.value(), DateTime.now().toString(ISODateTimeFormat.dateTime()));
    CompletableFuture.runAsync(() -> logEventPublisher.publishLogEvent(logEventPayload, eventType));

    return succeededFuture();
  }

  private void handleSuccess(FeeFineNoticeContext context) {
    if (context.isTemplateSet()) {
      logger.info("Patron notice was successfully sent");
      publishLogEvent(context, NOTICE);
    }
  }

  private void handleFailure(Throwable throwable, Feefineaction action) {
    logger.error("Failed to send patron notice: {}", throwable.getMessage());
    publishLogEvent(buildNoticeErrorLogEventPayload(throwable, action), NOTICE_ERROR);
  }

}
