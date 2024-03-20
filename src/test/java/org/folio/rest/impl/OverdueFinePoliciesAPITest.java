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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OverdueFinePoliciesAPITest extends ApiTests {
  private static final String REST_PATH = "/overdue-fines-policies";
  private static final String NEGATIVE_QUANTITY_MESSAGE = "Quantity must not be negative.";

  @BeforeEach
  public void setUp() {
    removeAllFromTable(OverdueFinePoliciesAPI.TABLE_NAME);
  }

  @Test
  public void postOverdueFinesPoliciesSuccess() {
    post(createEntity())
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  @Test
  public void postOverdueFinesPoliciesDuplicate() {
    post(createEntity());

    JsonObject errorJson = new JsonObject()
      .put("message", "The Overdue fine policy name entered already exists. Please enter a different name.")
      .put("code", OverdueFinePoliciesAPI.DUPLICATE_ERROR_CODE)
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
  public void postOverdueFinesPoliciesMissingName() {
    JsonObject parameters = new JsonObject()
      .put("key", "name")
      .put("value", "null");

    JsonObject error = new JsonObject()
      .put("message", "must not be null")
      .put("type", "1")
      .put("code", "jakarta.validation.constraints.NotNull.message")
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
  public void postOverdueFinesPoliciesMalformedJson() {
    post(createEntity().substring(1))
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .contentType(ContentType.TEXT)
      .body(startsWith(
        "Cannot construct instance of `org.folio.rest.jaxrs.model.OverdueFinePolicy`"));
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
      .put("code", "jakarta.validation.constraints.Pattern.message")
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
  public void postOverdueFinesPoliciesServerError() {
    client.getRequestSpecification()
      .header(OKAPI_HEADER_TENANT, "test_breaker")
      .body(createEntity())
      .post(REST_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
      .body(
        containsString("password authentication failed for user"));
  }

  @Test
  public void postOverdueFinePolicyWithZeroFineAmount() {
    JsonObject entityJson = createEntityJson();
    entityJson.getJsonObject("overdueFine").put("quantity", 0);
    entityJson.getJsonObject("overdueRecallFine").put("quantity", 0);

    post(entityJson.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  @Test
  public void postOverdueFinePolicyWithNegativeOverdueFineQuantity() {
    JsonObject entityJson = createEntityJson();
    entityJson.getJsonObject("overdueFine").put("quantity", "-1");

    post(entityJson.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(allOf(List.of(
        hasJsonPath("errors[0].message", is(NEGATIVE_QUANTITY_MESSAGE)),
        hasJsonPath("errors[0].parameters[0].key", is("overdueFine.quantity")),
        hasJsonPath("errors[0].parameters[0].value", is("-1.00"))
      )))
      .contentType(ContentType.JSON);
  }

  @Test
  public void postOverdueFinePolicyWithNegativeOverdueRecallFineQuantity() {
    JsonObject entityJson = createEntityJson();
    entityJson.getJsonObject("overdueRecallFine").put("quantity", "-2");

    post(entityJson.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(allOf(List.of(
        hasJsonPath("errors[0].message", is(NEGATIVE_QUANTITY_MESSAGE)),
        hasJsonPath("errors[0].parameters[0].key", is("overdueRecallFine.quantity")),
        hasJsonPath("errors[0].parameters[0].value", is("-2.00"))
      )))
      .contentType(ContentType.JSON);
  }

  @Test
  public void putOverdueFinePolicyWithNegativeoOverdueFineQuantity() {
    JsonObject entity = createEntityJson();
    post(entity.encodePrettily());

    entity.getJsonObject("overdueFine").put("quantity", "-1");

    put(entity.getString("id"), entity.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(allOf(List.of(
        hasJsonPath("errors[0].message", is(NEGATIVE_QUANTITY_MESSAGE)),
        hasJsonPath("errors[0].parameters[0].key", is("overdueFine.quantity")),
        hasJsonPath("errors[0].parameters[0].value", is("-1.00"))
      )))
      .contentType(ContentType.JSON);
  }

  @Test
  public void putOverdueFinePolicyWithNegativeOverdueRecallFineQuantity() {
    JsonObject entity = createEntityJson();
    post(entity.encodePrettily());

    entity.getJsonObject("overdueRecallFine").put("quantity", "-1");

    put(entity.getString("id"), entity.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(allOf(List.of(
        hasJsonPath("errors[0].message", is(NEGATIVE_QUANTITY_MESSAGE)),
        hasJsonPath("errors[0].parameters[0].key", is("overdueRecallFine.quantity")),
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

  private String createEntity() {
    return createEntityJson().encodePrettily();
  }

  private JsonObject createEntityJson() {
    return new JsonObject()
      .put("name", "Faculty standard")
      .put("description", "This is description for Faculty standard")
      .put("overdueFine", new JsonObject().put("quantity", 5.0).put("intervalId", "day"))
      .put("countClosed", true)
      .put("maxOverdueFine", 50.00)
      .put("forgiveOverdueFine", true)
      .put("overdueRecallFine", new JsonObject().put("quantity", 1.0).put("intervalId", "hour"))
      .put("gracePeriodRecall", false)
      .put("maxOverdueRecallFine", 50.00)
      .put("id", randomId());
  }
}
