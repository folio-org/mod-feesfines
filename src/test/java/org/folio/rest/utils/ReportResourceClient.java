package org.folio.rest.utils;

import static java.lang.String.format;

import org.folio.HttpStatus;

import io.restassured.response.Response;

public class ReportResourceClient extends ResourceClient {
  public ReportResourceClient(String baseUri) {
    super(baseUri);
  }

  public Response getByDateInterval(String startDate, String endDate) {
    return getByDateInterval(startDate, endDate, HttpStatus.HTTP_OK);
  }

  public Response getByDateInterval(String startDate, String endDate, HttpStatus expectedStatus) {
    return getByParameters(format("startDate=%s&endDate=%s", startDate, endDate),
      expectedStatus);
  }

  public Response getByParameters(String parameters, HttpStatus expectedStatus) {
    return okapiClient.get(format("%s?%s", baseUri, parameters))
      .then()
      .statusCode(expectedStatus.toInt())
      .extract()
      .response();
  }
}
