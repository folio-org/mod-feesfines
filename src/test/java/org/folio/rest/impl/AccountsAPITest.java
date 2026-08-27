package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.restassured.http.ContentType.JSON;
import static io.vertx.core.json.JsonObject.mapFrom;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.OUTSTANDING;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.PAID_FULLY;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.PAID_PARTIALLY;
import static org.folio.test.support.matcher.AccountMatchers.isPaidFully;
import static org.folio.test.support.matcher.AccountMatchers.singleAccountMatcher;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.EventType;
import org.hamcrest.Matcher;
import org.folio.rest.domain.FeeFineKafkaTopic;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ContributorData;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.test.support.ApiTests;
import org.folio.test.support.KafkaTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

public class AccountsAPITest extends ApiTests {
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String CONTRIBUTORS_FIELD_NAME = "contributors";

  @BeforeEach
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
      .contentType(JSON)
      .body(singleAccountMatcher(accountToPost));

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
      .withRemaining(new MonetaryValue(new BigDecimal("0.00")));

    assertBalanceChangedEventPublished(accountToDelete);
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
      .put("paymentStatus", createNamedObject(PAID_FULLY.value()))
      .put("remaining", 0.0);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId), isPaidFully());

    final JsonObject feeFineClosedPayload = getLastFeeFineClosedPayload();
    assertThat(feeFineClosedPayload, notNullValue());
    assertThat(feeFineClosedPayload.getString("loanId"), is(loanId));
  }

  @Test
  public void eventNotPublishedWhenFeeFineIsClosedWithRemainingAmount() {
    final String accountId = randomId();
    final String loanId = UUID.randomUUID().toString();
    final String processId = UUID.randomUUID().toString();

    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", loanId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"))
      .put("processId", processId);

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject(PAID_PARTIALLY.value()))
      .put("remaining", 0.1);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId).body().asString(), allOf(
      hasJsonPath("status.name", is("Closed")),
      hasJsonPath("paymentStatus.name", is(PAID_PARTIALLY.value())),
      hasJsonPath("remaining", is(0.1)),
      hasJsonPath("processId", is(processId))
    ));
    assertThat(getLastFeeFineClosedPayload(), nullValue());
  }

  @Test
  public void eventNotPublishedWhenFeeFineIsClosedWithoutLoan() {
    final String accountId = randomId();

    final JsonObject account = createAccountJsonObject(accountId)
      .put("remaining", 90.00)
      .put("status", createNamedObject("Open"))
      .put("loanId", null);

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject(PAID_FULLY.value()))
      .put("remaining", 0.0);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId), isPaidFully());
    assertThat(getLastFeeFineClosedPayload(), nullValue());
  }

  @Test
  public void eventNotPublishedWhenFeeFineIsOpenButNoRemainingAmount() {
    final String accountId = randomId();
    final String processId = randomId();
    final JsonObject account = createAccountJsonObject(accountId)
      .put("loanId", UUID.randomUUID().toString())
      .put("remaining", 0.0)
      .put("status", createNamedObject("Open"))
      .put("processId", processId);

    accountsClient.create(account);

    final JsonObject updatedAccount = account.copy()
      .put("status", createNamedObject("Closed"))
      .put("paymentStatus", createNamedObject(PAID_FULLY.value()))
      .put("remaining", 0.0);

    accountsClient.update(accountId, updatedAccount);

    assertThat(accountsClient.getById(accountId).body().asString(), allOf(
      hasJsonPath("status.name", is("Closed")),
      hasJsonPath("paymentStatus.name", is(PAID_FULLY.value())),
      hasJsonPath("remaining", is(0.0)),
      hasJsonPath("processId", is(processId))));
    assertThat(getLastFeeFineClosedPayload(), nullValue());
  }

  @Test
  public void canCreateAccountWithoutOptionalReferencedEntityId() {
    assertAccountCreationSuccess(buildAccount().withId(null));
    assertAccountCreationSuccess(buildAccount().withLoanId(null));
    assertAccountCreationSuccess(buildAccount().withItemId(null));
    assertAccountCreationSuccess(buildAccount().withInstanceId(null));
    assertAccountCreationSuccess(buildAccount().withHoldingsRecordId(null));
    assertAccountCreationSuccess(buildAccount().withMaterialTypeId(null));
    assertAccountCreationSuccess(buildAccount().withLoanPolicyId(null));
    assertAccountCreationSuccess(buildAccount().withOverdueFinePolicyId(null));
    assertAccountCreationSuccess(buildAccount().withLostItemFeePolicyId(null));
  }

  @Test
  public void canNotCreateAccountWithoutRequiredReferencedEntityId() {
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
    assertAccountCreationFailure(buildAccount().withLoanPolicyId(invalidId));
    assertAccountCreationFailure(buildAccount().withOverdueFinePolicyId(invalidId));
    assertAccountCreationFailure(buildAccount().withLostItemFeePolicyId(invalidId));
    assertAccountCreationFailure(buildAccount().withProcessId(invalidId));
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
      .withStatus(new Status().withName("Open"))
      .withBarcode("barcode")
      .withCallNumber("call number")
      .withTitle("title")
      .withMaterialType("Material type")
      .withMaterialTypeId(randomId())
      .withLocation("Location")
      .withDueDate(new Date())
      .withReturnedDate(new Date())
      .withLoanId(randomId())
      .withItemId(randomId())
      .withLoanPolicyId(randomId())
      .withOverdueFinePolicyId(randomId())
      .withLostItemFeePolicyId(randomId())
      .withProcessId(randomId());
  }

  private JsonObject createAccountJsonObject(String accountID) {
    return mapFrom(buildAccount().withId(accountID));
  }

  private JsonObject createNamedObject(String value) {
    return new JsonObject().put("name", value);
  }

  /** Returns the payload of the most recent LOAN_RELATED_FEE_FINE_CLOSED Kafka message, or null. */
  private JsonObject getLastFeeFineClosedPayload() {
    String topic = FeeFineKafkaTopic.LOAN_RELATED_FEE_FINE_CLOSED_TOPIC.fullTopicName(TENANT_NAME);
    List<String> messages = KafkaTestHelper.getInstance().pollMessages(topic, testStartTime);
    if (messages.isEmpty()) {
      return null;
    }
    return new JsonObject(messages.get(messages.size() - 1));
  }

  /** Returns the payload of the most recent FEE_FINE_BALANCE_CHANGED Kafka message, or null. */
  private JsonObject getLastBalanceChangedPayload() {
    String topic = FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC.fullTopicName(TENANT_NAME);
    List<String> messages = KafkaTestHelper.getInstance().pollMessages(topic, testStartTime);
    if (messages.isEmpty()) {
      return null;
    }
    return new JsonObject(messages.get(messages.size() - 1));
  }

  private void assertBalanceChangedEventPublished(Account account) {
    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .until(() -> getLastBalanceChangedPayload() != null);

    final JsonObject payload = getLastBalanceChangedPayload();
    assertThat(payload, notNullValue());

    assertEquals(EventType.FEE_FINE_BALANCE_CHANGED.name(),
      FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC.topicName());

    assertThat(payload.getString("userId"), is(account.getUserId()));
    assertThat(payload.getString("feeFineId"), is(account.getId()));
    assertThat(payload.getString("feeFineTypeId"), is(account.getFeeFineId()));
    assertThat(payload.getDouble("balance"), is(account.getRemaining().toDouble()));
    assertThat(payload.getString("loanId"), is(account.getLoanId()));
  }
}
