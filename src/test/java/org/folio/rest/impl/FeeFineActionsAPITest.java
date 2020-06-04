package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.folio.test.support.ApiTests;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

public class FeeFineActionsAPITest extends ApiTests {
  private static final String REST_PATH = "/feefineactions";
  private static final String FEEFINES_TABLE = "feefines";

  @Before
  public void setUp() {
    removeAllFromTable(FEEFINES_TABLE);
    removeAllFromTable(FeeFineActionsAPI.FEEFINEACTIONS_TABLE);
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

    User user = new User()
      .withId(userId)
      .withUsername("tester")
      .withActive(true)
      .withBarcode("123456")
      .withPatronGroup(randomId())
      .withType("patron")
      .withPersonal(new Personal()
        .withFirstName("First")
        .withMiddleName("Middle")
        .withLastName("Last")
        .withEmail("test@test.com"));

    setupUsersStub(user);

    final String feeFineActionJson = createFeeFineActionJson(dateAction, typeAction, notify,
      amountAction, balance, accountId, userId);
    final String expectedNoticeJson = createNoticeJson(defaultChargeTemplateId,
      userId, feeFineType, typeAction, amountAction, balance, amount, dateAction, user);

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
      .untilAsserted(() -> getOkapi().verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
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

  private String createNoticeJson(String defaultChargeTemplateId, String userId, String feeFineType,
    String typeAction, double amountAction, double balance, double amount, String dateAction,
    User user) {

    JsonObject noticeContext = createNoticeContext(feeFineType, typeAction,
      amountAction, balance, amount, dateAction, user);

    return new JsonObject()
      .put("recipientId", userId)
      .put("deliveryChannel", "email")
      .put("templateId", defaultChargeTemplateId)
      .put("outputFormat", "text/html")
      .put("lang", "en")
      .put("context", noticeContext)
      .encodePrettily();
  }

  private JsonObject createNoticeContext(String feeFineType, String typeAction, double amountAction,
    double balance, double amount, String dateAction, User user) {

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
        .put("reasonForCancellation", "staff comment"))
      .put("user", new JsonObject()
        .put("firstName", user.getPersonal().getFirstName())
        .put("lastName", user.getPersonal().getLastName())
        .put("middleName", user.getPersonal().getMiddleName())
        .put("barcode", user.getBarcode()));
  }

  private String createFeeFineActionJson(String dateAction, String typeAction, boolean notify,
    double amountAction, double balance, String accountId, String userId) {

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
      .baseUri(getOkapiUrl())
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TENANT_NAME))
      .header(new Header(OKAPI_URL_HEADER, getOkapiUrl()))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN));
  }

  private void setupPatronNoticeStub() {
    getOkapi().stubFor(WireMock.post(urlPathEqualTo("/patron-notice"))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(getOkapiUrl()))
      .willReturn(ok()));
  }

  private void setupUsersStub(User user) {
    String response = JsonObject.mapFrom(user)
      .encodePrettily();

    getOkapi().stubFor(WireMock.get(urlPathEqualTo("/users/" + user.getId()))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(getOkapiUrl()))
      .willReturn(aResponse().withBody(response)));
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
}

