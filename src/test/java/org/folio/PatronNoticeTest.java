package org.folio;


import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
class PatronNoticeTest {

  private static final String OKAPI_URL_HEADER = "x-okapi-url";

  private static String userId = "ff4acb6f-dcb3-4535-a81d-e2cac77f6a01";
  private static String ownerId = "6c67a895-f293-476f-9354-8923851b1ebd";
  private static String feeFineId = "72410bd6-a8ee-4764-bb7e-8b882e3ab18e";
  private static String chargeNoticeTemplateId = "2374fd04-5611-4db2-b949-cffbd7ec13b8";
  private static String tenant = "test_tenant";
  private static String token = "dummy-token";

  private static WireMockServer wireMockServer;
  private static RequestSpecification spec;
  private static PostgresClient pgClient;

  @BeforeAll
  static void beforeAll(Vertx vertx, VertxTestContext context) throws IOException {

    wireMockServer = new WireMockServer();
    wireMockServer.start();
    setupStub();

    int okapiPort = NetworkUtils.nextFreePort();
    String okapiUrl = "http://localhost:" + okapiPort;

    spec = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .setBaseUri(okapiUrl)
      .addHeader(OKAPI_HEADER_TENANT, tenant)
      .addHeader(OKAPI_HEADER_TOKEN, token)
      .addHeader(OKAPI_URL_HEADER, wireMockServer.baseUrl())
      .build();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    pgClient = PostgresClient.getInstance(vertx, tenant);
    pgClient.setIdField("id");

    TenantClient tenantClient = new TenantClient(
      okapiUrl, tenant, token, false);

    vertx.deployVerticle(RestVerticle::new,
      new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", okapiPort)), deploy -> {
        try {
          tenantClient.postTenant(null, post -> saveFeeFine()
            .setHandler(context.succeeding(save -> context.completeNow())));
        } catch (Exception e) {
          context.failNow(e);
        }
      });
  }

  @AfterAll
  static void afterAll() {
    wireMockServer.stop();
    PostgresClient.stopEmbeddedPostgres();
  }

  @Test
  void manualChargeNoticeShouldBeSentOnAccountCreation() {
    JsonObject account = new JsonObject()
      .put("id", "3140f801-2eba-470e-a30d-d56e7de485f5")
      .put("userId", userId)
      .put("itemId", "c8bf05d5-22a4-462e-9e07-01963dd4faa4")
      .put("materialTypeId", "8ca7cc49-2e4f-48d9-938c-18790f77ae1d")
      .put("feeFineId", feeFineId)
      .put("ownerId", ownerId);

    RestAssured.given()
      .spec(spec)
      .body(account.encode())
      .when()
      .post("/accounts")
      .then()
      .statusCode(201);

    JsonObject notice = new JsonObject()
      .put("recipientId", userId)
      .put("deliveryChannel", "email")
      .put("templateId", chargeNoticeTemplateId)
      .put("outputFormat", "text/html")
      .put("lang", "en");

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> wireMockServer.verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(notice.encode()))
      ));
  }

  private static Future<String> saveFeeFine() {
    Feefine feefine = new Feefine()
      .withId(feeFineId)
      .withChargeNoticeId(chargeNoticeTemplateId);

    Future<String> future = Future.future();
    pgClient.save("feefines", feeFineId, feefine, future);
    return future;
  }

  private static void setupStub() {
    wireMockServer.stubFor(post(urlPathEqualTo("/patron-notice"))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(tenant))
      .withHeader(OKAPI_HEADER_TOKEN, matching(token))
      .withHeader(OKAPI_URL_HEADER, matching(wireMockServer.baseUrl()))
      .willReturn(ok()));
  }
}
