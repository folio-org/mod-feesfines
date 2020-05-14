package org.folio.rest.impl;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;


import io.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;

import org.apache.http.HttpStatus;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.OkapiClient;
import org.junit.*;
import org.junit.runner.RunWith;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.util.Collections;

@RunWith(VertxUnitRunner.class)
public class LostItemFeePoliciesAPITest extends APITests {
  private static final String REST_PATH = "/lost-item-fees-policies";

  @Before
  public void setUp(TestContext context) {
    Async async = context.async();
    PostgresClient.getInstance(vertx, OKAPI_TENANT)
      .delete(LostItemFeePoliciesAPI.TABLE_NAME, new Criterion(), event -> {
        if (event.failed()) {
          log.error(event.cause());
          context.fail(event.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void postLostItemFeesPoliciesSuccess() {
    post(createEntity())
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  @Test
  public void postLostItemFeesPoliciesDuplicate() {
    post(createEntity());

    JsonObject errorJson = new JsonObject()
      .put("message",
        "The Lost item fee policy name entered already exists. Please enter a different name.")
      .put("code", LostItemFeePoliciesAPI.DUPLICATE_ERROR_CODE)
      .put("parameters", new JsonArray());

    String errors = new JsonObject()
      .put("errors", new JsonArray(Collections.singletonList(errorJson)))
      .encodePrettily();

    post(createEntity())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(ContentType.JSON)
      .body(equalTo(errors));
  }

  @Test
  public void postLostItemFeesPoliciesMissingName() {
    JsonObject parameters = new JsonObject()
      .put("key", "name")
      .put("value", "null");

    JsonObject error = new JsonObject()
      .put("message", "may not be null")
      .put("type", "1")
      .put("code", "-1")
      .put("parameters", new JsonArray(Collections.singletonList(parameters)));

    String errors = new JsonObject()
      .put("errors", new JsonArray(Collections.singletonList(error)))
      .encode();

    JsonObject payload = createEntityJson();
    payload.remove("name");

    post(payload.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(ContentType.JSON)
      .body(equalTo(errors));
  }

  @Test
  public void postLostItemFeesPoliciesMalformedJson() {
    post(createEntity().substring(1))
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .contentType(ContentType.TEXT)
      .body(startsWith(
        "Json content error Cannot construct instance of `org.folio.rest.jaxrs.model.LostItemFeePolicy`"));
  }

  @Test
  public void postOverdueFinesPoliciesInvalidUuid() {
    String invalidUuid = randomId() + "a";

    String payloadWithInvalidUuid = createEntityJson()
      .put("id", invalidUuid)
      .encodePrettily();

    JsonObject parameters = new JsonObject()
      .put("key", "lost_item_fee_policy.id")
      .put("value", invalidUuid);

    JsonObject error = new JsonObject()
      .put("message", "Invalid UUID format of id, should be xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx" +
        " where M is 1-5 and N is 8, 9, a, b, A or B and x is 0-9, a-f or A-F.")
      .put("type", "1")
      .put("code", "-1")
      .put("parameters", new JsonArray(Collections.singletonList(parameters)));

    String errors = new JsonObject()
      .put("errors", new JsonArray(Collections.singletonList(error)))
      .encodePrettily();

    post(payloadWithInvalidUuid)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(ContentType.JSON)
      .body(equalTo(errors));
  }

  @Test
  public void postLostItemFeesPoliciesServerError() {
    new OkapiClient(OKAPI_URL, "test_breaker", OKAPI_TOKEN)
      .post(REST_PATH, createEntity())
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
      .body(
        containsString("password authentication failed for user \"test_breaker_mod_feesfines\""));
  }

  private Response post(String body) {
    return okapiClient.post(REST_PATH, body);
  }

  private static String createEntity() {
    return createEntityJson().encodePrettily();
  }

  private static JsonObject createEntityJson() {
    return new JsonObject()
      .put("name", "Undergrad standard")
      .put("description", "This is description for undergrad standard")
      .put("itemAgedLostOverdue", new JsonObject().put("duration", 12).put("intervalId", "Months"))
      .put("patronBilledAfterAgedLost",
        new JsonObject().put("duration", 12).put("intervalId", "Months"))
      .put("chargeAmountItem",
        new JsonObject().put("chargeType", "Actual cost").put("amount", 5.00))
      .put("lostItemProcessingFee", 5.00)
      .put("chargeAmountItemPatron", true)
      .put("chargeAmountItemSystem", true)
      .put("lostItemChargeFeeFine", new JsonObject().put("duration", 6).put("intervalId", "Months"))
      .put("returnedLostItemProcessingFee", true)
      .put("replacedLostItemProcessingFee", true)
      .put("replacementProcessingFee", 0.00)
      .put("replacementAllowed", true)
      .put("lostItemReturned", "Charge")
      .put("id", randomId());
  }

}
