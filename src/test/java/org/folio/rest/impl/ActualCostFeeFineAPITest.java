package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;

import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.rest.jaxrs.model.ActualCostRecordLoan;
import org.folio.test.support.ApiTests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

class ActualCostFeeFineAPITest extends ApiTests {
  private static final String ACTUAL_COST_CANCEL_PATH = "/actual-cost-fee-fine/cancel";
  public static final String ACTUAL_COST_RECORDS_PATH = "/actual-cost-record-storage/actual-cost-records/%s";

  @Test
  void canPostActualCostCancelEntity() {
    String actualCostRecordId = randomId();

    String actualCostFeeFineCancel = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();

    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(ActualCostRecord.Status.OPEN)
      .withLoan(new ActualCostRecordLoan().withId(UUID.randomUUID().toString()));

    createStub(format(ACTUAL_COST_RECORDS_PATH, actualCostRecordId), actualCostRecord);
    createStub(WireMock.put(urlPathEqualTo(format(ACTUAL_COST_RECORDS_PATH, actualCostRecordId))),
      aResponse().withStatus(HttpStatus.SC_NO_CONTENT));

    postActualCostCancel(actualCostFeeFineCancel)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON)
      .body(allOf(List.of(
        hasJsonPath("id", is(actualCostRecordId)),
        hasJsonPath("additionalInfoForStaff", is("Test info for staff")),
        hasJsonPath("additionalInfoForPatron", is("Test info for patron")),
        hasJsonPath("status", is("Cancelled"))
      )));
  }

  @Test
  void postActualCostCancelShouldFailIfRecordIsNotFoundOnRetrieve() {
    String actualCostRecordId = randomId();
    String actualCostCancelEntity = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();
    createStub(WireMock.get(urlPathEqualTo(format(ACTUAL_COST_RECORDS_PATH, actualCostRecordId))),
      aResponse().withStatus(HttpStatus.SC_NOT_FOUND));

    postActualCostCancel(actualCostCancelEntity)
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .body(startsWith(format("Actual cost record %s was not found", actualCostRecordId)));
  }

  @Test
  void postActualCostCancelShouldFailIfRecordIsNotFoundOnUpdate() {
    String actualCostRecordId = randomId();
    String actualCostCancelEntity = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();
    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(ActualCostRecord.Status.OPEN);

    createStub(format(ACTUAL_COST_RECORDS_PATH, actualCostRecordId), actualCostRecord);
    createStub(WireMock.put(urlPathEqualTo(format(ACTUAL_COST_RECORDS_PATH, actualCostRecordId))),
      aResponse().withStatus(HttpStatus.SC_NOT_FOUND));

    postActualCostCancel(actualCostCancelEntity)
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .body(startsWith(format("Actual cost record %s was not found", actualCostRecordId)));
  }

  @Test
  void postActualCostCancelShouldFailIfBadRequestOnUpdate() {
    String actualCostRecordId = randomId();
    String actualCostCancelEntity = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();
    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(ActualCostRecord.Status.OPEN);

    createStub(format(ACTUAL_COST_RECORDS_PATH, actualCostRecordId), actualCostRecord);
    createStub(WireMock.put(urlPathEqualTo(format(ACTUAL_COST_RECORDS_PATH, actualCostRecordId))),
      aResponse().withStatus(HttpStatus.SC_BAD_REQUEST));

    postActualCostCancel(actualCostCancelEntity)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @ParameterizedTest
  @EnumSource(value = ActualCostRecord.Status.class, names = {"CANCELLED", "BILLED", "EXPIRED"})
  void postActualCostCancelShouldFailIfRecordAlreadyProcessed(ActualCostRecord.Status recordStatus) {
    String actualCostRecordId = randomId();
    String actualCostCancelEntity = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();
    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(recordStatus);
    createStub(format(ACTUAL_COST_RECORDS_PATH, actualCostRecordId), actualCostRecord);

    postActualCostCancel(actualCostCancelEntity)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(startsWith(format("Actual cost record %s is already %s", actualCostRecordId, recordStatus)));
  }

  private Response postActualCostCancel(String entity) {
    return client.post(ACTUAL_COST_CANCEL_PATH, entity);
  }
}
