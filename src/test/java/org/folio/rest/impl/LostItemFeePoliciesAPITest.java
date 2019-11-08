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
public class LostItemFeePoliciesAPITest {
    private static final Logger logger = LoggerFactory.getLogger(LostItemFeePoliciesAPITest.class);

    private static final String OKAPI_URL = "x-okapi-url";
    private static final String HTTP_PORT = "http.port";
    private static final String REST_PATH = "/lost-item-fees-policies";
    private static final String OKAPI_TOKEN = "test_token";
    private static final String OKAPI_URL_TEMPLATE = "http://localhost:%s";

    private static Vertx vertx;
    private static int port;
    private static String lostItemFeePolicyEntity;
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
        lostItemFeePolicyEntity = createEntity();

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
                .delete(LostItemFeePoliciesAPI.LOST_ITEM_FEE_TABLE, new Criterion(), event -> {
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
    public void postLostItemFeesPoliciesSuccess() {
        post(lostItemFeePolicyEntity)
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .contentType(ContentType.JSON)
                .body(equalTo(lostItemFeePolicyEntity));
    }

    @Test
    public void postLostItemFeesPoliciesDuplicate() {
        post(lostItemFeePolicyEntity);

        JsonObject errorJson = new JsonObject()
                .put("message", "The Lost item fee policy name entered already exists. Please enter a different name.")
                .put("code", LostItemFeePoliciesAPI.DUPLICATE_ERROR_CODE)
                .put("parameters", new JsonArray());

        String errors = new JsonObject()
                .put("errors", new JsonArray(Collections.singletonList(errorJson)))
                .encodePrettily();

        post(lostItemFeePolicyEntity)
                .then()
                .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
                .contentType(ContentType.JSON)
                .body(equalTo(errors));
    }

    @Test
    public void postLostItemFeesPoliciesMissingName() {
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
    public void postLostItemFeesPoliciesMalformedJson() {
        post(lostItemFeePolicyEntity.substring(1))
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .contentType(ContentType.TEXT)
                .body(startsWith(
                        "Json content error Cannot construct instance of `org.folio.rest.jaxrs.model.LostItemFeePolicy`"));
    }

    @Test
    public void postLostItemFeesPoliciesInvalidUuid() {
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
    public void postLostItemFeesPoliciesServerError() {
        String originalTenant = okapiTenant;
        okapiTenant = "test_breaker";

        post(lostItemFeePolicyEntity)
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
                .put("name", "Undergrad standard")
                .put("description", "This is description for undergrad standard")
                .put("itemAgedLostOverdue", new JsonObject().put("duration", 12).put("intervalId", "Months"))
                .put("patronBilledAfterAgedLost", new JsonObject().put("duration", 12).put("intervalId", "Months"))
                .put("chargeAmountItem", new JsonObject().put("chargeType", "Actual cost").put("amount", 5.00))
                .put("lostItemProcessingFee", 5.00)
                .put("chargeAmountItemPatron", true)
                .put("chargeAmountItemSystem", true)
                .put("lostItemChargeFeeFine", new JsonObject().put("duration", 6).put("intervalId", "Months"))
                .put("returnedLostItemProcessingFee", true)
                .put("replacedLostItemProcessingFee", true)
                .put("replacementProcessingFee", 0.00)
                .put("replacementAllowed", true)
                .put("lostItemReturned", "Charge")
                .put("id", "0c340536-8ed7-409e-8940-e65f2330d4d7");
    }

}
