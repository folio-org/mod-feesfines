package org.folio.rest.service;

import static org.folio.rest.domain.EventType.LOG_RECORD;
import static org.folio.rest.utils.JsonHelper.write;

import java.util.Map;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class LogEventPublisher extends AbstractEventPublisher {

  public static final String LOG_EVENT_TYPE = "logEventType";
  public static final String PAYLOAD = "payload";

  public LogEventPublisher(Context context, Map<String, String> headers) {
    super(context, headers);
  }

  public LogEventPublisher(Vertx vertx, Map<String, String> headers) {
    super(vertx, headers);
  }

  public void publishFeeFineActionLogEvent(String accountId, JsonObject json) {
    publishLogEvent(accountId, json, LogEventPayloadType.FEE_FINE);
  }

  public void publishManualBlockLogEvent(String userId, JsonObject json, LogEventPayloadType logEventPayloadType) {
    publishLogEvent(userId, json, logEventPayloadType);
  }

  public void publishPatronNoticeLogEvent(String userId, JsonObject json, LogEventPayloadType logEventPayloadType) {
    publishLogEvent(userId, json, logEventPayloadType);
  }

  private void publishLogEvent(String key, JsonObject json, LogEventPayloadType logEventPayloadType) {
    final JsonObject payload = createLogRecordPayload(json, logEventPayloadType);

    publish(key, LOG_RECORD, payload)
      .onFailure(e -> log.error("Failed to publish {} event [type={}]", LOG_RECORD, logEventPayloadType, e));
  }

  private JsonObject createLogRecordPayload(JsonObject payload, LogEventPayloadType logEventPayloadType) {
    JsonObject logEventPayload = new JsonObject();
    write(logEventPayload, LOG_EVENT_TYPE, logEventPayloadType.value());
    write(logEventPayload, PAYLOAD, payload);
    return logEventPayload;
  }

  public enum LogEventPayloadType {
    FEE_FINE("FEE_FINE"),
    NOTICE("NOTICE"),
    NOTICE_ERROR("NOTICE_ERROR"),
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
