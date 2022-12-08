package org.folio.rest.client;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.User;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class OkapiClientTest extends BaseClientTest{
  public static final String USERS_URL = "/users";
  public static final String USER_ID = "72fc6429-69b1-47ea-bf38-6a26c87594b6";
  public static final User USER = new User().withId(USER_ID);
  private final OkapiClient okapiClient;
  {
    okapiClient = new OkapiClient(vertx, okapiHeaders);
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
    createStub(USERS_URL, USER_ID, HttpStatus.SC_NOT_FOUND, "User not found");

    okapiClient.getById(USERS_URL, USER_ID, User.class)
      .onSuccess(r -> context.fail("should have failed"))
      .onFailure(failure -> {
        String expectedErrorMessage = "User " + USER_ID + " was not found";
        context.assertEquals(expectedErrorMessage, failure.getMessage());
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

}
