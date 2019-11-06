package org.folio.rest.impl;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;

import javax.ws.rs.core.MediaType;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.*;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import java.util.Collections;
import java.util.UUID;

@RunWith(VertxUnitRunner.class)
public class OverdueFinePoliciesAPITest {

  private static final Logger logger = LoggerFactory.getLogger(OverdueFinePoliciesAPITest.class);

  private static final String OKAPI_URL = "x-okapi-url";
  private static final String HTTP_PORT = "http.port";
  private static final String REST_PATH = "/overdue-fines-policies";
  private static final String OKAPI_TOKEN = "test_token";
  private static final String OKAPI_URL_TEMPLATE = "http://localhost:%s";

  private static Vertx vertx;
  private static int port;
  private static String overdueFinePolicyEntity;
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
    overdueFinePolicyEntity = createEntity();

    PostgresClient.setIsEmbedded(true);
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
      .delete(OverdueFinePoliciesAPI.OVERDUE_FINE_POLICY_TABLE, new Criterion(), event -> {
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
  public void postOverdueFinesPoliciesSuccess() {
    post(overdueFinePolicyEntity)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON)
      .body(equalTo(overdueFinePolicyEntity));
  }

  @Test
  public void postOverdueFinesPoliciesDuplicate() {
    post(overdueFinePolicyEntity);

    JsonObject errorJson = new JsonObject()
      .put("message", "An overdue fine policy with this name already exists. Please choose a different name.")
      .put("code", OverdueFinePoliciesAPI.DUPLICATE_ERROR_CODE)
      .put("parameters", new JsonArray());

    String errors = new JsonObject()
      .put("errors", new JsonArray(Collections.singletonList(errorJson)))
      .encodePrettily();

    post(overdueFinePolicyEntity)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(ContentType.JSON)
      .body(equalTo(errors));
  }

  @Test
  public void postOverdueFinesPoliciesMissingName() {
    JsonObject parameters = new JsonObject()
      .put("key", "name")
      .put("value", "null");

    JsonObject error = new JsonObject()
      .put("message", "may not be null")
      .put("type", "1")
      .put("code", "-1")
      .put("parameters", new JsonArray(Collections.singletonList(parameters)));

    String errors = new JsonObject()
      .put("errors", new JsonArray(Collections.singletonList(error)))
      .encode();

    JsonObject payload = createEntityJson();
    payload.remove("name");

    post(payload.encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(ContentType.JSON)
      .body(equalTo(errors));
  }

  @Test
  public void postOverdueFinesPoliciesMalformedJson() {
    post(overdueFinePolicyEntity.substring(1))
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .contentType(ContentType.TEXT)
      .body(startsWith(
        "Json content error Cannot construct instance of `org.folio.rest.jaxrs.model.OverdueFinePolicy`"));
  }

  @Test
  public void postOverdueFinesPoliciesInvalidUuid() {
    String payloadWithInvalidUuid = createEntityJson()
      .put("id", UUID.randomUUID().toString() + "a")
      .encodePrettily();

    post(payloadWithInvalidUuid)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST)
      .contentType(ContentType.TEXT)
      .body(equalTo("Invalid UUID format of id, should be xxxxxxxx-xxxx-Mxxx-Nxxx-xxxxxxxxxxxx" +
        " where M is 1-5 and N is 8, 9, a, b, A or B and x is 0-9, a-f or A-F."));
  }

  @Test
  public void postOverdueFinesPoliciesServerError() {
    String originalTenant = okapiTenant;
    okapiTenant = "test_breaker";

    post(overdueFinePolicyEntity)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
      .body(containsString("password authentication failed for user \"test_breaker_mod_feesfines\""));

    okapiTenant = originalTenant;
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

  private static String createEntity() {
    return createEntityJson().encodePrettily();
  }

  private static JsonObject createEntityJson() {
    return new JsonObject()
      .put("name", "Faculty standard")
      .put("description", "This is description for Faculty standard")
      .put("overdueFine", new JsonObject().put("quantity", 5.0).put("intervalId", "day"))
      .put("countClosed", true)
      .put("maxOverdueFine", 50.00)
      .put("forgiveOverdueFine", true)
      .put("overdueRecallFine", new JsonObject().put("quantity", 1.0).put("intervalId", "hour"))
      .put("gracePeriodRecall", false)
      .put("maxOverdueRecallFine", 50.00)
      .put("id", "b712dffc-c107-4e88-b3d5-fba2fb35755e");
  }

}
