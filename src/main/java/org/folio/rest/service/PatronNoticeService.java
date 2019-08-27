package org.folio.rest.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Map;
import java.util.Optional;

import org.folio.rest.client.PatronNoticeClient;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.jaxrs.model.PatronNotice;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineRepository;
import org.folio.rest.repository.OwnerRepository;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class PatronNoticeService {

  private static final Logger logger = LoggerFactory.getLogger(PatronNoticeService.class);

  private static final String PAID_FULLY = "Paid fully";
  private static final String PAID_PARTIALLY = "Paid partially";

  private FeeFineRepository feeFineRepository;
  private OwnerRepository ownerRepository;
  private AccountRepository accountRepository;
  private PatronNoticeClient patronNoticeClient;

  public PatronNoticeService(Vertx vertx, Map<String, String> okapiHeaders) {
    PostgresClient pgClient = PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders);
    feeFineRepository = new FeeFineRepository(pgClient);
    ownerRepository = new OwnerRepository(pgClient);
    accountRepository = new AccountRepository(pgClient);
    patronNoticeClient = new PatronNoticeClient(WebClient.create(vertx), okapiHeaders);
  }

  public void sendPatronNotice(Feefineaction action) {
    accountRepository.getById(action.getAccountId())
      .map(Account::getFeeFineId)
      .compose(feeFineRepository::getById)
      .compose(feefine -> fetchTemplateId(feefine, action))
      .compose(templateId -> isEmpty(templateId) ?
        failedFuture("Template not set") :
        succeededFuture(templateId))
      .map(templateId -> createNotice(action.getUserId(), templateId))
      .compose(patronNoticeClient::postPatronNotice)
      .setHandler(send -> {
        if (send.failed()) {
          logger.error("Patron notice failed to send or template is not set", send.cause());
        } else {
          logger.info("Patron notice has been successfully sent");
        }
      });
  }

  private Future<String> fetchTemplateId(Feefine feefine, Feefineaction action) {
    if (action.getPaymentMethod() == null) {
      return fetchChargeTemplateId(feefine);
    } else if (action.getTypeAction().equals(PAID_FULLY) || action.getTypeAction().equals(PAID_PARTIALLY)) {
      return fetchActionTemplateId(feefine);
    }
    return null;
  }

  private Future<String> fetchChargeTemplateId(Feefine feefine) {
    return Optional.ofNullable(feefine.getChargeNoticeId())
      .map(Future::succeededFuture)
      .orElseGet(() -> ownerRepository.getById(feefine.getOwnerId())
        .map(Owner::getDefaultChargeNoticeId));
  }

  private Future<String> fetchActionTemplateId(Feefine feefine) {
    return Optional.ofNullable(feefine.getActionNoticeId())
      .map(Future::succeededFuture)
      .orElseGet(() -> ownerRepository.getById(feefine.getOwnerId())
        .map(Owner::getDefaultActionNoticeId));
  }

  private PatronNotice createNotice(String userId, String templateId) {
    return new PatronNotice()
      .withDeliveryChannel("email")
      .withOutputFormat("text/html")
      .withRecipientId(userId)
      .withTemplateId(templateId);
  }
}
