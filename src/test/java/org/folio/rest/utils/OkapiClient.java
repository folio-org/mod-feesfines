package org.folio.rest.utils;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import javax.ws.rs.core.MediaType;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class OkapiClient {
  private static final String TEST_TENANT = "test_tenant";
  private static final String OKAPI_URL = "x-okapi-url";

  private final String okapiUrl;

  public OkapiClient(int okapiPort) {
    this.okapiUrl = "http://localhost:" + okapiPort;
  }

  public Response get(String uri) {
    return getRequestSpecification()
      .when()
      .get(uri);
  }

  private RequestSpecification getRequestSpecification() {
    return RestAssured.given()
      .baseUri(okapiUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TEST_TENANT))
      .header(new Header(OKAPI_URL, okapiUrl))
      .header(new Header(OKAPI_HEADER_TOKEN, "test_token"));
  }
}
