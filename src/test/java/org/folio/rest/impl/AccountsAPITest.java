package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.restassured.http.ContentType.JSON;
import static io.vertx.core.json.Json.decodeValue;
import static io.vertx.core.json.JsonObject.mapFrom;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.OUTSTANDING;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.PAID_FULLY;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.PAID_PARTIALLY;
import static org.folio.test.support.matcher.AccountMatchers.isPaidFully;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.EventType;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.jaxrs.model.ContributorData;
import org.folio.test.support.ApiTests;
import org.folio.test.support.matcher.TypeMappingMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.restassured.response.Response;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import static org.hamcrest.CoreMatchers.equalTo;

public class AccountsAPITest extends ApiTests {
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String FEEFINE_CLOSED_EVENT_NAME = "LOAN_RELATED_FEE_FINE_CLOSED";
  private static final String CONTRIBUTORS_FIELD_NAME = "contributors";
  private static final String DATE_CLOSED = new Date().toString();

  @Before
  public void setUp() {
    getOkapi().stubFor(WireMock.get(WireMock.urlPathMatching("/holdings-storage/holdings.*"))
      .willReturn(aResponse().withBodyFile("holdings.json")));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  @Test
  public void testAllMethodsAndEventPublishing() {
    List<ContributorData> contributorsToPost = List.of(
      new ContributorData().withName("contributor name 1"),
      new ContributorData().withName("contributor name 2")
    );
    Account accountToPost = buildAccount()
      .withContributors(contributorsToPost);
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

    Matcher<JsonObject> contributorsAfterPostMatcher = allOf(
      hasItem(hasJsonPath("name", is(contributorsToPost.get(0).getName()))),
      hasItem(hasJsonPath("name", is(contributorsToPost.get(1).getName())))
    );

    // get individual account by id
    accountsClient.getById(accountId)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body(CONTRIBUTORS_FIELD_NAME, hasSize(2))
      .body(CONTRIBUTORS_FIELD_NAME, contributorsAfterPostMatcher);

    List<ContributorData> contributorsToPut = List.of(
      new ContributorData().withName("contributor name 3")
    );
    Account accountToPut = accountToPost
      .withRemaining(new MonetaryValue(4.55))
      .withContributors(contributorsToPut);

    // put account
    accountsClient.update(accountId, accountToPut)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    assertBalanceChangedEventPublished(accountToPut);

    Matcher<JsonObject> contributorsAfterPutMatcher = allOf(
      hasItem(hasJsonPath("name", is(contributorsToPut.get(0).getName())))
    );

    accountsClient.getById(accountId)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body(CONTRIBUTORS_FIELD_NAME, hasSize(1))
      .body(CONTRIBUTORS_FIELD_NAME, contributorsAfterPutMatcher);

    // delete account
    accountsClient.delete(accountId)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    Account accountToDelete = new Account()
      .withId(accountId)
      .withRemaining(new MonetaryValue(new BigDecimal("0.00")))
      .withDateClosed(new Date());
    assertBalanceChangedEventPublished(accountToDelete);
  }

  @Test
  public void eventIsPublishedWhenFeeFineIsClosedWithLoanAndNoRemainingAmount() {
    final String accountId = randomId();
    final String loanId = UUID.randomUUID().toString();

    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", loanId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"))
      .put("dateClosed", null);

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject(PAID_FULLY.value()))
      .put("remaining", 0.0)
      .put("dateClosed", DATE_CLOSED);

    accountsClient.update(accountId, updatedAccount);

    Response byId = accountsClient.getById(accountId);
    assertThat(byId, isPaidFully());

    assertThat(accountsClient.getById(accountId).body().asString(), allOf(
      hasJsonPath("dateClosed", equalTo(DATE_CLOSED))
    ));

    final Event event = getLastFeeFineClosedEvent();
    assertThat(event, notNullValue());

    assertThat(event, isFeeFineClosedEventPublished());
    assertThat(event.getEventPayload(), allOf(
      hasJsonPath("loanId", is(loanId)),
      hasJsonPath("feeFineId", is(accountId))
    ));
  }

  @Test
  public void canCloseFeeFineWithLoanIfNoEventSubscribers() {
    getOkapi().stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(aResponse().withStatus(400)
        .withBody("There is no SUBSCRIBERS registered for event type "
          + FEEFINE_CLOSED_EVENT_NAME
          + ". Event 1bf88206-ccf4-4b28-b5f1-d90c72cba37b will not be published")));

    final String accountId = randomId();
    final String loanId = UUID.randomUUID().toString();

    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", loanId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"))
      .put("dateClosed", null);
    
    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject(PAID_FULLY.value()))
      .put("remaining", 0.0)
      .put("dateClosed", DATE_CLOSED);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId), isPaidFully());
    assertThat(accountsClient.getById(accountId).body().asString(), allOf(
      hasJsonPath("dateClosed", equalTo(DATE_CLOSED))
    ));
    assertThat(getLastFeeFineClosedEvent(), notNullValue());
  }

  @Test
  public void eventNotPublishedWhenFeeFineIsClosedWithRemainingAmount() {
    final String accountId = randomId();
    final String loanId = UUID.randomUUID().toString();

    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", loanId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"))
      .put("dateClosed", null);

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject(PAID_PARTIALLY.value()))
      .put("remaining", 0.1)
      .put("dateClosed", DATE_CLOSED);


    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId).body().asString(), allOf(
      hasJsonPath("status.name", is("Closed")),
      hasJsonPath("dateClosed", equalTo(DATE_CLOSED)),
      hasJsonPath("paymentStatus.name", is(PAID_PARTIALLY.value())),
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
      .put("paymentStatus", createNamedObject(PAID_FULLY.value()))
      .put("remaining", 0.0)
      .put("dateClosed", DATE_CLOSED);

    accountsClient.update(accountId, updatedAccount);
    assertThat(accountsClient.getById(accountId).body().asString(), allOf(
      hasJsonPath("dateClosed", equalTo(DATE_CLOSED))
    ));
    assertThat(accountsClient.getById(accountId), isPaidFully());
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
      .put("paymentStatus", createNamedObject(PAID_FULLY.value()))
      .put("remaining", 0.0);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId).getBody().asString(), allOf(
      hasJsonPath("status.name", is("Open")),
      hasJsonPath("paymentStatus.name", is(PAID_FULLY.value())),
      hasJsonPath("remaining", is(0.0))
    ));
    assertThat(getLastFeeFineClosedEvent(), nullValue());
  }

  @Test
  public void canForwardPubSubFailureOnFeeFineClose() {
    final MonetaryValue remaining = new MonetaryValue(0.0);
    final String expectedError = "Pub-sub unavailable";
    getOkapi().stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(aResponse().withStatus(500).withBody(expectedError)));

    final String accountId = randomId();
    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", UUID.randomUUID().toString())
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"))
      .put("dateClosed", null);

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject(PAID_FULLY.value()))
      .put("remaining", remaining.toDouble())
      .put("dateClosed", new Date());

    accountsClient.attemptUpdate(accountId, updatedAccount)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
      .body(containsString(expectedError));
  }

  @Test
  public void canCreateAccountWithoutOptionalReferencedEntityId() {
    assertAccountCreationSuccess(buildAccount().withLoanId(null));
    assertAccountCreationSuccess(buildAccount().withItemId(null));
    assertAccountCreationSuccess(buildAccount().withInstanceId(null));
    assertAccountCreationSuccess(buildAccount().withHoldingsRecordId(null));
    assertAccountCreationSuccess(buildAccount().withMaterialTypeId(null));
  }

  @Test
  public void canNotCreateAccountWithoutRequiredReferencedEntityId() {
    assertAccountCreationFailure(buildAccount().withId(null));
    assertAccountCreationFailure(buildAccount().withFeeFineId(null));
    assertAccountCreationFailure(buildAccount().withUserId(null));
    assertAccountCreationFailure(buildAccount().withOwnerId(null));
  }

  @Test
  public void canNotCreateAccountWithInvalidUuid() {
    final String invalidId = "0";

    assertAccountCreationFailure(buildAccount().withId(invalidId));
    assertAccountCreationFailure(buildAccount().withFeeFineId(invalidId));
    assertAccountCreationFailure(buildAccount().withUserId(invalidId));
    assertAccountCreationFailure(buildAccount().withOwnerId(invalidId));
    assertAccountCreationFailure(buildAccount().withLoanId(invalidId));
    assertAccountCreationFailure(buildAccount().withItemId(invalidId));
    assertAccountCreationFailure(buildAccount().withInstanceId(invalidId));
    assertAccountCreationFailure(buildAccount().withHoldingsRecordId(invalidId));
    assertAccountCreationFailure(buildAccount().withMaterialTypeId(invalidId));
  }

  private void assertAccountCreationFailure(Account account) {
    accountsClient.attemptCreate(mapFrom(account))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
  }

  private void assertAccountCreationSuccess(Account account) {
    accountsClient.attemptCreate(mapFrom(account))
      .then()
      .statusCode(HttpStatus.SC_CREATED);
  }

  private static Account buildAccount() {
    return new Account()
      .withId(randomId())
      .withOwnerId(randomId())
      .withUserId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(new MonetaryValue(new BigDecimal("7.77")))
      .withRemaining(new MonetaryValue(new BigDecimal("3.33")))
      .withPaymentStatus(new PaymentStatus().withName(OUTSTANDING))
      .withStatus(new Status().withName("Open"));

  }

  private JsonObject createAccountJsonObject(String accountID) {
    return mapFrom(buildAccount().withId(accountID));
  }

  private JsonObject createNamedObject(String value) {
    return new JsonObject().put("name", value);
  }

  private Event getLastFeeFineClosedEvent() {
    return getLastPublishedEventOfType(FEEFINE_CLOSED_EVENT_NAME);
  }

  private Event getLastBalanceChangedEvent() {
    return getLastPublishedEventOfType(EventType.FEE_FINE_BALANCE_CHANGED.toString());
  }

  private Event getLastPublishedEventOfType(String eventType) {
    final FindRequestsResult requests = getOkapi().findRequestsMatching(
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
    return new TypeMappingMatcher<>(Json::encode,
      allOf(
        hasJsonPath("eventType", is(FEEFINE_CLOSED_EVENT_NAME)),
        hasJsonPath("eventMetadata.tenantId", is(TENANT_NAME)),
        hasJsonPath("eventMetadata.publishedBy",
          matchesPattern("mod-feesfines-[0-9]+\\.[0-9]+\\.[0-9]+")),
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

    assertEquals(EventType.FEE_FINE_BALANCE_CHANGED.name(), event.getEventType());
    assertThat(eventMetadata.getPublishedBy(),
      matchesPattern("mod-feesfines-[0-9]+\\.[0-9]+\\.[0-9]+"));
    assertEquals(TENANT_NAME, eventMetadata.getTenantId());
    assertEquals(1, eventMetadata.getEventTTL().intValue());

    final JsonObject eventPayload = new JsonObject(event.getEventPayload());

    assertThat(eventPayload.getString("userId"), is(account.getUserId()));
    assertThat(eventPayload.getString("feeFineId"), is(account.getId()));
    assertThat(eventPayload.getString("feeFineTypeId"), is(account.getFeeFineId()));
    assertThat(eventPayload.getDouble("balance"), is(account.getRemaining().toDouble()));
    assertThat(eventPayload.getString("loanId"), is(account.getLoanId()));
  }
}
