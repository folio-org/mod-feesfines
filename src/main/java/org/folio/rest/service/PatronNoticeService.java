package org.folio.rest.service;

import java.util.Map;
import java.util.Optional;

import org.folio.rest.client.PatronNoticeClient;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.jaxrs.model.PatronNotice;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.FeeFineRepository;
import org.folio.rest.repository.OwnerRepository;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class PatronNoticeService {

  private static final Logger logger = LoggerFactory.getLogger(PatronNoticeService.class);

  private FeeFineRepository feeFineRepository;
  private OwnerRepository ownerRepository;
  private PatronNoticeClient patronNoticeClient;

  public PatronNoticeService(Vertx vertx, Map<String, String> okapiHeaders) {
    PostgresClient pgClient = PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders);
    feeFineRepository = new FeeFineRepository(pgClient);
    ownerRepository = new OwnerRepository(pgClient);
    patronNoticeClient = new PatronNoticeClient(WebClient.create(vertx), okapiHeaders);
  }

  public void sendManualChargeNotice(Account account) {
    feeFineRepository.getById(account.getFeeFineId())
      .compose(this::fetchChargeTemplateId)
      .map(templateId -> createNotice(account.getUserId(), templateId))
      .compose(patronNoticeClient::postPatronNotice)
      .setHandler(send -> {
        if (send.failed()) {
          logger.error("Failed to send patron notice", send.cause());
        } else {
          logger.info("Patron notice has been successfully sent");
        }
      });
  }

  public void sendActionNotice(Account account) {
    feeFineRepository.getById(account.getFeeFineId())
      .compose(this::fetchActionTemplateId)
      .map(templateId -> createNotice(account.getUserId(), templateId))
      .compose(patronNoticeClient::postPatronNotice)
      .setHandler(send -> {
        if (send.failed()) {
          logger.error("Failed to send patron notice", send.cause());
        } else {
          logger.info("Patron notice has been successfully sent");
        }
      });
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
