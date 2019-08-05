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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.awaitility.Awaitility;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Owner;
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
  private static final String TENANT = "test_tenant";
  private static final String TOKEN = "dummy-TOKEN";
  private static final String USER_ID = "ff4acb6f-dcb3-4535-a81d-e2cac77f6a01";
  private static final String OWNER_ID = "6c67a895-f293-476f-9354-8923851b1ebd";
  private static final String CHARGE_TEMPLATE_ID = "2374fd04-5611-4db2-b949-cffbd7ec13b8";
  private static final String DEFAULT_CHARGE_TEMPLATE_ID = "5e7315f4-93fb-4fe0-b9a4-a6abfd22b1cd";

  private static Vertx vertx;
  private static String okapiUrl;
  private static WireMockServer wireMockServer;
  private static RequestSpecification spec;
  private static PostgresClient pgClient;

  @BeforeAll
  static void beforeAll(Vertx vertx, VertxTestContext context) throws IOException {

    PatronNoticeTest.vertx = vertx;
    wireMockServer = new WireMockServer();
    wireMockServer.start();
    setupStub();

    int okapiPort = NetworkUtils.nextFreePort();
    okapiUrl = "http://localhost:" + okapiPort;

    spec = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .setBaseUri(okapiUrl)
      .addHeader(OKAPI_HEADER_TENANT, TENANT)
      .addHeader(OKAPI_HEADER_TOKEN, TOKEN)
      .addHeader(OKAPI_URL_HEADER, wireMockServer.baseUrl())
      .build();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    pgClient = PostgresClient.getInstance(vertx, TENANT);
    pgClient.setIdField("id");

    Owner owner = new Owner()
      .withId(OWNER_ID)
      .withDefaultChargeNoticeId(DEFAULT_CHARGE_TEMPLATE_ID);

    deployRestVerticle(okapiPort)
      .compose(deploy -> postTenant())
      .compose(post -> persistOwner(owner))
      .setHandler(persist -> context.completeNow());
  }

  @AfterAll
  static void afterAll() {
    wireMockServer.stop();
    PostgresClient.stopEmbeddedPostgres();
  }

  @Test
  void manualChargeNoticeShouldBeSentWithDefaultTemplate()
    throws InterruptedException, ExecutionException, TimeoutException {

    Feefine feefine = new Feefine()
      .withOwnerId(OWNER_ID)
      .withId(UUID.randomUUID().toString());
    persistFeeFine(feefine).get(5, TimeUnit.SECONDS);

    JsonObject account = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("userId", USER_ID)
      .put("itemId", UUID.randomUUID().toString())
      .put("materialTypeId", UUID.randomUUID().toString())
      .put("feeFineId", feefine.getId())
      .put("ownerId", OWNER_ID);

    RestAssured.given()
      .spec(spec)
      .body(account.encode())
      .when()
      .post("/accounts")
      .then()
      .statusCode(201);

    JsonObject notice = new JsonObject()
      .put("recipientId", USER_ID)
      .put("deliveryChannel", "email")
      .put("templateId", DEFAULT_CHARGE_TEMPLATE_ID)
      .put("outputFormat", "text/html")
      .put("lang", "en");

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> wireMockServer.verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(notice.encode()))
      ));
  }

  @Test
  void manualChargeNoticeShouldBeSentWithSpecificTemplate()
    throws InterruptedException, ExecutionException, TimeoutException {

    Feefine feefine = new Feefine()
      .withId(UUID.randomUUID().toString())
      .withOwnerId(OWNER_ID)
      .withChargeNoticeId(CHARGE_TEMPLATE_ID);
    persistFeeFine(feefine).get(5, TimeUnit.SECONDS);

    JsonObject account = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("userId", USER_ID)
      .put("itemId", UUID.randomUUID().toString())
      .put("materialTypeId", UUID.randomUUID().toString())
      .put("feeFineId", feefine.getId())
      .put("ownerId", OWNER_ID);

    RestAssured.given()
      .spec(spec)
      .body(account.encode())
      .when()
      .post("/accounts")
      .then()
      .statusCode(201);

    JsonObject notice = new JsonObject()
      .put("recipientId", USER_ID)
      .put("deliveryChannel", "email")
      .put("templateId", CHARGE_TEMPLATE_ID)
      .put("outputFormat", "text/html")
      .put("lang", "en");

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> wireMockServer.verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(notice.encode()))
      ));
  }

  private static Future<String> deployRestVerticle(int port) {
    Future<String> future = Future.future();
    vertx.deployVerticle(RestVerticle::new,
      new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", port)), future);
    return future.map(deploy -> null);
  }

  private static Future<Void> postTenant() {
    Future<Void> future = Future.future();
    TenantClient tenantClient = new TenantClient(
      okapiUrl, TENANT, TOKEN, false);

    try {
      tenantClient.postTenant(null, post -> future.complete());
    } catch (Exception e) {
      future.fail(e);
    }
    return future;
  }

  private static Future<String> persistOwner(Owner owner) {
    Future<String> future = Future.future();
    pgClient.save("owners", owner.getId(), owner, future);
    return future;
  }

  private static void setupStub() {
    wireMockServer.stubFor(post(urlPathEqualTo("/patron-notice"))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT))
      .withHeader(OKAPI_HEADER_TOKEN, matching(TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(wireMockServer.baseUrl()))
      .willReturn(ok()));
  }

  private CompletableFuture<Void> persistFeeFine(Feefine feefine) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    pgClient.save("feefines", feefine.getId(), feefine, save -> future.complete(null));
    return future;
  }
}
