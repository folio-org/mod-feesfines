package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.vertx.core.json.JsonObject.mapFrom;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.FEE_FINE;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.NOTICE;
import static org.folio.rest.utils.LogEventUtils.fetchPublishedLogRecords;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import io.vertx.core.json.JsonArray;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.jaxrs.model.*;
import org.folio.rest.service.LogEventPublisher;
import org.folio.test.support.ApiTests;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

public class FeeFineActionsAPITest extends ApiTests {
  private static final String ACTIONS_PATH = "/feefineactions";
  private static final String OWNERS_PATH = "/owners";
  private static final String ACCOUNTS_PATH = "/accounts";
  private static final String FEEFINES_PATH = "/feefines";
  private static final String ITEMS_PATH = "/item-storage/items";
  private static final String HOLDINGS_PATH = "/holdings-storage/holdings";
  private static final String INSTANCES_PATH = "/instance-storage/instances";
  private static final String LOCATIONS_PATH = "/locations";
  private static final String INSTITUTIONS_PATH = "/location-units/institutions";
  private static final String CAMPUSES_PATH = "/location-units/campuses";
  private static final String LIBRARIES_PATH = "/location-units/libraries";
  private static final String USERS_PATH = "/users";

  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String FEEFINES_TABLE = "feefines";
  private static final String OWNERS_TABLE = "owners";
  private static final String FEE_FINE_ACTIONS_TABLE = "feefineactions";

  private static final String KEY_NAME = "name";
  private static final String KEY_ID = "id";

  private static final String PRIMARY_CONTRIBUTOR_NAME = "Primary contributor";
  private static final String NON_PRIMARY_CONTRIBUTOR_NAME = "Non-primary contributor";

  private static final String CHARGE_COMMENT_FOR_PATRON = "Charge comment";
  private static final String ACTION_COMMENT_FOR_PATRON = "Action comment";

  private static final String ACCOUNT_AMOUNT = "12.34";
  private static final String ACCOUNT_REMAINING = "5.67";
  private static final String ACTION_AMOUNT = "6.67";

  @Before
  public void setUp() {
    removeAllFromTable(FEEFINES_TABLE);
    removeAllFromTable(ACCOUNTS_TABLE);
    removeAllFromTable(FEE_FINE_ACTIONS_TABLE);
    removeAllFromTable(OWNERS_TABLE);
  }

  @Test
  public void postActionWithPatronNotice() {
    getOkapi().stubFor(WireMock.post(urlPathMatching("/patron-notice"))
      .willReturn(aResponse().withStatus(200)));

    final Library library = createLibrary();
    final Campus campus = createCampus();
    final Institution institution = createInstitution();
    final Location location = createLocation(library, campus, institution);
    final Instance instance = createInstance();
    final HoldingsRecord holdingsRecord = createHoldingsRecord(instance);
    final Item item = createItem(holdingsRecord, location);
    final User user = createUser();
    final Owner owner = createOwner();
    final Feefine feefine = createFeeFine(owner);
    final Account account = createAccount(user, item, feefine, owner, instance, holdingsRecord);
    final Feefineaction charge = createCharge(user, account, true);
    final Feefineaction action = createAction(user, account, true);

    createEntity(OWNERS_PATH, owner);
    createEntity(FEEFINES_PATH, feefine);
    createEntity(ACCOUNTS_PATH, account);

    createStub(USERS_PATH, user, user.getId());
    createStub(ITEMS_PATH, item, item.getId());
    createStub(LOCATIONS_PATH, location, location.getId());
    createStub(HOLDINGS_PATH, holdingsRecord, holdingsRecord.getId());
    createStub(INSTANCES_PATH, instance, instance.getId());
    createStub(LIBRARIES_PATH, library, getIdFromProperties(library.getAdditionalProperties()));
    createStub(CAMPUSES_PATH, campus, getIdFromProperties(campus.getAdditionalProperties()));
    createStub(INSTITUTIONS_PATH, institution, getIdFromProperties(institution.getAdditionalProperties()));

    postAction(charge);

    final String accountCreationDate = getAccountCreationDate(account);

    final JsonObject expectedChargeContext = new JsonObject()
      .put("recipientId", action.getUserId())
      .put("deliveryChannel", "email")
      .put("outputFormat", "text/html")
      .put("lang", "en")
      .put("templateId", feefine.getChargeNoticeId())
      .put("context", new JsonObject()
        .put("user", new JsonObject()
          .put("barcode", user.getBarcode())
          .put("firstName", user.getPersonal().getFirstName())
          .put("lastName", user.getPersonal().getLastName())
          .put("middleName", user.getPersonal().getMiddleName()))
        .put("item", new JsonObject()
          .put("barcode", item.getBarcode())
          .put("enumeration", item.getEnumeration())
          .put("volume", item.getVolume())
          .put("chronology", item.getChronology())
          .put("yearCaption", "2000")
          .put("copy", item.getCopyNumber())
          .put("numberOfPieces", item.getNumberOfPieces())
          .put("descriptionOfPieces", item.getDescriptionOfPieces())
          .put("callNumber", item.getEffectiveCallNumberComponents().getCallNumber())
          .put("callNumberPrefix", item.getEffectiveCallNumberComponents().getPrefix())
          .put("callNumberSuffix", item.getEffectiveCallNumberComponents().getSuffix())
          .put("title", instance.getTitle())
          .put("primaryContributor", PRIMARY_CONTRIBUTOR_NAME)
          .put("allContributors", PRIMARY_CONTRIBUTOR_NAME + "; " + NON_PRIMARY_CONTRIBUTOR_NAME)
          .put("effectiveLocationSpecific", location.getName())
          .put("effectiveLocationLibrary", getNameFromProperties(library.getAdditionalProperties()))
          .put("effectiveLocationInstitution", getNameFromProperties(institution.getAdditionalProperties()))
          .put("effectiveLocationCampus", getNameFromProperties(campus.getAdditionalProperties()))
          .put("materialType", account.getMaterialType()))
        .put("feeCharge", new JsonObject()
          .put("owner", account.getFeeFineOwner())
          .put("type", account.getFeeFineType())
          .put("paymentStatus", account.getPaymentStatus().getName())
          .put("amount", ACCOUNT_AMOUNT)
          .put("remainingAmount", ACCOUNT_REMAINING)
          .put("chargeDate", accountCreationDate)
          .put("chargeDateTime", accountCreationDate)
          .put("additionalInfo", CHARGE_COMMENT_FOR_PATRON))
        .put("feeAction", new JsonObject()
        )
      );

    JsonObject expectedNoticeLogContext = new JsonObject()
      .put("userId", user.getId())
      .put("userBarcode", user.getBarcode())
      .put("feeFineId", feefine.getId())
      .put("items", new JsonArray()
        .add(new JsonObject()
          .put("triggeringEvent", charge.getTypeAction())
          .put("itemId", item.getId())
          .put("itemBarcode", account.getBarcode())
          .put("templateId", feefine.getChargeNoticeId())));

    JsonObject expectedFeeFineLogContext = new JsonObject()
      .put("userId", user.getId())
      .put("userBarcode", user.getBarcode())
      .put("itemBarcode", account.getBarcode())
      .put("action", "Billed")
      .put("feeFineId", account.getFeeFineId())
      .put("feeFineOwner", account.getFeeFineOwner())
      .put("type", account.getFeeFineType())
      .put("amount", charge.getAmountAction());

    checkResult(expectedChargeContext);
    assertThatPublishedLogRecordsCountIsEqualTo(2);
    assertThatLogPayloadIsValid(expectedNoticeLogContext, extractLastLogRecordPayloadOfType(NOTICE));
    assertThatLogPayloadIsValid(expectedFeeFineLogContext, extractLastLogRecordPayloadOfType(FEE_FINE));

    final JsonObject expectedActionContext = expectedChargeContext
      .put("templateId", feefine.getActionNoticeId());

    expectedActionContext
      .getJsonObject("context")
      .getJsonObject("feeAction")
      .put("type", action.getTypeAction())
      .put("actionDate", dateToString(action.getDateAction()))
      .put("actionDateTime", dateToString(action.getDateAction()))
      .put("amount", ACTION_AMOUNT)
      .put("remainingAmount", ACCOUNT_REMAINING)
      .put("additionalInfo", ACTION_COMMENT_FOR_PATRON);

    expectedNoticeLogContext = new JsonObject()
      .put("userId", user.getId())
      .put("userBarcode", user.getBarcode())
      .put("feeFineId", feefine.getId())
      .put("items", new JsonArray()
        .add(new JsonObject()
          .put("triggeringEvent", action.getTypeAction())
          .put("itemId", item.getId())
          .put("itemBarcode", account.getBarcode())
          .put("templateId", feefine.getActionNoticeId())));

    expectedFeeFineLogContext = new JsonObject()
      .put("userId", user.getId())
      .put("userBarcode", user.getBarcode())
      .put("itemBarcode", account.getBarcode())
      .put("action", action.getTypeAction())
      .put("feeFineId", account.getFeeFineId())
      .put("feeFineOwner", account.getFeeFineOwner())
      .put("type", account.getFeeFineType())
      .put("amount", action.getAmountAction());

    postAction(action);
    checkResult(expectedActionContext);
    assertThatPublishedLogRecordsCountIsEqualTo(4);
    assertThatLogPayloadIsValid(expectedNoticeLogContext, extractLastLogRecordPayloadOfType(NOTICE));
    assertThatLogPayloadIsValid(expectedFeeFineLogContext, extractLastLogRecordPayloadOfType(FEE_FINE));
  }

  @Test
  public void postFeefineActionsWithoutPatronNotification() {
    final String ownerId = randomId();
    final String feeFineId = randomId();
    final String accountId = randomId();
    final String defaultChargeTemplateId = randomId();
    final User user = createUser();
    final String feeFineType = "damaged book";
    final String typeAction = "damaged book";
    final boolean notify = false;
    final double amountAction = 100;
    final double balance = 100;
    final String dateAction = "2019-12-23T14:25:59.550+0000";
    final String feeFineActionJson = createFeeFineActionJson(dateAction, typeAction, notify,
      amountAction, balance, accountId, user.getId());

    createStub(USERS_PATH, user, user.getId());

    createEntity("/owners", new JsonObject()
      .put("id", ownerId)
      .put("owner", "library")
      .put("defaultChargeNoticeId", defaultChargeTemplateId));

    createEntity("/accounts", new JsonObject()
      .put("id", accountId)
      .put("userId", user.getId())
      .put("itemId", randomId())
      .put("materialTypeId", randomId())
      .put("feeFineId", feeFineId)
      .put("ownerId", ownerId)
      .put("remaining", 10.0)
      .put("amount", 10.0));

    createEntity("/feefines", new JsonObject()
      .put("id", feeFineId)
      .put("feeFineType", feeFineType)
      .put("ownerId", ownerId));

    post(feeFineActionJson)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .body(equalTo(feeFineActionJson));

    JsonObject expectedFeeFineLogContext = new JsonObject()
      .put("userId", user.getId())
      .put("userBarcode", user.getBarcode())
      .put("action", "Billed")
      .put("feeFineId", feeFineId)
      .put("type", feeFineType)
      .put("amount", amountAction);

    assertThatPublishedLogRecordsCountIsEqualTo(1);
    assertThatLogPayloadIsValid(expectedFeeFineLogContext, extractLastLogRecordPayloadOfType(FEE_FINE));
  }

  @Test
  public void postFeeFineActionsChangeStaffInfo() {
    final String ownerId = randomId();
    final String feeFineId = randomId();
    final String accountId = randomId();
    final String defaultChargeTemplateId = randomId();
    final User user = createUser();
    final String feeFineType = "damaged book";
    final String typeAction = "Staff info only";
    final String expectedTypeAction = "Staff information only added";
    final boolean notify = false;
    final double amountAction = 100;
    final double balance = 100;
    final String dateAction = "2019-12-23T14:25:59.550+0000";
    final String feeFineActionJson = createFeeFineActionJson(dateAction, typeAction, notify,
      amountAction, balance, accountId, user.getId());

    createStub(USERS_PATH, user, user.getId());

    createEntity("/owners", new JsonObject()
      .put("id", ownerId)
      .put("owner", "library")
      .put("defaultChargeNoticeId", defaultChargeTemplateId));

    createEntity("/accounts", new JsonObject()
      .put("id", accountId)
      .put("userId", user.getId())
      .put("itemId", randomId())
      .put("materialTypeId", randomId())
      .put("feeFineId", feeFineId)
      .put("ownerId", ownerId)
      .put("remaining", 10.0)
      .put("amount", 10.0));

    createEntity("/feefines", new JsonObject()
      .put("id", feeFineId)
      .put("feeFineType", feeFineType)
      .put("ownerId", ownerId));

    post(feeFineActionJson)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .body(equalTo(feeFineActionJson));

    JsonObject expectedFeeFineLogContext = new JsonObject()
      .put("userId", user.getId())
      .put("userBarcode", user.getBarcode())
      .put("action", expectedTypeAction)
      .put("feeFineId", feeFineId)
      .put("type", feeFineType)
      .put("amount", amountAction);

    assertThatPublishedLogRecordsCountIsEqualTo(1);
    assertThatLogPayloadIsValid(expectedFeeFineLogContext, extractLastLogRecordPayloadOfType(FEE_FINE));
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

  private <T> void createEntity(String path, T entity) {
    RestAssured.given()
      .spec(getRequestSpecification())
      .body(mapFrom(entity).encodePrettily())
      .when()
      .post(path)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  private Response post(String body) {
    return getRequestSpecification()
      .body(body)
      .when()
      .post(ACTIONS_PATH);
  }

  private Response get(String path, String id) {
    return getRequestSpecification()
      .when()
      .get(path + "/" + id);
  }

  private Response postAction(Feefineaction action) {
    return post(mapFrom(action).encodePrettily());
  }

  private RequestSpecification getRequestSpecification() {
    return RestAssured.given()
      .baseUri(getOkapiUrl())
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TENANT_NAME))
      .header(new Header(OKAPI_URL_HEADER, getOkapiUrl()))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN));
  }

  private <T> void createStub(String url, T returnObject) {
    createStub(url, aResponse().withBody(mapFrom(returnObject).encodePrettily()));
  }

  private <T> void createStub(String url, T returnObject, String id) {
    createStub(url + "/" + id, returnObject);
  }

  private void createStub(String url, ResponseDefinitionBuilder responseBuilder) {
    getOkapi().stubFor(WireMock.get(urlPathEqualTo(url))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(getOkapiUrl()))
      .willReturn(responseBuilder));
  }

  private static Feefineaction createAction(User user, Account account, boolean notify) {
    return new Feefineaction()
      .withId(randomId())
      .withUserId(user.getId())
      .withAccountId(account.getId())
      .withNotify(notify)
      .withTypeAction("Paid partially")
      .withDateAction(new Date())
      .withAmountAction(Double.valueOf(ACTION_AMOUNT))
      .withBalance(Double.valueOf(ACCOUNT_REMAINING))
      .withPaymentMethod("Cash")
      .withComments("STAFF : staff comment \n PATRON : " + ACTION_COMMENT_FOR_PATRON);
  }

  private static Feefineaction createCharge(User user, Account account, boolean notify) {
    return new Feefineaction()
      .withUserId(user.getId())
      .withAccountId(account.getId())
      .withNotify(notify)
      .withTypeAction("Overdue fine")
      .withDateAction(new Date())
      .withAmountAction(Double.valueOf(ACTION_AMOUNT))
      .withBalance(Double.valueOf(ACCOUNT_REMAINING))
      .withComments("STAFF : staff comment \n PATRON : " + CHARGE_COMMENT_FOR_PATRON);
  }

  private static Feefine createFeeFine(Owner owner) {
    return new Feefine()
      .withId(randomId())
      .withOwnerId(owner.getId())
      .withFeeFineType("Overdue fine")
      .withAutomatic(true)
      .withActionNoticeId(UUID.randomUUID().toString())
      .withChargeNoticeId(UUID.randomUUID().toString());
  }

  private static Instance createInstance() {
    return new Instance()
      .withId(randomId())
      .withTitle("Instance title")
      .withContributors(Arrays.asList(
        new Contributor().withName(PRIMARY_CONTRIBUTOR_NAME).withPrimary(true),
        new Contributor().withName(NON_PRIMARY_CONTRIBUTOR_NAME).withPrimary(false)));
  }

  private static Account createAccount(User user, Item item, Feefine feefine, Owner owner,
    Instance instance, HoldingsRecord holdingsRecord) {

    return new Account()
      .withId(randomId())
      .withUserId(user.getId())
      .withItemId(item.getId())
      .withFeeFineId(feefine.getId())
      .withOwnerId(owner.getId())
      .withFeeFineOwner(owner.getOwner())
      .withInstanceId(instance.getId())
      .withHoldingsRecordId(holdingsRecord.getId())
      .withBarcode("Account-level barcode")
      .withTitle("Account-level title")
      .withCallNumber("Account-level call number")
      .withLocation("Account-level location")
      .withPaymentStatus(new PaymentStatus().withName("Paid fully"))
      .withFeeFineType(feefine.getFeeFineType())
      .withMaterialType("book")
      .withMaterialTypeId(randomId())
      .withAmount(Double.valueOf(ACCOUNT_AMOUNT))
      .withRemaining(Double.valueOf(ACCOUNT_REMAINING));
  }

  private static HoldingsRecord createHoldingsRecord(Instance instance) {
    return new HoldingsRecord()
      .withId(randomId())
      .withInstanceId(instance.getId())
      .withCopyNumber("cp.2");
  }

  private static Location createLocation(Library library, Campus campus, Institution institution) {
    return new Location()
      .withId(randomId())
      .withName("Specific")
      .withCampusId(String.valueOf(campus.getAdditionalProperties().get(KEY_ID)))
      .withLibraryId(String.valueOf(library.getAdditionalProperties().get(KEY_ID)))
      .withInstitutionId(String.valueOf(institution.getAdditionalProperties().get(KEY_ID)));
  }

  private static Library createLibrary() {
    return new Library()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Library");
  }

  private static Campus createCampus() {
    return new Campus()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Campus");
  }

  private static Institution createInstitution() {
    return new Institution()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Institution");
  }

  private static Item createItem(HoldingsRecord holdingsRecord,
    Location location) {
    return new Item()
      .withId(randomId())
      .withHoldingsRecordId(holdingsRecord.getId())
      .withBarcode("12345")
      .withEnumeration("enum")
      .withVolume("vol.1")
      .withChronology("chronology")
      .withYearCaption(new HashSet<>(Collections.singletonList("2000")))
      .withCopyNumber("cp.1")
      .withNumberOfPieces("1")
      .withDescriptionOfPieces("little pieces")
      .withEffectiveLocationId(location.getId())
      .withEffectiveCallNumberComponents(
        new EffectiveCallNumberComponents()
          .withCallNumber("ABC.123.DEF")
          .withPrefix("PREFIX")
          .withSuffix("SUFFIX"));
  }

  private static User createUser() {
    return new User()
      .withId(randomId())
      .withBarcode("54321")
      .withPersonal(new Personal()
        .withFirstName("First")
        .withMiddleName("Middle")
        .withLastName("Last"));
  }

  private static Owner createOwner() {
    return new Owner()
      .withId(randomId())
      .withOwner("Test owner")
      .withDefaultActionNoticeId(UUID.randomUUID().toString())
      .withDefaultChargeNoticeId(UUID.randomUUID().toString());
  }

  private static String getNameFromProperties(Map<String, Object> properties) {
    return (String) properties.get("name");
  }

  private static String getIdFromProperties(Map<String, Object> properties) {
    return (String) properties.get("id");
  }

  private static String dateToString(Date date) {
    return new DateTime(date, DateTimeZone.UTC).toString();
  }

  private String getAccountCreationDate(Account account) {
    String getAccountResponse = get(ACCOUNTS_PATH, account.getId())
      .getBody().prettyPrint();

    final String creationDateFromMetadata = new JsonObject(getAccountResponse)
      .getJsonObject("metadata")
      .getString("createdDate");

    return new DateTime(creationDateFromMetadata, DateTimeZone.UTC).toString();
  }

  private void checkResult(JsonObject expectedRequest) {
    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> getOkapi().verify(postRequestedFor(urlPathEqualTo("/patron-notice"))
        .withRequestBody(equalToJson(expectedRequest.encodePrettily()))
      ));
  }

  private void assertThatPublishedLogRecordsCountIsEqualTo(int count) {
    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .until(() -> fetchPublishedLogRecords(getOkapi()).size() == count);
  }

  private JsonObject extractLastLogRecordPayloadOfType(LogEventPublisher.LogEventPayloadType type) {
    return fetchPublishedLogRecords(getOkapi()).stream()
      .map(json -> json.getString("eventPayload"))
      .filter(s -> s.contains(type.value()))
      .map(JsonObject::new)
      .map(json -> json.getString("payload"))
      .map(JsonObject::new)
      .max(Comparator.comparing(json -> json.getString("date")))
      .orElse(new JsonObject());
  }

  private void assertThatLogPayloadIsValid(JsonObject expected, JsonObject actual) {
    String jsonAsString = actual.encode();
    expected.forEach(entry -> {
      if (entry.getValue() instanceof JsonArray) {
        JsonObject exp = ((JsonArray) entry.getValue()).getJsonObject(0);
        JsonObject act = ((actual.getJsonArray(entry.getKey())) != null)?
          actual.getJsonArray(entry.getKey()).getJsonObject(0): new JsonObject();
        assertThatLogPayloadIsValid(exp, act);
      } else {
        assertThat(jsonAsString, hasJsonPath(entry.getKey(), equalTo(entry.getValue())));
      }
    });
  }
}

