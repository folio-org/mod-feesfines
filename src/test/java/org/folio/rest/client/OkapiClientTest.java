package org.folio.rest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class OkapiClientTest extends BaseClientTest{
  public static final String USERS_URL = "/users";
  public static final String USER_ID = "72fc6429-69b1-47ea-bf38-6a26c87594b6";
  public static final User USER = new User().withId(USER_ID);
  private final OkapiClient okapiClient;
  {
    okapiClient = new OkapiClient(vertx, okapiHeaders);
  }

  @Test
  void getByIdShouldSucceed(VertxTestContext context) {
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    okapiClient.getById(USERS_URL, USER_ID, User.class)
      .onComplete(context.succeeding(user -> {
        assertEquals(USER_ID, user.getId());
        context.completeNow();
      }));
  }

  @Test
  void getByIdShouldFailWhenReturnObjectTypeIsNull(VertxTestContext context) {
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    okapiClient.getById(USERS_URL, USER_ID, null)
      .onComplete(context.failing(failure -> {
        assertEquals("Requested object type is null", failure.getMessage());
        context.completeNow();
      }));
  }

  @Test
  void getByIdShouldFailWhenResourcePathIsNull(VertxTestContext context) {
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    okapiClient.getById(null, USER_ID, User.class)
      .onComplete(context.failing(failure -> {
        assertEquals("Invalid resource path for User", failure.getMessage());
        context.completeNow();
      }));
  }

  @Test
  void getByIdShouldFailWhenResourcePathIsBlank(VertxTestContext context) {
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    okapiClient.getById(" ", USER_ID, User.class)
      .onComplete(context.failing(failure -> {
        assertEquals("Invalid resource path for User", failure.getMessage());
        context.completeNow();
      }));
  }

  @Test
  void getByIdShouldFailWhenIdIsInvalid(VertxTestContext context) {
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, USER);

    String invalidId = USER_ID + "z";

    okapiClient.getById(USERS_URL, invalidId, User.class)
      .onComplete(context.failing(failure -> {
        assertEquals("Invalid UUID: " + invalidId, failure.getMessage());
        context.completeNow();
      }));
  }

  @Test
  void getByIdShouldFailWhenRemoteCallFailed(VertxTestContext context) {
    createStub(USERS_URL, USER_ID, HttpStatus.SC_NOT_FOUND, "User not found");

    okapiClient.getById(USERS_URL, USER_ID, User.class)
      .onComplete(context.failing(failure -> {
        String expectedErrorMessage = "User " + USER_ID + " was not found";
        assertEquals(expectedErrorMessage, failure.getMessage());
        context.completeNow();
      }));
  }

  @Test
  void getByIdShouldFailWhenFailedToParseResponse(VertxTestContext context) {
    String responseBody = "not_a_json";
    createStub(USERS_URL, USER_ID, HttpStatus.SC_OK, responseBody);

    String expectedErrorMessage = String.format("Failed to parse response from %s. Response body: %s",
    USERS_URL + "/" + USER_ID, responseBody);

    okapiClient.getById(USERS_URL, USER_ID, User.class)
      .onComplete(context.failing(failure -> {
        assertEquals(expectedErrorMessage, failure.getMessage());
        context.completeNow();
      }));
  }

}
