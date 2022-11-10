package org.folio.rest.impl;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;

import java.util.List;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.test.support.ApiTests;
import org.junit.jupiter.api.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

public class DoNotBillActualCostLostItemFeeAPITest extends ApiTests {
  private static final String REST_PATH = "/do-not-bill-actual-cost-lost-item-fee";

  @Test
  public void canPostDoNotBillActualCostEntity() {
    String actualCostRecordId = randomId();

    String doNotBilActualCostJson = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();

    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(ActualCostRecord.Status.OPEN);

    createStub(format("/actual-cost-record-storage/actual-cost-records/%s", actualCostRecordId),
      actualCostRecord);

    post(doNotBilActualCostJson)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON)
      .body(allOf(List.of(
        hasJsonPath("actualCostRecordId", is(actualCostRecordId)),
        hasJsonPath("additionalInfoForStaff", is("Test info for staff")),
        hasJsonPath("additionalInfoForPatron", is("Test info for patron"))
      )));
  }

  @Test
  public void postDoNotBillActualCostEntityShouldFailIfRecordIsNotFound() {
    String actualCostRecordId = randomId();

    String doNotBilActualCostJson = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();

    createStubWith404Status(format("/actual-cost-record-storage/actual-cost-records/%s",
      actualCostRecordId));

    post(doNotBilActualCostJson)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  private Response post(String entity) {
    return client.post(REST_PATH, entity);
  }
}
