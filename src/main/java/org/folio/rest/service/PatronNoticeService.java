package org.folio.rest.service;

import java.util.Map;

import org.folio.rest.client.PatronNoticeClient;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.PatronNotice;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.repository.FeeFineRepository;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class PatronNoticeService {

  private static final Logger logger = LoggerFactory.getLogger(PatronNoticeService.class);

  private FeeFineRepository feeFineRepository;
  private PatronNoticeClient patronNoticeClient;

  public PatronNoticeService(Vertx vertx, Map<String, String> okapiHeaders) {
    feeFineRepository = new FeeFineRepository(PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders));
    patronNoticeClient = new PatronNoticeClient(WebClient.create(vertx), okapiHeaders);
  }

  public void sendManualChargeNotice(Account account) {
    feeFineRepository.getById(account.getFeeFineId())
      .map(feefine -> createManualChargeNotice(account.getUserId(), feefine))
      .compose(patronNoticeClient::postPatronNotice)
      .setHandler(send -> {
        if (send.failed()) {
          logger.error("Failed to send patron notice", send.cause());
        } else {
          logger.info("Patron notice has been successfully sent");
        }
      });
  }

  private PatronNotice createManualChargeNotice(String userId, Feefine feefine) {
    return createNotice(userId, feefine.getChargeNoticeId());
  }

  private PatronNotice createNotice(String userId, String templateId) {
    return new PatronNotice()
      .withDeliveryChannel("email")
      .withOutputFormat("text/html")
      .withRecipientId(userId)
      .withTemplateId(templateId);
  }
}
