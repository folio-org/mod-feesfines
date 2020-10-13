package org.folio.rest.service;

import static org.folio.rest.domain.EventType.LOG_RECORD;
import static org.folio.rest.utils.JsonHelper.write;

import java.util.Map;

import org.folio.rest.jaxrs.model.Manualblock;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

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

  public void publishLogEvent(Manualblock manualBlock, LogEventPayloadType logEventPayloadType) {
    final JsonObject payload = createLogRecordPayload(manualBlock, logEventPayloadType);
    eventPublisher.publishEventAsynchronously(LOG_RECORD, payload.encode());
  }

  private JsonObject createLogRecordPayload(Manualblock manualBlock, LogEventPayloadType logEventPayloadType) {
    JsonObject logEventPayload = new JsonObject();
    write(logEventPayload, LOG_EVENT_TYPE, logEventPayloadType.value());
    write(logEventPayload, PAYLOAD, JsonObject.mapFrom(manualBlock));
    return logEventPayload;
  }

  public enum LogEventPayloadType {
    FEE_FINE("FEE_FINE"),
    NOTICE("NOTICE"),
    MANUAL_BLOCK_CREATED("MANUAL_BLOCK_CREATED_EVENT"),
    MANUAL_BLOCK_MODIFIED("MANUAL_BLOCK_MODIFIED_EVENT"),
    MANUAL_BLOCK_DELETED("MANUAL_BLOCK_DELETED_EVENT");

    private final String value;

    LogEventPayloadType(String value) {
      this.value = value;
    }

    public String value() {
      return this.value;
    }
  }
}
