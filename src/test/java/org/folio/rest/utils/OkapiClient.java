package org.folio.rest.utils;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import java.util.Base64;

import javax.ws.rs.core.MediaType;

import org.folio.rest.tools.utils.NetworkUtils;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

public class OkapiClient {
  private static final String TEST_TENANT = "test_tenant";
  private static final String OKAPI_HEADER_URL = "x-okapi-url";

  private final RequestSpecification requestSpecification;

  private final int port;
  private final String tenant;
  private final String url;
  private final String token;

  public OkapiClient() {
    token = buildToken();
    port = NetworkUtils.nextFreePort();
    url = "http://localhost:" + port;
    tenant = TEST_TENANT;
    requestSpecification = RestAssured.given()
      .baseUri(url)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TEST_TENANT))
      .header(new Header(OKAPI_HEADER_URL, url))
      .header(new Header(OKAPI_HEADER_TOKEN, token));
  }

  public String getToken() {
    return token;
  }

  public int getPort() {
    return port;
  }

  public String getUrl() {
    return url;
  }

  public String getTenant() {
    return tenant;
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

  private static String buildToken() {
    byte[] jsonBytes = new JsonObject()
      .put("user_id", "test_user")
      .toString()
      .getBytes();

    return "test." + Base64.getEncoder().encodeToString(jsonBytes);
  }
}
