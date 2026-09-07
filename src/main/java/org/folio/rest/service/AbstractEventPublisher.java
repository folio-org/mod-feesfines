package org.folio.rest.service;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.EventType;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AbstractEventPublisher {

  protected static final Logger log = LogManager.getLogger(AbstractEventPublisher.class);

  private final KafkaEventProducer kafkaEventProducer;
  private final Map<String, String> headers;

  public AbstractEventPublisher(Context context, Map<String, String> headers) {
    this(context.owner(), headers);
  }

  public AbstractEventPublisher(Vertx vertx, Map<String, String> headers) {
    this.kafkaEventProducer = new KafkaEventProducer(vertx);
    this.headers = headers;
  }

  protected Future<Void> publish(String key, EventType eventType, JsonObject payload) {
    return kafkaEventProducer.publish(key, eventType, payload.encode(), headers);
  }

}
