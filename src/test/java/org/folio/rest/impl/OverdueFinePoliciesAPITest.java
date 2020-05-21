package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.core.StringStartsWith.startsWith;

import java.util.Collections;

import org.apache.http.HttpStatus;
import org.folio.test.support.ApiTests;
import org.junit.Before;
import org.junit.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class OverdueFinePoliciesAPITest extends ApiTests {
  private static final String REST_PATH = "/overdue-fines-policies";

  @Before
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
  public void postOverdueFinesPoliciesMalformedJson() {
    post(createEntity().substring(1))
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .contentType(ContentType.TEXT)
      .body(startsWith(
        "Json content error Cannot construct instance of `org.folio.rest.jaxrs.model.OverdueFinePolicy`"));
  }

  @Test
  public void postOverdueFinesPoliciesInvalidUuid() {
    String invalidUuid = randomId() + "a";

    String payloadWithInvalidUuid = createEntityJson()
      .put("id", invalidUuid)
      .encodePrettily();

    JsonObject parameters = new JsonObject()
      .put("key", "overdue_fine_policy.id")
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
  public void postOverdueFinesPoliciesServerError() {
    client.getRequestSpecification()
      .header(OKAPI_HEADER_TENANT, "test_breaker")
      .body(createEntity())
      .post(REST_PATH)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
      .body(
        containsString("password authentication failed for user \"test_breaker_mod_feesfines\""));
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

  private Response post(String body) {
    return client.post(REST_PATH, body);
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
