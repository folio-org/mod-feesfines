package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.core.IsEqual.equalTo;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.tools.utils.NetworkUtils;

@RunWith(VertxUnitRunner.class)
public class FeeFineActionsAPITest {
  private static final Logger logger = LoggerFactory.getLogger(FeeFineActionsAPITest.class);

  private static final String OKAPI_URL = "x-okapi-url";
  private static final String HTTP_PORT = "http.port";
  private static final String REST_PATH = "/feefineactions";
  private static final String OKAPI_TOKEN = "test_token";
  private static final String OKAPI_URL_TEMPLATE = "http://localhost:%s";

  private static Vertx vertx;
  private static int port;
  private static String okapiTenant = "test_tenant";
  private static RequestSpecification spec;

  private String okapiUrl;

  @Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    Async async = context.async();
    vertx = Vertx.vertx();
    port = NetworkUtils.nextFreePort();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    TenantClient tenantClient =
      new TenantClient(String.format(OKAPI_URL_TEMPLATE, port), okapiTenant, OKAPI_TOKEN);
    DeploymentOptions restDeploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put(HTTP_PORT, port));

    vertx.deployVerticle(RestVerticle.class.getName(), restDeploymentOptions,
      res -> {
        try {
          tenantClient.postTenant(null, res2 -> async.complete()
          );
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
      });
  }

  @Before
  public void setUp(TestContext context) {
    Async async = context.async();
    PostgresClient.getInstance(vertx, okapiTenant)
      .delete(FeeFineActionsAPI.FEEFINEACTIONS_TABLE, new Criterion(), event -> {
        if (event.failed()) {
          logger.error(event.cause());
          context.fail(event.cause());
        } else {
          async.complete();
        }
      });
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Test
  public void postFeefineactionsWithPatronNotification() {
    setupPatronNoticeStub();

    final String ownerId = UUID.randomUUID().toString();
    final String feeFineId = UUID.randomUUID().toString();
    final String accountId = UUID.randomUUID().toString();
    final String defaultChargeTemplateId = UUID.randomUUID().toString();
    final String userId = UUID.randomUUID().toString();
    final String feeFineType = "damaged book";
    final String typeAction = "damaged book";
    final boolean notify = true;
    final double amountAction = 100;
    final double balance = 100;
    final double amount = 10;
    final String dateAction = "2019-12-23T14:25:59.550+0000";

    final String feeFineActionJson = createFeeFineActionJson(dateAction, typeAction, notify,
      amountAction, balance, accountId, userId);
    final String expectedNoticeJson = createNoticeJson(defaultChargeTemplateId,
      userId, feeFineType, typeAction, amountAction, balance, amount, dateAction);

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
      .put("amount", amount));

    createEntity("/feefines", new JsonObject()
      .put("id", feeFineId)
      .put("feeFineType", feeFineType)
      .put("ownerId", ownerId));

    post(feeFineActionJson)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON)
      .body(equalTo(feeFineActionJson));

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> userMockServer.verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(expectedNoticeJson))
      ));

  }

  @Test
  public void postFeefineactionsWithoutPatronNotification() {
    final String ownerId = UUID.randomUUID().toString();
    final String feeFineId = UUID.randomUUID().toString();
    final String accountId = UUID.randomUUID().toString();
    final String defaultChargeTemplateId = UUID.randomUUID().toString();
    final String userId = UUID.randomUUID().toString();
    final String feeFineType = "damaged book1";
    final String typeAction = "damaged book1";
    final boolean notify = false;
    final double amountAction = 100;
    final double balance = 100;
    final String dateAction = "2019-12-23T14:25:59.550+0000";
    final String feeFineActionJson = createFeeFineActionJson(dateAction, typeAction, notify,
      amountAction, balance, accountId, userId);

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

    post(feeFineActionJson)
      .then()
      .statusCode(201)
      .body(equalTo(feeFineActionJson));
  }

  private String createNoticeJson(String defaultChargeTemplateId,
                                  String userId, String feeFineType,
                                  String typeAction, double amountAction,
                                  double balance, double amount, String dateAction) {
    JsonObject noticeContext = createNoticeContext(feeFineType, typeAction,
      amountAction, balance, amount, dateAction);

    return new JsonObject()
      .put("recipientId", userId)
      .put("deliveryChannel", "email")
      .put("templateId", defaultChargeTemplateId)
      .put("outputFormat", "text/html")
      .put("lang", "en")
      .put("context", noticeContext)
      .encodePrettily();
  }

  private JsonObject createNoticeContext(String feeFineType,
                                         String typeAction,
                                         double amountAction,
                                         double balance,
                                         double amount,
                                         String dateAction) {
    return new JsonObject()
      .put("fee", new JsonObject()
        .put("owner", "library")
        .put("type", feeFineType)
        .put("amount", amount)
        .put("actionType", typeAction)
        .put("actionAmount", amountAction)
        .put("actionDateTime", dateAction)
        .put("balance", balance)
        .put("actionAdditionalInfo", "patron comment")
        .put("reasonForCancellation", "staff comment"));
  }

  private String createFeeFineActionJson(String dateAction, String typeAction,
                                         boolean notify, double amountAction,
                                         double balance, String accountId,
                                         String userId) {
    return new JsonObject()
      .put("dateAction", dateAction)
      .put("typeAction", typeAction)
      .put("comments", "STAFF : staff comment \n PATRON : patron comment")
      .put("notify", notify)
      .put("amountAction", amountAction)
      .put("balance", balance)
      .put("transactionInformation", "-")
      .put("createdAt", "Test")
      .put("source", "ADMINISTRATOR, DIKU")
      .put("accountId", accountId)
      .put("userId", userId)
      .put("id", UUID.randomUUID().toString())
      .encodePrettily();

  }

  private Response post(String body) {
    return getRequestSpecification()
      .body(body)
      .when()
      .post(REST_PATH);
  }

  private RequestSpecification getRequestSpecification() {
    if (okapiUrl == null) {
      okapiUrl = String.format(OKAPI_URL_TEMPLATE, userMockServer.port());
    }

    return RestAssured.given()
      .port(port)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, okapiTenant))
      .header(new Header(OKAPI_URL, okapiUrl))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN));
  }

  private void setupPatronNoticeStub() {
    userMockServer.stubFor(WireMock.post(urlPathEqualTo("/patron-notice"))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(okapiTenant))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL, matching(userMockServer.baseUrl()))
      .willReturn(ok()));
  }

  private void createEntity(String path, JsonObject entity) {
    RestAssured.given()
      .spec(getRequestSpecification())
      .body(entity.encode())
      .when()
      .post(path)
      .then()
      .statusCode(201);
  }
}

