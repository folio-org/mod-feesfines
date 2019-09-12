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
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.Ignore;
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

  private static Vertx vertx;
  private static String okapiUrl;
  private static WireMockServer wireMockServer;
  private static RequestSpecification spec;

  @BeforeAll
  static void beforeAll(Vertx vertx, VertxTestContext context) throws IOException {
    PatronNoticeTest.vertx = vertx;
    wireMockServer = new WireMockServer(NetworkUtils.nextFreePort());
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

    deployRestVerticle(okapiPort)
      .compose(deploy -> postTenant())
      .setHandler(post -> context.completeNow());
  }

  @AfterAll
  static void afterAll() {
    wireMockServer.stop();
    PostgresClient.stopEmbeddedPostgres();
  }

  @Test
  void manualChargeNoticeShouldBeSentWithDefaultTemplate() {
    String ownerId = UUID.randomUUID().toString();
    String feeFineId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String defaultChargeTemplateId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();
    String feeFineType = "damaged book";
    String typeAction = "damaged book";

    createEntity("/owners", new JsonObject()
      .put("id", ownerId)
      .put("owner", "library")
      .put("defaultChargeNoticeId", defaultChargeTemplateId));

    createEntity("/accounts", new JsonObject()
      .put("id", accountId)
      .put("userId", userId)
      .put("itemId", UUID.randomUUID().toString())
      .put("materialTypeId", UUID.randomUUID().toString())
      .put("feeFineId", feeFineId)
      .put("ownerId", ownerId)
      .put("amount", 10.0));

    createEntity("/feefines", new JsonObject()
      .put("id", feeFineId)
      .put("feeFineType", feeFineType)
      .put("ownerId", ownerId));

    createEntity("/feefineactions", new JsonObject()
      .put("userId", userId)
      .put("accountId", accountId)
      .put("typeAction", typeAction)
      .put("amountAction", 10.0)
      .put("balance", 10.0)
      .put("dateAction", "2019-09-17T08:43:15.000+0000")
      .put("comments", "STAFF : staff comment \n PATRON : patron comment"));

    JsonObject noticeContext = new JsonObject()
      .put("fee", new JsonObject()
        .put("owner", "library")
        .put("type", feeFineType)
        .put("amount", 10.0)
        .put("actionType", typeAction)
        .put("actionAmount", 10.0)
        .put("actionDateTime", "2019-09-17T08:43:15.000+0000")
        .put("balance", 10.0)
        .put("actionAdditionalInfo", "patron comment"));

    JsonObject notice = new JsonObject()
      .put("recipientId", userId)
      .put("deliveryChannel", "email")
      .put("templateId", defaultChargeTemplateId)
      .put("outputFormat", "text/html")
      .put("lang", "en")
      .put("context", noticeContext);

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> wireMockServer.verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(notice.encode()))
      ));
  }

  @Test
  void manualChargeNoticeShouldBeSentWithSpecificTemplate() {
    String ownerId = UUID.randomUUID().toString();
    String feeFineId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String defaultChargeTemplateId = UUID.randomUUID().toString();
    String specificChargeTemplateId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();
    String feeFineType = "damaged book";
    String typeAction = "damaged book";

    createEntity("/owners", new JsonObject()
      .put("id", ownerId)
      .put("owner", "library")
      .put("defaultChargeNoticeId", defaultChargeTemplateId));

    createEntity("/accounts", new JsonObject()
      .put("id", accountId)
      .put("userId", userId)
      .put("itemId", UUID.randomUUID().toString())
      .put("materialTypeId", UUID.randomUUID().toString())
      .put("feeFineId", feeFineId)
      .put("ownerId", ownerId)
      .put("amount", 10.0));

    createEntity("/feefines", new JsonObject()
      .put("id", feeFineId)
      .put("feeFineType", feeFineType)
      .put("ownerId", ownerId)
      .put("chargeNoticeId", specificChargeTemplateId));

    createEntity("/feefineactions", new JsonObject()
      .put("userId", userId)
      .put("accountId", accountId)
      .put("typeAction", typeAction)
      .put("amountAction", 10.0)
      .put("balance", 10.0)
      .put("dateAction", "2019-09-17T08:43:15.000+0000")
      .put("comments", "STAFF : staff comment \n PATRON : patron comment"));

    JsonObject noticeContext = new JsonObject()
      .put("fee", new JsonObject()
        .put("owner", "library")
        .put("type", feeFineType)
        .put("amount", 10.0)
        .put("actionType", typeAction)
        .put("actionAmount", 10.0)
        .put("actionDateTime", "2019-09-17T08:43:15.000+0000")
        .put("balance", 10.0)
        .put("actionAdditionalInfo", "patron comment"));

    JsonObject notice = new JsonObject()
      .put("recipientId", userId)
      .put("deliveryChannel", "email")
      .put("templateId", specificChargeTemplateId)
      .put("outputFormat", "text/html")
      .put("lang", "en")
      .put("context", noticeContext);

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> wireMockServer.verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(notice.encode()))
      ));
  }

  @Test
  void actionNoticeShouldBeSentWithDefaultTemplate() {
    String ownerId = UUID.randomUUID().toString();
    String feeFineId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String defaultActionTemplateId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();
    String feeFineType = "damaged book";
    String typeAction = "Paid fully";

    createEntity("/owners", new JsonObject()
      .put("id", ownerId)
      .put("owner", "library")
      .put("defaultActionNoticeId", defaultActionTemplateId));

    createEntity("/accounts", new JsonObject()
      .put("id", accountId)
      .put("userId", userId)
      .put("itemId", UUID.randomUUID().toString())
      .put("materialTypeId", UUID.randomUUID().toString())
      .put("feeFineId", feeFineId)
      .put("ownerId", ownerId)
      .put("amount", 10.0));

    createEntity("/feefines", new JsonObject()
      .put("id", feeFineId)
      .put("feeFineType", feeFineType)
      .put("ownerId", ownerId));

    createEntity("/feefineactions", new JsonObject()
      .put("userId", userId)
      .put("accountId", accountId)
      .put("typeAction", typeAction)
      .put("amountAction", 10.0)
      .put("balance", 0.0)
      .put("dateAction", "2019-09-17T08:43:15.000+0000")
      .put("paymentMethod", "credit card")
      .put("comments", "STAFF : staff comment \n PATRON : patron comment"));

    JsonObject noticeContext = new JsonObject()
      .put("fee", new JsonObject()
        .put("owner", "library")
        .put("type", feeFineType)
        .put("amount", 10.0)
        .put("actionType", typeAction)
        .put("actionAmount", 10.0)
        .put("actionDateTime", "2019-09-17T08:43:15.000+0000")
        .put("balance", 0.0)
        .put("actionAdditionalInfo", "patron comment"));

    JsonObject notice = new JsonObject()
      .put("recipientId", userId)
      .put("deliveryChannel", "email")
      .put("templateId", defaultActionTemplateId)
      .put("outputFormat", "text/html")
      .put("lang", "en")
      .put("context", noticeContext);

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> wireMockServer.verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(notice.encode()))
      ));
  }

  @Test
  @Ignore
  void actionNoticeShouldBeSentWithSpecificTemplate() {
    String ownerId = UUID.randomUUID().toString();
    String feeFineId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String defaultActionTemplateId = UUID.randomUUID().toString();
    String specificActionTemplateId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();
    String feeFineType = "damaged book";
    String typeAction = "Paid fully";

    createEntity("/owners", new JsonObject()
      .put("id", ownerId)
      .put("owner", "library")
      .put("defaultActionNoticeId", defaultActionTemplateId));

    createEntity("/accounts", new JsonObject()
      .put("id", accountId)
      .put("userId", userId)
      .put("itemId", UUID.randomUUID().toString())
      .put("materialTypeId", UUID.randomUUID().toString())
      .put("feeFineId", feeFineId)
      .put("ownerId", ownerId)
      .put("amount", 10.0));

    createEntity("/feefines", new JsonObject()
      .put("id", feeFineId)
      .put("feeFineType", feeFineType)
      .put("ownerId", ownerId)
      .put("actionNoticeId", specificActionTemplateId));

    createEntity("/feefineactions", new JsonObject()
      .put("userId", userId)
      .put("accountId", accountId)
      .put("typeAction", typeAction)
      .put("amountAction", 10.0)
      .put("balance", 0.0)
      .put("dateAction", "2019-09-17T08:43:15.000+0000")
      .put("paymentMethod", "credit card")
      .put("comments", "STAFF : staff comment \n PATRON : patron comment"));

    JsonObject noticeContext = new JsonObject()
      .put("fee", new JsonObject()
        .put("owner", "library")
        .put("type", feeFineType)
        .put("amount", 10.0)
        .put("actionType", typeAction)
        .put("actionAmount", 10.0)
        .put("actionDateTime", "2019-09-17T08:43:15.000+0000")
        .put("balance", 0.0)
        .put("actionAdditionalInfo", "patron comment"));

    JsonObject notice = new JsonObject()
      .put("recipientId", userId)
      .put("deliveryChannel", "email")
      .put("templateId", specificActionTemplateId)
      .put("outputFormat", "text/html")
      .put("lang", "en")
      .put("context", noticeContext);

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

  private static void setupStub() {
    wireMockServer.stubFor(post(urlPathEqualTo("/patron-notice"))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT))
      .withHeader(OKAPI_HEADER_TOKEN, matching(TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(wireMockServer.baseUrl()))
      .willReturn(ok()));
  }

  private void createEntity(String path, JsonObject entity) {
    RestAssured.given()
      .spec(spec)
      .body(entity.encode())
      .when()
      .post(path)
      .then()
      .statusCode(201);
  }
}
