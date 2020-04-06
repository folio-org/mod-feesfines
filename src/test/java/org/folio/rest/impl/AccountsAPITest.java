package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AccountsAPITest {
  private static final Logger logger = LoggerFactory.getLogger(FeeFinesAPITest.class);

  private static final String OKAPI_URL = "x-okapi-url";
  private static final String HTTP_PORT = "http.port";
  private static final String REST_PATH = "/accounts";
  private static final String OKAPI_TOKEN = "test_token";
  private static final String OKAPI_URL_TEMPLATE = "http://localhost:%s";
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String ITEM_ID = "43ec57e3-3974-4d05-a2c2-95126e087b72";

  private static Vertx vertx;
  private static int port;
  private static String okapiTenant = "test_tenant";

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
    TenantAttributes attributes = new TenantAttributes()
      .withModuleTo(String.format("mod-feesfines-%s", PomReader.INSTANCE.getVersion()));

    vertx.deployVerticle(RestVerticle.class.getName(), restDeploymentOptions,
      res -> {
        try {
          tenantClient.postTenant(attributes, res2 -> async.complete()
          );
        } catch (Exception e) {
          logger.error(e.getMessage());
        }
      });
  }

  @Before
  public void setUp(TestContext context) {
    Async async = context.async();
    PostgresClient client = PostgresClient.getInstance(vertx, okapiTenant);

    userMockServer.stubFor(WireMock.get(WireMock.urlPathMatching("/inventory/items.*"))
      .willReturn(aResponse().withBodyFile("items.json")));

    userMockServer.stubFor(WireMock.get(WireMock.urlPathMatching("/holdings-storage/holdings.*"))
      .willReturn(aResponse().withBodyFile("holdings.json")));

    client.delete(ACCOUNTS_TABLE, new Criterion(), result -> processEvent(context, result, async));
    async.complete();
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
  public void canGetAccounts() {
    post(createAccountJson(randomId()))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    get("/accounts")
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(ContentType.JSON);
  }

  @Test
  public void canGetAccount() {
    String accountId = randomId();

    post(createAccountJson(accountId))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    get(String.format("/accounts/%s", accountId))
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(ContentType.JSON);
  }

  private String createAccountJson(String accountID) {
    return new JsonObject()
      .put("id", accountID)
      .put("userId", randomId())
      .put("feeFineId", randomId())
      .put("materialTypeId", randomId())
      .put("ownerId", randomId())
      .put("itemId", ITEM_ID)
      .encodePrettily();
  }

  private Response get(String path) {
    return getRequestSpecification()
      .when()
      .get(REST_PATH);
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

  private String randomId() {
    return UUID.randomUUID().toString();
  }

  private void processEvent(TestContext context, AsyncResult<UpdateResult> event, Async async) {
    if (event.failed()) {
      logger.error(event.cause());
      context.fail(event.cause());
    } else {
      async.countDown();
    }
  }
}
