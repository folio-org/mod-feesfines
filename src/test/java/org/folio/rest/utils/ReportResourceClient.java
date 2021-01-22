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

    return okapiClient.post(baseUri, createRefundReportRequest(startDate, endDate, null))
      .then()
      .statusCode(expectedStatus.toInt())
      .extract()
      .response();
  }

  private String createRefundReportRequest(String startDate, String endDate,
    List<String> ownerIds) {

    JsonArray feeFineOwners = new JsonArray();
    if (ownerIds != null) {
      ownerIds.forEach(feeFineOwners::add);
    }

    return new JsonObject()
      .put("startDate", startDate)
      .put("endDate", endDate)
      .put("feeFineOwners", feeFineOwners)
      .encodePrettily();
  }
}
