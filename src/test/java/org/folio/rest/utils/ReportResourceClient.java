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

  public Response getFeeFineRefundReports(String startDate, String endDate) {
    return getFeeFineRefundReports(startDate, endDate, null, HttpStatus.HTTP_OK);
  }

  public Response getFeeFineRefundReports(String startDate, String endDate,
    List<String> ownerIds) {
    return getFeeFineRefundReports(startDate, endDate, ownerIds, HttpStatus.HTTP_OK);
  }

  public Response getFeeFineRefundReports(String startDate, String endDate, List<String> ownerIds,
    HttpStatus expectedStatus) {

    return okapiClient.post(baseUri, createRefundReportRequest(startDate, endDate, ownerIds))
      .then()
      .statusCode(expectedStatus.toInt())
      .extract()
      .response();
  }

  public Response getFeeFineRefundReports(String startDate, String endDate,
    HttpStatus expectedStatus) {

    return getFeeFineRefundReports(startDate, endDate, null, expectedStatus);
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
}
