package org.folio.rest.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.FeeFineActionCommentsParser.parseFeeFineComments;

import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.client.PatronNoticeClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Context;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.PatronNotice;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineRepository;
import org.folio.rest.repository.OwnerRepository;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class PatronNoticeService {

  private static final Logger logger = LoggerFactory.getLogger(PatronNoticeService.class);

  private static final String PATRON_COMMENTS_KEY = "PATRON";
  private static final String STAFF_COMMENTS_KEY = "STAFF";

  private FeeFineRepository feeFineRepository;
  private OwnerRepository ownerRepository;
  private AccountRepository accountRepository;
  private PatronNoticeClient patronNoticeClient;
  private UsersClient usersClient;

  public PatronNoticeService(Vertx vertx, Map<String, String> okapiHeaders) {
    PostgresClient pgClient = PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders);
    feeFineRepository = new FeeFineRepository(pgClient);
    ownerRepository = new OwnerRepository(pgClient);
    accountRepository = new AccountRepository(pgClient);
    patronNoticeClient = new PatronNoticeClient(WebClient.create(vertx), okapiHeaders);
    usersClient = new UsersClient(vertx, okapiHeaders);
  }

  public void sendPatronNotice(Feefineaction feefineaction) {
    succeededFuture(new FeeFineNoticeContext().withFeefineaction(feefineaction))
      .compose(accountRepository::loadAccount)
      .compose(feeFineRepository::loadFeefine)
      .compose(ownerRepository::loadOwner)
      .compose(this::refuseWhenEmptyTemplateId)
      .compose(this::fetchUser)
      .map(this::createNotice)
      .compose(patronNoticeClient::postPatronNotice)
      .onComplete(this::handleSendPatronNoticeResult);
  }

  private Future<FeeFineNoticeContext> fetchUser(FeeFineNoticeContext context) {
    return usersClient.fetchUserById(context.getUserId())
      .map(context::withUser);
  }

  private Future<FeeFineNoticeContext> refuseWhenEmptyTemplateId(FeeFineNoticeContext ctx) {
    return ctx.getTemplateId() == null ?
      failedFuture("Template not set") : succeededFuture(ctx);
  }

  private PatronNotice createNotice(FeeFineNoticeContext ctx) {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    df.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));

    Feefineaction feefineaction = ctx.getFeefineaction();
    String actionDateTime = Optional.ofNullable(feefineaction.getDateAction())
      .map(df::format)
      .orElse(null);

    return new PatronNotice()
      .withDeliveryChannel("email")
      .withOutputFormat("text/html")
      .withRecipientId(ctx.getUserId())
      .withTemplateId(ctx.getTemplateId())
      .withContext(new Context()
        .withAdditionalProperty("fee", new JsonObject()
          .put("owner", ctx.getOwner().getOwner())
          .put("type", ctx.getFeefine().getFeeFineType())
          .put("amount", ctx.getAccount().getAmount())
          .put("actionType", feefineaction.getTypeAction())
          .put("actionAmount", feefineaction.getAmountAction())
          .put("actionDateTime", actionDateTime)
          .put("balance", feefineaction.getBalance())
          .put("actionAdditionalInfo", getCommentsFromFeeFineAction(feefineaction, PATRON_COMMENTS_KEY))
          .put("reasonForCancellation", getCommentsFromFeeFineAction(feefineaction, STAFF_COMMENTS_KEY)))
        .withAdditionalProperty("user", buildUserContext(ctx.getUser()))
      );
  }

  private JsonObject buildUserContext(User user) {
    JsonObject userContext = new JsonObject()
      .put("barcode", user.getBarcode());

    Personal personal = user.getPersonal();
    if (user.getPersonal() != null) {
      userContext
        .put("firstName", personal.getFirstName())
        .put("lastName", personal.getLastName())
        .put("middleName", personal.getMiddleName());
    }
    return userContext;
  }

  private String getCommentsFromFeeFineAction(Feefineaction feefineaction, String commentsKey){
    String comments = Optional.ofNullable(feefineaction.getComments()).orElse(StringUtils.EMPTY);
    return parseFeeFineComments(comments).getOrDefault(commentsKey, StringUtils.EMPTY);
  }

  private void handleSendPatronNoticeResult(AsyncResult<Void> post) {
    if (post.failed()) {
      logger.error("Patron notice failed to send or template is not set", post.cause());
    } else {
      logger.info("Patron notice has been successfully sent");
    }
  }
}
