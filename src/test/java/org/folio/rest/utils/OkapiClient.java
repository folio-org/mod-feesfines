package org.folio.rest.utils;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.test.support.ApiTests.OKAPI_TOKEN;
import static org.folio.test.support.ApiTests.OKAPI_URL_HEADER;
import static org.folio.test.support.ApiTests.TENANT_NAME;

import javax.ws.rs.core.MediaType;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.Json;

public class OkapiClient {
  private final String okapiUrl;

  public OkapiClient(String okapiUrl) {
    this.okapiUrl = okapiUrl;
  }

  public Response get(String uri) {
    return getRequestSpecification()
      .when()
      .get(uri);
  }

  public Response post(String uri, Object body) {
    return getRequestSpecification()
      .when()
      .body(encodeBody(body))
      .post(uri);
  }

  public Response put(String uri, Object body) {
    return getRequestSpecification()
      .when()
      .body(encodeBody(body))
      .put(uri);
  }

  public Response delete(String uri) {
    return getRequestSpecification()
      .when()
      .delete(uri);
  }

  private String encodeBody(Object body) {
    return body instanceof String ? body.toString() : Json.encode(body);
  }

  public RequestSpecification getRequestSpecification() {
    return RestAssured.given()
      .baseUri(okapiUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TENANT_NAME))
      .header(new Header(OKAPI_URL_HEADER, okapiUrl))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN));
  }
}
