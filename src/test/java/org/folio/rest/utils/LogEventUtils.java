package org.folio.rest.utils;

import static org.folio.test.support.ApiTests.TENANT_NAME;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.domain.FeeFineKafkaTopic;
import org.folio.rest.service.LogEventPublisher.LogEventPayloadType;
import org.folio.test.support.KafkaTestHelper;

import io.vertx.core.json.JsonObject;

public class LogEventUtils {
  private LogEventUtils() {
  }

  public static List<JsonObject> fetchPublishedLogRecords(long fromTimestampMs) {
    String topic = FeeFineKafkaTopic.LOG_RECORD_TOPIC.fullTopicName(TENANT_NAME);
    return KafkaTestHelper.getInstance().pollMessages(topic, fromTimestampMs)
      .stream()
      .map(JsonObject::new)
      .toList();
  }

  public static List<JsonObject> fetchPublishedLogRecords(long fromTimestampMs,
    LogEventPayloadType logEventPayloadType) {

    return fetchPublishedLogRecords(fromTimestampMs)
      .stream()
      .filter(json -> isLogEventOfType(json, logEventPayloadType))
      .toList();
  }

  private static boolean isLogEventOfType(JsonObject event, LogEventPayloadType logEventType) {
    return Optional.of(event)
      .filter(json -> StringUtils.equals(json.getString("logEventType"), logEventType.value()))
      .isPresent();
  }

  public static List<String> fetchLogEventPayloads(long fromTimestampMs) {
    return fetchPublishedLogRecords(fromTimestampMs).stream()
      .map(json -> json.getJsonObject("payload").encodePrettily())
      .toList();
  }

  public static String fetchFirstLogRecordEventPayload(long fromTimestampMs,
    LogEventPayloadType logEventPayloadType) {

    return fetchPublishedLogRecords(fromTimestampMs, logEventPayloadType)
      .stream()
      .map(json -> json.getJsonObject("payload").encodePrettily())
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No log records found"));
  }
}
