package org.folio.rest.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.vertx.core.json.JsonObject.mapFrom;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.test.support.ApiTests.OKAPI_TOKEN;
import static org.folio.test.support.ApiTests.OKAPI_URL_HEADER;
import static org.folio.test.support.ApiTests.TENANT_NAME;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.folio.test.support.OkapiDeployment;

import java.util.List;
import java.util.stream.Collectors;

public class LogEventUtils {
  private static final String USERS_PATH = "/users";

  private LogEventUtils(){}

  public static User createUser(String id) {
    return new User()
      .withId(id)
      .withBarcode("54321")
      .withPersonal(new Personal()
        .withFirstName("First")
        .withMiddleName("Middle")
        .withLastName("Last"));
  }

  public static void stubFor(User user, OkapiDeployment okapiDeployment) {
    okapiDeployment.stubFor(WireMock.get(urlPathEqualTo(USERS_PATH + "/" + user.getId()))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(okapiDeployment.getOkapiUrl()))
      .willReturn(aResponse().withBody(mapFrom(user).encodePrettily())));
  }

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
      .map(json -> json.getString("payload"))
      .collect(Collectors.toList());
  }
}
