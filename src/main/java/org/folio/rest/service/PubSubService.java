package org.folio.rest.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.domain.EventType.FF_BALANCE_CHANGED;
import static org.folio.rest.utils.JsonHelper.write;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

  public CompletableFuture<Boolean> registerModuleInPubsub(Promise<Object> promise) {
    return PubSubClientUtils.registerModule(connectionParams)
      .whenComplete((result, throwable) -> {
        if (isTrue(result) && throwable == null) {
          logger.info("Module was successfully registered as publisher/subscriber in mod-pubsub");
          promise.complete(result);
        } else {
          logger.error("Error during module registration in mod-pubsub", throwable);
          promise.fail(throwable);
        }
      });
  }

  public CompletableFuture<Boolean> publishAccountBalanceChangeEvent(Account account) {
    String payload = createPayload(account);
    Event event = createEvent(FF_BALANCE_CHANGED, payload);

    return publishEvent(event);
  }

  public CompletableFuture<Boolean> publishDeletedAccountBalanceChangeEvent(String accountId) {
    Account account = new Account()
      .withId(accountId)
      .withRemaining(0.0);

    return publishAccountBalanceChangeEvent(account);
  }

  private CompletableFuture<Boolean> publishEvent(final Event event) {
    return PubSubClientUtils.sendEventMessage(event, connectionParams)
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
    write(payload, "userId", account.getUserId());
    write(payload, "feeFineId", account.getId());
    write(payload, "feeFineTypeId", account.getFeeFineId());
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
