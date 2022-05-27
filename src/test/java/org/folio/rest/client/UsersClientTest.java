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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.folio.test.support.ApiTests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
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
  public void shouldSucceedWhenUserIsFetched(VertxTestContext context) {
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
      .onFailure(context::failNow)
      .onSuccess(result -> {
        assertThat(userJson, is(JsonObject.mapFrom(result)));
        context.completeNow();
      });
  }

  @Test
  public void shouldFailWhenUserIsNotFound(VertxTestContext context) {
    String responseBody = "User not found";
    mockUsersResponse(HttpStatus.SC_NOT_FOUND, responseBody);

    String expectedFailureMessage = "Failed to fetch User " + USER_ID + ": [404] User not found";

    usersClient.fetchUserById(USER_ID)
      .onSuccess(user -> context.failNow("Should have failed"))
      .onFailure(throwable -> {
        assertThat(expectedFailureMessage, is(throwable.getMessage()));
        context.completeNow();
      });
  }

  @Test
  public void shouldFailWhenReceivedInvalidResponse(VertxTestContext context) {
    String responseBody = "not a json";
    mockUsersResponse(HttpStatus.SC_OK, responseBody);

    String expectedErrorMessage = String.format(
      "Failed to parse response from %s. Response body: %s", "/users/" + USER_ID, responseBody);


    usersClient.fetchUserById(USER_ID)
      .onSuccess(user -> context.failNow("Should have failed"))
      .onFailure(throwable -> {
        assertThat(expectedErrorMessage, is(throwable.getMessage()));
        context.completeNow();
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
