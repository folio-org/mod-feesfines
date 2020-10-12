package org.folio.rest.service;

import static org.folio.rest.domain.EventType.LOG_RECORD;
import static org.folio.rest.utils.JsonHelper.write;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public class LogEventPublisher {
  public static final String LOG_EVENT_TYPE = "logEventType";
  public static final String PAYLOAD = "payload";
  private final EventPublisher eventPublisher;

  public LogEventPublisher(Context context, Map<String, String> headers) {
    this(context.owner(), headers);
  }

  public LogEventPublisher(Vertx vertx, Map<String, String> headers) {
    eventPublisher = new EventPublisher(vertx, headers);
  }

  public void publishLogEvent(JsonObject jsonObject, LogEventPayloadType logEventPayloadType) {
    final JsonObject payload = createLogRecordPayload(jsonObject, logEventPayloadType);
    eventPublisher.publishEventAsynchronously(LOG_RECORD, payload.encode());
  }

  private JsonObject createLogRecordPayload(JsonObject jsonObject, LogEventPayloadType logEventPayloadType) {
    JsonObject logEventPayload = new JsonObject();
    write(logEventPayload, LOG_EVENT_TYPE, logEventPayloadType.value());
    write(logEventPayload, PAYLOAD, jsonObject.encode());
    return logEventPayload;
  }

  public enum LogEventPayloadType {

    FEE_FINE("FEE_FINE"),
    NOTICE("NOTICE");

    private final String value;

    LogEventPayloadType(String value) {
      this.value = value;
    }

    public String value() {
      return this.value;
    }
  }
}
