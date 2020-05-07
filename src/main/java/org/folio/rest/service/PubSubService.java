package org.folio.rest.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.domain.EventType.FF_BALANCE_CHANGE;
import static org.folio.rest.utils.JsonHelper.write;

import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.EventType;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;

import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class PubSubService {
  private final Logger logger = LoggerFactory.getLogger(PubSubService.class);
  private final OkapiConnectionParams connectionParams;

  public PubSubService(Map<String, String> okapiHeaders, Context context) {
    this(okapiHeaders, context.owner());
  }

  public PubSubService(Map<String, String> okapiHeaders, Vertx vertx) {
    this.connectionParams = new OkapiConnectionParams(okapiHeaders, vertx);
  }

  public void registerModuleInPubsub(Promise<Object> promise) {
    PubSubClientUtils.registerModule(connectionParams)
      .whenComplete((result, throwable) -> {
        if (throwable == null) {
          logger.info("Module was successfully registered as publisher/subscriber in mod-pubsub");
          promise.complete();
        } else {
          logger.error("Error during module registration in mod-pubsub", throwable);
          promise.fail(throwable);
        }
      });
  }

  public void publishAccountBalanceChangeEvent(Account account) {
    String payload = createPayload(account);
    Event event = createEvent(FF_BALANCE_CHANGE, payload);
    publishEvent(event);
  }

  public void publishDeletedAccountBalanceChangeEvent(String accountId) {
    Account account = new Account()
      .withId(accountId)
      .withRemaining(0.0);

    publishAccountBalanceChangeEvent(account);
  }

  private void publishEvent(final Event event) {
    PubSubClientUtils.sendEventMessage(event, connectionParams)
      .whenComplete((result, throwable) -> {
        String eventType = event.getEventType();
        String eventId = event.getId();
        if (throwable == null && isTrue(result)) {
          logger.info("Event {} published successfully: {}", eventType, eventId);
        } else {
          logger.error("Failed to publish event {}: {}", throwable, eventType, eventId);
        }
      });
  }

  private String createPayload(Account account) {
    JsonObject payload = new JsonObject();
    write(payload, "accountId", account.getId());
    write(payload, "userId", account.getUserId());
    write(payload, "feeFineId", account.getFeeFineId());
    write(payload, "balance", account.getRemaining());

    return payload.encodePrettily();
  }

  private Event createEvent(EventType eventType, String payload) {
    return new Event()
      .withId(UUID.randomUUID().toString())
      .withEventType(eventType.name())
      .withEventPayload(payload)
      .withEventMetadata(new EventMetadata()
        .withPublishedBy(PubSubClientUtils.constructModuleName())
        .withTenantId(connectionParams.getTenantId())
        .withEventTTL(1));
  }

}
