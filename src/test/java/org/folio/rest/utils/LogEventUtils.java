package org.folio.rest.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.vertx.core.json.JsonObject;
import org.folio.test.support.OkapiDeployment;

import java.util.List;
import java.util.stream.Collectors;

public class LogEventUtils {
  private LogEventUtils(){}

  public static List<JsonObject> fetchPublishedLogRecords(OkapiDeployment okapiDeployment) {
    return okapiDeployment
      .findRequestsMatching(postRequestedFor(urlPathMatching("/pubsub/publish")).build())
      .getRequests().stream()
      .map(LoggedRequest::getBody)
      .map(String::new)
      .filter(s -> s.contains("LOG_RECORD"))
      .map(JsonObject::new)
      .collect(Collectors.toList());
  }

  public static List<String> fetchLogEventPayloads(OkapiDeployment okapiDeployment) {
    return fetchPublishedLogRecords(okapiDeployment).stream()
      .map(json -> json.getString("eventPayload"))
      .map(JsonObject::new)
      .map(json -> json.getJsonObject("payload").encode())
      .collect(Collectors.toList());
  }
}
