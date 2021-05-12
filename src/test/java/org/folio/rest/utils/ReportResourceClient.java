package org.folio.rest.utils;

import java.util.List;

import org.folio.HttpStatus;

import io.restassured.response.Response;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ReportResourceClient extends ResourceClient {
  public ReportResourceClient(String baseUri) {
    super(baseUri);
  }

  public Response getFeeFineRefundReport(String startDate, String endDate,
    List<String> ownerIds) {

    return getFeeFineRefundReport(startDate, endDate, ownerIds, HttpStatus.HTTP_OK);
  }

  public Response getFeeFineRefundReport(String startDate, String endDate,
    HttpStatus expectedStatus) {

    return getFeeFineRefundReport(startDate, endDate, null, expectedStatus);
  }

  private Response getFeeFineRefundReport(String startDate, String endDate, List<String> ownerIds,
    HttpStatus expectedStatus) {

    return getReport(createRefundReportRequest(startDate, endDate, ownerIds), expectedStatus);
  }

  public Response getCashDrawerReconciliationReport(String startDate, String endDate,
    String createdAt, List<String> sources) {

    return getCashDrawerReconciliationReport(startDate, endDate, createdAt, sources,
      HttpStatus.HTTP_OK);
  }

  public Response getCashDrawerReconciliationReport(String startDate, String endDate,
    String createdAt, List<String> sources, HttpStatus expectedStatus) {

    return getReport(createCashDrawerReconciliationReportRequest(startDate, endDate,
      createdAt, sources), expectedStatus);
  }

  public Response getCashDrawerReconciliationReportSources(String createdAt) {

    return getCashDrawerReconciliationReportSources(createdAt, HttpStatus.HTTP_OK);
  }

  public Response getCashDrawerReconciliationReportSources(String createdAt,
    HttpStatus expectedStatus) {

    return getReport(createCashDrawerReconciliationReportSourcesRequest(createdAt), expectedStatus);
  }

  private String createRefundReportRequest(String startDate, String endDate,
    List<String> ownerIds) {

    JsonArray feeFineOwners = null;
    if (ownerIds != null) {
      feeFineOwners = new JsonArray(ownerIds);
    }

    return new JsonObject()
      .put("startDate", startDate)
      .put("endDate", endDate)
      .put("feeFineOwners", feeFineOwners)
      .encodePrettily();
  }

  private String createCashDrawerReconciliationReportRequest(String startDate, String endDate,
    String createdAt, List<String> sources) {

    JsonArray sourceArray = null;
    if (sources != null) {
      sourceArray = new JsonArray(sources);
    }

    return new JsonObject()
      .put("startDate", startDate)
      .put("endDate", endDate)
      .put("createdAt", createdAt)
      .put("sources", sourceArray)
      .encodePrettily();
  }

  private String createCashDrawerReconciliationReportSourcesRequest(String createdAt) {
    return new JsonObject()
      .put("createdAt", createdAt)
      .encodePrettily();
  }

  private Response getReport(String requestBody, HttpStatus expectedStatus) {
    return okapiClient.post(baseUri, requestBody)
      .then()
      .statusCode(expectedStatus.toInt())
      .extract()
      .response();
  }
}
