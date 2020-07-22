package org.folio.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.vertx.core.json.JsonObject.mapFrom;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.test.support.ApiTests.OKAPI_TOKEN;
import static org.folio.test.support.ApiTests.TENANT_NAME;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;

@RunWith(VertxUnitRunner.class)
public class OkapiClientTest {
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final int OKAPI_PORT = NetworkUtils.nextFreePort();
  public static final String OKAPI_URL = "http://localhost:" + OKAPI_PORT;
  public static final String USERS_URL = "/users";
  public static final String USER_ID = "72fc6429-69b1-47ea-bf38-6a26c87594b6";
  public static final User USER = new User().withId(USER_ID);

  @Rule
  public WireMockRule mock = new WireMockRule(OKAPI_PORT);

  private final OkapiClient okapiClient;
  {
    CaseInsensitiveMap<String, String> okapiHeaders = new CaseInsensitiveMap<>();
    okapiHeaders.put(URL, OKAPI_URL);
    okapiHeaders.put(TENANT, TENANT_NAME);
    okapiHeaders.put(TOKEN, OKAPI_TOKEN);
    okapiClient = new OkapiClient(WebClient.create(Vertx.vertx()), okapiHeaders);
  }

  @Before
  public void beforeEach() {
    mock.resetAll();
  }

  @Test
  public void getByIdShouldSucceed(TestContext context) {
    Async async = context.async();
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    okapiClient.getById(USERS_URL, USER_ID, User.class)
      .onFailure(context::fail)
      .onSuccess(userResponse -> {
        context.assertEquals(USER_ID, userResponse.getId());
        async.complete();
      });
  }

  @Test
  public void getByIdShouldFailWhenReturnObjectTypeIsNull(TestContext context) {
    Async async = context.async();
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    okapiClient.getById(USERS_URL, USER_ID, null)
      .onSuccess(r -> context.fail("should have failed"))
      .onFailure(failure -> {
        context.assertEquals("Requested object type is null", failure.getMessage());
        async.complete();
      });
  }

  @Test
  public void getByIdShouldFailWhenResourcePathIsNull(TestContext context) {
    Async async = context.async();
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    okapiClient.getById(null, USER_ID, User.class)
      .onSuccess(r -> context.fail("should have failed"))
      .onFailure(failure -> {
        context.assertEquals("Invalid resource path for User", failure.getMessage());
        async.complete();
      });
  }

  @Test
  public void getByIdShouldFailWhenResourcePathIsBlank(TestContext context) {
    Async async = context.async();
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    okapiClient.getById(" ", USER_ID, User.class)
      .onSuccess(r -> context.fail("should have failed"))
      .onFailure(failure -> {
        context.assertEquals("Invalid resource path for User", failure.getMessage());
        async.complete();
      });
  }

  @Test
  public void getByIdShouldFailWhenIdIsInvalid(TestContext context) {
    Async async = context.async();
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    String invalidId = USER_ID + "z";

    okapiClient.getById(USERS_URL, invalidId, User.class)
      .onSuccess(r -> context.fail("should have failed"))
      .onFailure(failure -> {
        context.assertEquals("Invalid UUID: " + invalidId, failure.getMessage());
        async.complete();
      });
  }

  @Test
  public void getByIdShouldFailWhenRemoteCallFailed(TestContext context) {
    Async async = context.async();
    createStub(USERS_URL, USER_ID, HttpStatus.SC_NOT_FOUND, USER);

    okapiClient.getById(USERS_URL, USER_ID, User.class)
      .onSuccess(r -> context.fail("should have failed"))
      .onFailure(failure -> {
        context.assertEquals("Failed to get User by ID. Response status code: 404", failure.getMessage());
        async.complete();
      });
  }

  @Test
  public void getByIdShouldFailWhenFailedToParseResponse(TestContext context) {
    Async async = context.async();
    String responseBody = "not_a_json";
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, responseBody);

    String expectedErrorMessage = String.format("Failed to parse response from %s. Response body: %s",
    USERS_URL + "/" + USER_ID, responseBody);

    okapiClient.getById(USERS_URL, USER_ID, User.class)
      .onSuccess(r -> context.fail("should have failed"))
      .onFailure(failure -> {
        context.assertEquals(expectedErrorMessage, failure.getMessage());
        async.complete();
      });
  }

  private <T> void createStub(String url, String id, int status, T stubObject) {
    createStub(url, id, aResponse()
        .withStatus(status)
        .withBody(mapFrom(stubObject).encodePrettily()));
  }

  private <T> void createStub(String url, String id, int status, String responseBody) {
    createStub(url, id, aResponse()
      .withStatus(status)
      .withBody(responseBody));
  }

  private void createStub(String url, String id, ResponseDefinitionBuilder responseBuilder) {
    mock.stubFor(WireMock.get(urlPathEqualTo(url + "/" + id))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(OKAPI_URL))
      .willReturn(responseBuilder));
  }


}