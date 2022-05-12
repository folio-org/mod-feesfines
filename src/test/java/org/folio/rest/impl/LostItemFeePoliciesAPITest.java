package org.folio.rest.impl;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;

import java.util.Collections;
import java.util.List;

import org.apache.http.HttpStatus;
import org.folio.test.support.ApiTests;
import org.junit.Before;
import org.junit.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class LostItemFeePoliciesAPITest extends ApiTests {
  private static final String REST_PATH = "/lost-item-fees-policies";
  private static final String NEGATIVE_VALUE_MESSAGE = "Value must not be negative.";

  @Before
  public void setUp() {
    removeAllFromTable(LostItemFeePoliciesAPI.TABLE_NAME);
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
      .put("message", "must not be null")
      .put("type", "1")
      .put("code", "javax.validation.constraints.NotNull.message")
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
        "Cannot construct instance of `org.folio.rest.jaxrs.model.LostItemFeePolicy`"));
  }

  @Test
  public void postOverdueFinesPoliciesInvalidUuid() {
    String invalidUuid = randomId() + "a";

    String payloadWithInvalidUuid = createEntityJson()
      .put("id", invalidUuid)
      .encode();

    JsonObject parameters = new JsonObject()
      .put("key", "id")
      .put("value", invalidUuid);

    JsonObject error = new JsonObject()
      .put("message", "must match \"^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}$\"")
      .put("type", "1")
      .put("code", "javax.validation.constraints.Pattern.message")
      .put("parameters", new JsonArray(Collections.singletonList(parameters)));

    String errors = new JsonObject()
      .put("errors", new JsonArray(Collections.singletonList(error)))
      .encode();

    post(payloadWithInvalidUuid)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(ContentType.JSON)
      .body(equalTo(errors));
  }

  @Test
  public void postLostItemFeesPoliciesServerError() {
    client.getRequestSpecification()
      .header(OKAPI_HEADER_TENANT, "test_breaker")
      .body(createEntity())
      .post(REST_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
      .body(
        containsString("password authentication failed for user \\\"test_breaker_mod_feesfines\\\""));
  }

  @Test
  public void postLostItemFeesPolicyWithNegativeLostItemProcessingFeeValue() {
    JsonObject entityJson = createEntityJson();
    entityJson.put("lostItemProcessingFee", "-1");

    post(entityJson.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(allOf(List.of(
        hasJsonPath("errors[0].message", is(NEGATIVE_VALUE_MESSAGE)),
        hasJsonPath("errors[0].parameters[0].key", is("lostItemProcessingFee")),
        hasJsonPath("errors[0].parameters[0].value", is("-1.00"))
      )))
      .contentType(ContentType.JSON);
  }

  @Test
  public void putLostItemFeesPolicyWithNegativeLostItemProcessingFeeValue() {
    JsonObject entity = createEntityJson();
    post(entity.encodePrettily());

    entity.put("lostItemProcessingFee", "-1");

    put(entity.getString("id"), entity.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(allOf(List.of(
        hasJsonPath("errors[0].message", is(NEGATIVE_VALUE_MESSAGE)),
        hasJsonPath("errors[0].parameters[0].key", is("lostItemProcessingFee")),
        hasJsonPath("errors[0].parameters[0].value", is("-1.00"))
      )))
      .contentType(ContentType.JSON);
  }

  private Response post(String body) {
    return client.post(REST_PATH, body);
  }

  private Response put(String id, String body) {
    return client.put(REST_PATH + "/" + id, body);
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
        new JsonObject().put("duration", 5).put("intervalId", "Months"))
      .put("recalledItemAgedLostOverdue", new JsonObject().put("duration", 12).put("intervalId", "Months"))
      .put("patronBilledAfterRecalledItemAgedLost",
        new JsonObject().put("duration", 6).put("intervalId", "Months"))
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
