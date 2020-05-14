package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.folio.test.support.matcher.AccountMatchers.isAccountPaidFully;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.test.support.BaseApiTest;
import org.folio.test.support.matcher.MappableMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.FindRequestsResult;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

public class AccountsAPITest extends BaseApiTest {
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String ITEM_ID = "43ec57e3-3974-4d05-a2c2-95126e087b72";
  public static final String ACCOUNT_CLOSED_EVENT = "FEESFINES_ACCOUNT_WITH_LOAN_CLOSED";

  @Before
  public void setUp() throws Exception {
    wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/inventory/items.*"))
      .willReturn(aResponse().withBodyFile("items.json")));

    wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/holdings-storage/holdings.*"))
      .willReturn(aResponse().withBodyFile("holdings.json")));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  @Test
  public void canGetAccounts() {
    accountsClient.create(createAccountJson(randomId()))
      .then()
      .contentType(ContentType.JSON);

    accountsClient.getAll()
      .then()
      .contentType(ContentType.JSON);
  }

  @Test
  public void canGetAccount() {
    String accountId = randomId();

    accountsClient.create(createAccountJson(accountId))
      .then()
      .contentType(ContentType.JSON);

    accountsClient.getById(accountId)
      .then()
      .contentType(ContentType.JSON);
  }

  @Test
  public void canPutAccount() {
    String accountId = randomId();

    accountsClient.create(createAccountJson(accountId))
      .then()
      .contentType(ContentType.JSON);

    accountsClient.update(accountId, createAccountJson(accountId))
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

  @Test
  public void canPutAccountWithEmptyAdditionalFields() {
    String accountId = randomId();

    accountsClient.create(createAccountJson(accountId))
      .then()
      .contentType(ContentType.JSON);

    accountsClient.update(accountId,
      createAccountJsonWithEmptyAdditionalFields(accountId))
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

  @Test
  public void eventIsPublishedWhenAccountIsClosedWithLoanAndNoRemainingAmount() {
    wireMock.stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(noContent()));

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

    final JsonObject publishRequest = getEventPublishRequest();
    assertThat(publishRequest, notNullValue());

    assertThat(publishRequest, isEventPublished());
    assertThat(publishRequest.getString("eventPayload"), allOf(
      hasJsonPath("loanId", is(loanId)),
      hasJsonPath("accountId", is(accountId))
    ));
  }

  @Test
  public void canCloseAccountWithLoanIfNoEventSubscribers() {
    wireMock.stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(aResponse().withStatus(400)
        .withBody("There is no SUBSCRIBERS registered for event type "
          + ACCOUNT_CLOSED_EVENT
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

    final JsonObject publishRequest = getEventPublishRequest();
    assertThat(publishRequest, notNullValue());
  }

  @Test
  public void eventNotPublishedWhenAccountIsClosedWithRemainingAmount() {
    wireMock.stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(noContent()));

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
    assertThat(getEventPublishRequest(), nullValue());
  }

  @Test
  public void eventNotPublishedWhenAccountIsClosedWithoutLoan() {
    wireMock.stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(noContent()));

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
    assertThat(getEventPublishRequest(), nullValue());
  }

  @Test
  public void eventNotPublishedWhenAccountIsOpenButNoRemainingAmount() {
    wireMock.stubFor(WireMock.post(urlPathEqualTo("/pubsub/publish"))
      .willReturn(noContent()));

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
    assertThat(getEventPublishRequest(), nullValue());
  }

  @Test
  public void canForwardPubSubFailureOnAccountClose() {
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

  private String randomId() {
    return UUID.randomUUID().toString();
  }

  private JsonObject createNamedObject(String value) {
    return new JsonObject().put("name", value);
  }

  private JsonObject getEventPublishRequest() {
    final FindRequestsResult requests = wireMock.findRequestsMatching(
      postRequestedFor(urlPathMatching("/pubsub/publish")).build());

    return requests.getRequests().size() == 1
      ? new JsonObject(requests.getRequests().get(0).getBodyAsString())
      : null;
  }

  private Matcher<JsonObject> isEventPublished() {
    return new MappableMatcher<>(JsonObject::toString,
      allOf(
        hasJsonPath("eventType", is(ACCOUNT_CLOSED_EVENT)),
        hasJsonPath("eventMetadata.tenantId", is(TENANT_NAME)),
        hasJsonPath("eventMetadata.publishedBy",
          containsString("mod-feesfines")),
        hasJsonPath("eventPayload", notNullValue())
      ));
  }
}
