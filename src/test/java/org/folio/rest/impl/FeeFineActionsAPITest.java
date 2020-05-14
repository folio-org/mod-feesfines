package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.core.IsEqual.equalTo;

import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

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
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criterion;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@RunWith(VertxUnitRunner.class)
@PrepareForTest(PubSubClientUtils.class)
public class FeeFineActionsAPITest extends APITests {
  private static final String REST_PATH = "/feefineactions";
  private static final String FEEFINES_TABLE = "feefines";

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Rule
  public WireMockRule wireMock = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @Before
  public void setUp(TestContext context) {
    Async async = context.async();

    mockStatic(PubSubClientUtils.class);
    when(PubSubClientUtils.sendEventMessage(any(Event.class), any(OkapiConnectionParams.class)))
      .thenReturn(completedFuture(true));

    PostgresClient client = PostgresClient.getInstance(vertx, OKAPI_TENANT);
    client.delete(FEEFINES_TABLE, new Criterion(), event -> processEvent(context, event));
    client.delete(FeeFineActionsAPI.FEEFINEACTIONS_TABLE, new Criterion(), event ->
      processEvent(context, event));
    async.complete();
  }

  @Test
  public void postFeefineActionsWithPatronNotification() {
    setupPatronNoticeStub();

    final String ownerId = randomId();
    final String feeFineId = randomId();
    final String accountId = randomId();
    final String defaultChargeTemplateId = randomId();
    final String userId = randomId();
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
      .put("itemId", randomId())
      .put("materialTypeId", randomId())
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
      .untilAsserted(() -> wireMock.verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(expectedNoticeJson))
      ));
  }

  @Test
  public void postFeefineActionsWithoutPatronNotification() {
    final String ownerId = randomId();
    final String feeFineId = randomId();
    final String accountId = randomId();
    final String defaultChargeTemplateId = randomId();
    final String userId = randomId();
    final String feeFineType = "damaged book";
    final String typeAction = "damaged book";
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
      .put("itemId", randomId())
      .put("materialTypeId", randomId())
      .put("feeFineId", feeFineId)
      .put("ownerId", ownerId)
      .put("amount", 10.0));

    createEntity("/feefines", new JsonObject()
      .put("id", feeFineId)
      .put("feeFineType", feeFineType)
      .put("ownerId", ownerId));

    post(feeFineActionJson)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
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
      .put("id", randomId())
      .encodePrettily();

  }

  private Response post(String body) {
    return getRequestSpecification()
      .body(body)
      .when()
      .post(REST_PATH);
  }

  private RequestSpecification getRequestSpecification() {
    return RestAssured.given()
      .port(OKAPI_PORT)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, OKAPI_TENANT))
      .header(new Header(OKAPI_HEADER_URL, wireMock.baseUrl()))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN));
  }

  private void setupPatronNoticeStub() {
    wireMock.stubFor(WireMock.post(urlPathEqualTo("/patron-notice"))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(OKAPI_TENANT))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_HEADER_URL, matching(wireMock.baseUrl()))
      .willReturn(ok()));
  }

  private void createEntity(String path, JsonObject entity) {
    RestAssured.given()
      .spec(getRequestSpecification())
      .body(entity.encode())
      .when()
      .post(path)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  private void processEvent(TestContext context, AsyncResult<UpdateResult> event) {
    if (event.failed()) {
      log.error(event.cause());
      context.fail(event.cause());
    }
  }
}

