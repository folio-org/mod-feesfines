package org.folio.rest.utils;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import javax.ws.rs.core.MediaType;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class OkapiClient {
  private static final String OKAPI_HEADER_URL = "x-okapi-url";
  private final RequestSpecification requestSpecification;

  public OkapiClient(String url, String tenant, String token) {
    requestSpecification = RestAssured.given()
      .baseUri(url)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, tenant))
      .header(new Header(OKAPI_HEADER_URL, url))
      .header(new Header(OKAPI_HEADER_TOKEN, token));
  }

  public Response get(String url) {
    return requestSpecification
      .when()
      .get(url);
  }

  public Response put(String url, String body) {
    return requestSpecification
      .body(body)
      .when()
      .put(url);
  }

  public Response delete(String url) {
    return requestSpecification
      .when()
      .delete(url);
  }

  public Response post(String url, String body) {
    return requestSpecification
      .body(body)
      .when()
      .post(url);
  }
}
