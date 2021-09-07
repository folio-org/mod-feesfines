package org.folio.rest.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.service.LogEventPublisher.LogEventPayloadType;
import org.folio.test.support.OkapiDeployment;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.vertx.core.json.JsonObject;

public class LogEventUtils {
  private LogEventUtils() {
  }

  public static List<JsonObject> fetchPublishedLogRecords(OkapiDeployment okapiDeployment) {
    return okapiDeployment
      .findRequestsMatching(postRequestedFor(urlPathMatching("/pubsub/publish")).build())
      .getRequests().stream()
      .map(LoggedRequest::getBody)
      .map(String::new)
      .map(JsonObject::new)
      .filter(json -> "LOG_RECORD".equals(json.getString("eventType")))
      .collect(toList());
  }

  public static List<JsonObject> fetchPublishedLogRecords(OkapiDeployment okapiDeployment,
    LogEventPayloadType logEventPayloadType) {

    return fetchPublishedLogRecords(okapiDeployment)
      .stream()
      .filter(json -> isLogEventOfType(json, logEventPayloadType))
      .collect(toList());
  }

  private static boolean isLogEventOfType(JsonObject event, LogEventPayloadType logEventType) {
    return Optional.of(event)
      .map(json -> json.getString("eventPayload"))
      .map(JsonObject::new)
      .filter(json -> StringUtils.equals(json.getString("logEventType"), logEventType.value()))
      .isPresent();
  }

  public static List<String> fetchLogEventPayloads(OkapiDeployment okapiDeployment) {
    return fetchPublishedLogRecords(okapiDeployment).stream()
      .map(json -> json.getString("eventPayload"))
      .map(JsonObject::new)
      .map(json -> json.getJsonObject("payload").encodePrettily())
      .collect(toList());
  }

  public static String fetchFirstLogRecordEventPayload(OkapiDeployment okapiDeployment,
    LogEventPayloadType logEventPayloadType) {

    return fetchPublishedLogRecords(okapiDeployment, logEventPayloadType)
      .stream()
      .map(json -> json.getString("eventPayload"))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No log records found"));
  }

}
