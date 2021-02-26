package org.folio.rest.service;

import static org.folio.rest.tools.utils.TenantTool.tenantId;

import io.vertx.core.Vertx;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.FeeFinePubSubClient;
import org.folio.rest.domain.EventType;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.util.pubsub.PubSubClientUtils;

public class EventPublisher {
  private final Logger logger = LogManager.getLogger(EventPublisher.class);
  private final FeeFinePubSubClient pubSubClient;
  private final String tenantId;

  public EventPublisher(Vertx vertx, Map<String, String> okapiHeaders) {
    pubSubClient = new FeeFinePubSubClient(vertx, okapiHeaders);
    tenantId = tenantId(okapiHeaders);
  }

  public CompletableFuture<Void> publishEvent(EventType type, String payload) {
    return publishEvent(createEvent(type, payload));
  }

  private CompletableFuture<Void> publishEvent(Event event) {
    return pubSubClient.publishEvent(event);
  }

  public void publishEventAsynchronously(EventType type, String payload) {
    publishEventAsynchronously(createEvent(type, payload));
  }

  private void publishEventAsynchronously(final Event event) {
    publishEvent(event).whenComplete((noResult, error) -> {
      final String id = event.getId();
      final String type = event.getEventType();
      final String payload = event.getEventPayload();

      if (error == null) {
        logger.info("Event {} published successfully: {}", type, id);
      } else {
        logger.error("Failed to publish event [id={}, type={}, payload={}]: cause {}",
          id, type, payload, error);
      }
    });
  }

  private Event createEvent(EventType eventType, String payload) {
    return new Event()
      .withId(UUID.randomUUID().toString())
      .withEventType(eventType.name())
      .withEventPayload(payload)
      .withEventMetadata(new EventMetadata()
        .withPublishedBy(PubSubClientUtils.constructModuleName())
        .withTenantId(tenantId)
        .withEventTTL(1));
  }
}
