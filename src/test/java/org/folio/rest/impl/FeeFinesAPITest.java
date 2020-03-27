package org.folio.rest.impl;

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

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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
public class FeeFinesAPITest {
  private static final Logger logger = LoggerFactory.getLogger(FeeFinesAPITest.class);

  private static final String OKAPI_URL = "x-okapi-url";
  private static final String HTTP_PORT = "http.port";
  private static final String REST_PATH = "/feefines";
  private static final String OKAPI_TOKEN = "test_token";
  private static final String OKAPI_URL_TEMPLATE = "http://localhost:%s";
  private static final String FEEFINES_TABLE = "feefines";
  private static final String OWNERS_TABLE = "owners";

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
    Async async = context.async(2);
    PostgresClient client = PostgresClient.getInstance(vertx, okapiTenant);

    client.delete(FEEFINES_TABLE, new Criterion(), result -> processEvent(context, result, async));
    client.delete(OWNERS_TABLE, new Criterion(), result -> processEvent(context, result, async));
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
  public void canCreateNewFeefine() {
    String ownerId = randomId();
    createOwner(ownerId, "test_owner");
    String entity = createFeefineJson(randomId(), "book lost", ownerId);

    post(entity)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  @Test
  public void cannotCreateFeefineWithDuplicateId() {
    String ownerId1 = randomId();
    String ownerId2 = randomId();

    createOwner(ownerId1, "test_owner_1");
    createOwner(ownerId2, "test_owner_2");

    String feefineId = randomId();

    String entity1 = createFeefineJson(feefineId, "type_1", ownerId1);
    String entity2 = createFeefineJson(feefineId, "type_2", ownerId2);

    post(entity1)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    post(entity2)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void cannotCreateDuplicateFeefineTypeForSameOwner() {
    String ownerId = randomId();
    createOwner(ownerId, "test_owner");

    String entity1 = createFeefineJson(randomId(), "book lost", ownerId);
    String entity2 = createFeefineJson(randomId(), "book lost", ownerId);

    post(entity1)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    post(entity2)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void canCreateDuplicateFeefineTypeForDifferentOwners() {
    String ownerId1 = randomId();
    String ownerId2 = randomId();

    createOwner(ownerId1, "test_owner_1");
    createOwner(ownerId2, "test_owner_2");

    String entity1 = createFeefineJson(randomId(), "book lost", ownerId1);
    String entity2 = createFeefineJson(randomId(), "book lost", ownerId2);

    post(entity1)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    post(entity2)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  private String createFeefineJson(String id, String type, String ownerId) {
    return new JsonObject()
      .put("id", id)
      .put("automatic", false)
      .put("feeFineType", type)
      .put("defaultAmount", "1.00")
      .put("chargeNoticeId", randomId())
      .put("actionNoticeId", randomId())
      .put("ownerId", ownerId)
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

  private void createEntity(String path, JsonObject entity) {
    RestAssured.given()
      .spec(getRequestSpecification())
      .body(entity.encode())
      .when()
      .post(path)
      .then()
      .statusCode(201);
  }

  private void createOwner(String id, String name) {
    createEntity("/owners", new JsonObject()
      .put("id", id)
      .put("owner", name)
      .put("defaultChargeNoticeId", randomId())
      .put("defaultActionNoticeId", randomId()));
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

