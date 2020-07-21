package org.folio.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import java.util.Collections;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.folio.test.support.ApiTests;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class UsersClientTest extends ApiTests {
  private final UsersClient usersClient;
  {
    CaseInsensitiveMap<String, String> okapiHeaders = new CaseInsensitiveMap<>();
    okapiHeaders.put(URL, getOkapiUrl());
    okapiHeaders.put(TENANT, TENANT_NAME);
    okapiHeaders.put(TOKEN, OKAPI_TOKEN);
    usersClient = new UsersClient(vertx, okapiHeaders);
  }

  @Test
  public void shouldSucceedWhenUserIsFetched(TestContext context) {
    Async async = context.async();

    User user = new User()
      .withId(USER_ID)
      .withUsername("tester")
      .withActive(true)
      .withBarcode("123456")
      .withPatronGroup(randomId())
      .withType("patron")
      .withAdditionalProperty("additionalProperty", "value")
      .withPersonal(new Personal()
        .withFirstName("First")
        .withMiddleName("Middle")
        .withLastName("Last")
        .withEmail("test@test.com")
        .withAdditionalProperty("additionalProperty", "value")
        .withAddresses(Collections.singletonList(
          new Address()
            .withId(randomId())
            .withAdditionalProperty("additionalProperty", "value")))
      );

    JsonObject userJson = JsonObject.mapFrom(user);
    mockUsersResponse(HttpStatus.SC_OK, userJson.encodePrettily());

    usersClient.fetchUserById(USER_ID)
      .onFailure(context::fail)
      .onSuccess(result -> {
        context.assertEquals(userJson, JsonObject.mapFrom(result));
        async.complete();
      });
  }

  @Test
  public void shouldFailWhenUserIsNotFound(TestContext context) {
    Async async = context.async();

    String responseBody = "User not found";
    mockUsersResponse(HttpStatus.SC_NOT_FOUND, responseBody);

    String expectedFailureMessage = "Failed to get User by ID. Response status code: 404";

    usersClient.fetchUserById(USER_ID)
      .onSuccess(user -> context.fail("Should have failed"))
      .onFailure(throwable -> {
        context.assertEquals(expectedFailureMessage, throwable.getMessage());
        async.complete();
      });
  }

  @Test
  public void shouldFailWhenReceivedInvalidResponse(TestContext context) {
    Async async = context.async();

    String responseBody = "not a json";
    mockUsersResponse(HttpStatus.SC_OK, responseBody);

    String expectedErrorMessage = String.format(
      "Failed to parse response from %s. Response body: %s", "/users/" + USER_ID, responseBody);


    usersClient.fetchUserById(USER_ID)
      .onSuccess(user -> context.fail("Should have failed"))
      .onFailure(throwable -> {
        context.assertEquals(expectedErrorMessage, throwable.getMessage());
        async.complete();
      });
  }

  private void mockUsersResponse(int status, String response) {
    getOkapi().stubFor(WireMock.get(urlPathEqualTo("/users/" + USER_ID))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(getOkapiUrl()))
      .willReturn(aResponse().withStatus(status).withBody(response)));
  }

}