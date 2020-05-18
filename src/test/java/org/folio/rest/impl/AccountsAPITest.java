package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.restassured.http.ContentType.JSON;
import static io.vertx.core.json.Json.decodeValue;
import static org.folio.test.support.matcher.AccountMatchers.isAccountPaidFully;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.EventType;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.test.support.matcher.MappableMatcher;
import org.folio.util.pubsub.PubSubClientUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class AccountsAPITest extends APITests {
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String ITEM_ID = "43ec57e3-3974-4d05-a2c2-95126e087b72";
  public static final String FEEFINE_CLOSED_EVENT_NAME = "FEEFINE_WITH_LOAN_CLOSED";

  @Before
  public void setUp() {
    wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/inventory/items.*"))
      .willReturn(aResponse().withBodyFile("items.json")));

    wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/holdings-storage/holdings.*"))
      .willReturn(aResponse().withBodyFile("holdings.json")));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  @Test
  public void testAllMethodsAndEventPublishing() {
    Account accountToPost = createAccount();
    String accountId = accountToPost.getId();

    // create an account
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    assertBalanceChangedEventPublished(accountToPost);

    // get all accounts
    accountsClient.getAll()
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON);

    // get individual account by id
    accountsClient.getById(accountId)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON);

    Account accountToPut = accountToPost.withRemaining(4.55);

    // put account
    accountsClient.update(accountId, accountToPut)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    assertBalanceChangedEventPublished(accountToPut);

    // delete account
    accountsClient.delete(accountId)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    Account accountToDelete = new Account()
      .withId(accountId)
      .withRemaining(0.00);

    assertBalanceChangedEventPublished(accountToDelete);
  }

  @Test
  public void canPutAccountWithEmptyAdditionalFields() {
    String accountId = randomId();

    accountsClient.create(createAccountJson(accountId))
      .then()
      .contentType(JSON);

    accountsClient.update(accountId,
      createAccountJsonWithEmptyAdditionalFields(accountId))
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

  @Test
  public void eventIsPublishedWhenFeeFineIsClosedWithLoanAndNoRemainingAmount() {
    final String accountId = randomId();
    final String loanId = UUID.randomUUID().toString();

    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", loanId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"));

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject("Paid fully"))
      .put("remaining", 0.0);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId), isAccountPaidFully());

    final Event event = getLastFeeFineClosedEvent();
    assertThat(event, notNullValue());

    assertThat(event, isFeeFineClosedEventPublished());
    assertThat(event.getEventPayload(), allOf(
      hasJsonPath("loanId", is(loanId)),
      hasJsonPath("accountId", is(accountId))
    ));
  }

  @Test
  public void canCloseFeeFineWithLoanIfNoEventSubscribers() {
    wireMock.stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(aResponse().withStatus(400)
        .withBody("There is no SUBSCRIBERS registered for event type "
          + FEEFINE_CLOSED_EVENT_NAME
          + ". Event 1bf88206-ccf4-4b28-b5f1-d90c72cba37b will not be published")));

    final String accountId = randomId();
    final String loanId = UUID.randomUUID().toString();

    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", loanId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"));

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject("Paid fully"))
      .put("remaining", 0.0);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId), isAccountPaidFully());

    assertThat(getLastFeeFineClosedEvent(), notNullValue());
  }

  @Test
  public void eventNotPublishedWhenFeeFineIsClosedWithRemainingAmount() {
    final String accountId = randomId();
    final String loanId = UUID.randomUUID().toString();

    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", loanId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"));

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject("Paid partially"))
      .put("remaining", 0.1);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId).body().asString(), allOf(
      hasJsonPath("status.name", is("Closed")),
      hasJsonPath("paymentStatus.name", is("Paid partially")),
      hasJsonPath("remaining", is(0.1))
    ));
    assertThat(getLastFeeFineClosedEvent(), nullValue());
  }

  @Test
  public void eventNotPublishedWhenFeeFineIsClosedWithoutLoan() {
    final String accountId = randomId();
    final JsonObject account = createAccountJsonObject(accountId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"));

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject("Paid fully"))
      .put("remaining", 0.0);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId), isAccountPaidFully());
    assertThat(getLastFeeFineClosedEvent(), nullValue());
  }

  @Test
  public void eventNotPublishedWhenFeeFineIsOpenButNoRemainingAmount() {
    final String accountId = randomId();
    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", UUID.randomUUID().toString())
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"));

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Open"))
      .put("paymentStatus", createNamedObject("Paid fully"))
      .put("remaining", 0.0);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId).getBody().asString(), allOf(
      hasJsonPath("status.name", is("Open")),
      hasJsonPath("paymentStatus.name", is("Paid fully")),
      hasJsonPath("remaining", is(0.0))
    ));
    assertThat(getLastFeeFineClosedEvent(), nullValue());
  }

  @Test
  public void canForwardPubSubFailureOnFeeFineClose() {
    final String expectedError = "Pub-sub unavailable";
    wireMock.stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(aResponse().withStatus(500).withBody(expectedError)));

    final String accountId = randomId();
    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", UUID.randomUUID().toString())
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"));

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject("Paid fully"))
      .put("remaining", 0.0);

    accountsClient.attemptUpdate(accountId, updatedAccount)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
      .body(containsString(expectedError));
  }

  private Account createAccount() {
    return new Account()
      .withId(randomId())
      .withOwnerId(randomId())
      .withUserId(randomId())
      .withItemId(randomId())
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(9.00)
      .withRemaining(4.55)
      .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
      .withStatus(new Status().withName("Open"));
  }

  private JsonObject createAccountJsonObject(String accountID) {
    return new JsonObject()
      .put("id", accountID)
      .put("userId", randomId())
      .put("feeFineId", randomId())
      .put("materialTypeId", randomId())
      .put("ownerId", randomId())
      .put("itemId", ITEM_ID);
  }

  private JsonObject createAccountJson(String accountID) {
    return createAccountJsonObject(accountID);
  }

  private JsonObject createAccountJsonWithEmptyAdditionalFields(String accountID) {
    return createAccountJsonObject(accountID)
      .put("holdingsRecordId", "")
      .put("instanceId", "");
  }

  private JsonObject createNamedObject(String value) {
    return new JsonObject().put("name", value);
  }

  private Event getLastFeeFineClosedEvent() {
    return getLastPublishedEventOfType(FEEFINE_CLOSED_EVENT_NAME);
  }

  private Event getLastBalanceChangedEvent() {
    return getLastPublishedEventOfType(EventType.FF_BALANCE_CHANGED.toString());
  }

  private Event getLastPublishedEventOfType(String eventType) {
    final FindRequestsResult requests = wireMock.findRequestsMatching(
      postRequestedFor(urlPathMatching("/pubsub/publish")).build());

    return requests.getRequests().stream()
      .filter(request -> StringUtils.isNotBlank(request.getBodyAsString()))
      .filter(request -> decodeValue(request.getBodyAsString(), Event.class)
        .getEventType().equals(eventType))
      .max(Comparator.comparing(LoggedRequest::getLoggedDate))
      .map(LoggedRequest::getBodyAsString)
      .map(JsonObject::new)
      .map(json -> json.mapTo(Event.class))
      .orElse(null);
  }

  private Matcher<Event> isFeeFineClosedEventPublished() {
    return new MappableMatcher<>(Json::encode,
      allOf(
        hasJsonPath("eventType", is(FEEFINE_CLOSED_EVENT_NAME)),
        hasJsonPath("eventMetadata.tenantId", is(TENANT_NAME)),
        hasJsonPath("eventMetadata.publishedBy",
          containsString("mod-feesfines")),
        hasJsonPath("eventPayload", notNullValue())
      ));
  }

  private void assertBalanceChangedEventPublished(Account account) {
    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .until(() -> getLastBalanceChangedEvent() != null);

    final Event event = getLastBalanceChangedEvent();
    assertThat(event, notNullValue());

    EventMetadata eventMetadata = event.getEventMetadata();

    assertEquals(EventType.FF_BALANCE_CHANGED.name(), event.getEventType());
    assertEquals(PubSubClientUtils.constructModuleName(), eventMetadata.getPublishedBy());
    assertEquals(TENANT_NAME, eventMetadata.getTenantId());
    assertEquals(1, eventMetadata.getEventTTL().intValue());

    final JsonObject eventPayload = new JsonObject(event.getEventPayload());

    assertThat(eventPayload.getString("userId"), is(account.getUserId()));
    assertThat(eventPayload.getString("feeFineId"), is(account.getId()));
    assertThat(eventPayload.getString("feeFineTypeId"), is(account.getFeeFineId()));
    assertThat(eventPayload.getDouble("balance"), is(account.getRemaining()));
  }
}
