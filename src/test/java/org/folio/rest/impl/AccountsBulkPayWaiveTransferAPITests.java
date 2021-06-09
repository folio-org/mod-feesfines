package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.utils.LogEventUtils.fetchLogEventPayloads;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkWaiveClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.test.support.matcher.FeeFineActionMatchers.feeFineAction;
import static org.folio.test.support.matcher.LogEventMatcher.feeFineActionLogEventPayload;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.EventType;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.DefaultBulkActionRequest;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ActionsAPITests;
import org.folio.util.pubsub.PubSubClientUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

@RunWith(value = Parameterized.class)
public class AccountsBulkPayWaiveTransferAPITests extends ActionsAPITests {
  private static final String FEE_FINE_ACTIONS = "feefineactions";

  private static final String AMOUNT_KEY = "amount";
  private static final String ACCOUNT_IDS_KEY = "accountIds";
  private static final String ERROR_MESSAGE_KEY = "errorMessage";

  private static final String USER_ID = randomId();
  private static final String FIRST_ACCOUNT_ID = randomId();
  private static final String SECOND_ACCOUNT_ID = randomId();
  private static final List<String> FIRST_ACCOUNT_ID_AS_LIST = singletonList(FIRST_ACCOUNT_ID);
  private static final List<String> TWO_ACCOUNT_IDS = Arrays.asList(FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID);

  private final ResourceClient actionsClient = buildFeeFineActionsClient();
  private final Action action;
  private ResourceClient resourceClient;

  public AccountsBulkPayWaiveTransferAPITests(Action action) {
    this.action = action;
  }

  @Parameters(name = "{0}")
  public static Object[] parameters() {
    return new Object[] { PAY, WAIVE, TRANSFER };
  }

  @Before
  public void beforeEach() {
    removeAllFromTable(FEE_FINE_ACTIONS);
    removeAllFromTable("accounts");
    resourceClient = getClient();
  }

  private ResourceClient getClient() {
    switch (action) {
    case PAY:
      return buildAccountBulkPayClient();
    case WAIVE:
      return buildAccountBulkWaiveClient();
    case TRANSFER:
      return buildAccountBulkTransferClient();
    default:
      throw new IllegalArgumentException("Failed to get ResourceClient for action: " + action.name());
    }
  }

  @Test
  public void return404WhenAccountDoesNotExist() {
    resourceClient.post(createRequestJson(String.valueOf(10.0), FIRST_ACCOUNT_ID_AS_LIST))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(equalTo(format("Fee/fine ID %s not found", FIRST_ACCOUNT_ID)));
  }

  @Test
  public void return422WhenRequestedAmountIsNegative() {
    testRequestWithNonPositiveAmount(-1);
  }

  @Test
  public void return422WhenRequestedAmountIsZero() {
    testRequestWithNonPositiveAmount(0);
  }

  private void testRequestWithNonPositiveAmount(double amount) {
    postAccount(createAccount(FIRST_ACCOUNT_ID, 1.0));

    String amountString = String.valueOf(amount);

    resourceClient.post(createRequestJson(amountString, FIRST_ACCOUNT_ID_AS_LIST))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(AMOUNT_KEY, is(amountString))
      .body(ACCOUNT_IDS_KEY, hasItem(FIRST_ACCOUNT_ID))
      .body(ERROR_MESSAGE_KEY, is("Amount must be positive"));
  }

  @Test
  public void return422WhenRequestedAmountIsInvalidString() {
    postAccount(createAccount(FIRST_ACCOUNT_ID, 1.0));

    String invalidAmount = "eleven";

    resourceClient.post(createRequestJson(invalidAmount, FIRST_ACCOUNT_ID_AS_LIST))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(AMOUNT_KEY, is(invalidAmount))
      .body(ACCOUNT_IDS_KEY, hasItem(FIRST_ACCOUNT_ID))
      .body(ERROR_MESSAGE_KEY, is("Invalid amount entered"));
  }

  @Test
  public void return422WhenRequestedAmountExceedsRemainingAmount() {
    postAccount(createAccount(FIRST_ACCOUNT_ID, 1.0));
    postAccount(createAccount(SECOND_ACCOUNT_ID, 1.0));

    String requestedAmount = "3.0";

    resourceClient.post(createRequestJson(requestedAmount, TWO_ACCOUNT_IDS))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(AMOUNT_KEY, is(requestedAmount))
      .body(ACCOUNT_IDS_KEY, hasItem(FIRST_ACCOUNT_ID))
      .body(ACCOUNT_IDS_KEY, hasItem(SECOND_ACCOUNT_ID))
      .body(ERROR_MESSAGE_KEY, is("Requested amount exceeds remaining amount"));
  }

  @Test
  public void return422WhenAccountIsClosed() {
    return422WhenAccountIsEffectivelyClosed(0.00);
  }

  @Test
  public void return422WhenAccountIsEffectivelyClosed() {
    // will be rounded to 0.00 (2 decimal places) when compared to zero
    return422WhenAccountIsEffectivelyClosed(0.004987654321);
  }

  private void return422WhenAccountIsEffectivelyClosed(double remainingAmount) {
    Account closedAccount = createAccount(FIRST_ACCOUNT_ID, remainingAmount)
      .withAmount(remainingAmount + 1)
      .withStatus(new Status().withName(FeeFineStatus.CLOSED.getValue()));

    postAccount(closedAccount);

    String requestedAmount = "1.23";

    resourceClient.post(createRequestJson(requestedAmount, FIRST_ACCOUNT_ID_AS_LIST))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(AMOUNT_KEY, is(requestedAmount))
      .body(ACCOUNT_IDS_KEY, hasItem(FIRST_ACCOUNT_ID))
      .body(ERROR_MESSAGE_KEY, is("Fee/fine is already closed"));
  }

  @Test
  public void longDecimalsAreHandledCorrectlyAndAccountIsClosed() {
    double accountBalanceBeforeAction = 1.004987654321;
    final Account account = createAccount(FIRST_ACCOUNT_ID, accountBalanceBeforeAction);
    postAccount(account);

    String requestedAmountString = "1.004123456789";
    String expectedPaymentStatus = action.getFullResult();

    DefaultBulkActionRequest request = createRequest(requestedAmountString, FIRST_ACCOUNT_ID_AS_LIST);

    resourceClient.post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body(AMOUNT_KEY, is("1.00"))
      .body(ACCOUNT_IDS_KEY, hasItem(FIRST_ACCOUNT_ID));

    actionsClient.getAll()
      .then()
      .body(FEE_FINE_ACTIONS, hasSize(1))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("amountAction", is(1.0f)),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is(expectedPaymentStatus))
      )));

    verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID, expectedPaymentStatus, 0.0, "Closed");
  }

  @Test
  public void longDecimalsAreHandledCorrectly() {
    double accountBalanceBeforeAction = 1.23987654321; // should be rounded to 1.24
    Account account = createAccount(FIRST_ACCOUNT_ID, accountBalanceBeforeAction);
    postAccount(account);

    String requestedAmountString = "1.004987654321"; // should be rounded to 1.00
    String expectedPaymentStatus = action.getPartialResult();

    DefaultBulkActionRequest request = createRequest(requestedAmountString, FIRST_ACCOUNT_ID_AS_LIST);

    resourceClient.post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body(AMOUNT_KEY, is("1.00"))
      .body(ACCOUNT_IDS_KEY, hasItem(FIRST_ACCOUNT_ID));

    actionsClient.getAll()
      .then()
      .body(FEE_FINE_ACTIONS, hasSize(1))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("amountAction", is(1.00f)),
        hasJsonPath("balance", is(0.24f)), // 1.24 - 1.00
        hasJsonPath("typeAction", is(expectedPaymentStatus))
      )));

    verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID, expectedPaymentStatus, 0.24, "Open");
  }

  @Test
  public void paymentCreatesActionsAndUpdatesAccounts() {
    double remainingAmount1 = 2.0;
    double remainingAmount2 = 1.5;
    String requestedAmount = "3.00";

    Account account1 = createAccount(FIRST_ACCOUNT_ID, remainingAmount1);
    Account account2 = createAccount(SECOND_ACCOUNT_ID, remainingAmount2);

    postAccount(account1);
    postAccount(account2);

    DefaultBulkActionRequest request = createRequest(requestedAmount, TWO_ACCOUNT_IDS);

    double expectedActionAmount = 1.5;
    double expectedRemainingAmount1 = 0.5;
    double expectedRemainingAmount2 = 0.0;

    String expectedPaymentStatus1 = action.getPartialResult();
    String expectedPaymentStatus2 = action.getFullResult();

    String expectedAccountStatus1 = FeeFineStatus.OPEN.getValue();
    String expectedAccountStatus2 = FeeFineStatus.CLOSED.getValue();

    Matcher<JsonObject> feeFineActionsMatcher = allOf(
      hasItem(
        feeFineAction(FIRST_ACCOUNT_ID, account1.getUserId(), expectedRemainingAmount1,
          expectedActionAmount, expectedPaymentStatus1, request.getTransactionInfo(), request)),
      hasItem(
        feeFineAction(SECOND_ACCOUNT_ID, account2.getUserId(), expectedRemainingAmount2,
          expectedActionAmount, expectedPaymentStatus2, request.getTransactionInfo(), request)));

    resourceClient.post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body(AMOUNT_KEY, is(requestedAmount))
      .body(ACCOUNT_IDS_KEY, is(TWO_ACCOUNT_IDS))
      .body(FEE_FINE_ACTIONS, feeFineActionsMatcher);

    actionsClient.getAll()
      .then()
      .body(FEE_FINE_ACTIONS, hasSize(2))
      .body(FEE_FINE_ACTIONS, feeFineActionsMatcher);

    verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID, expectedPaymentStatus1,
      expectedRemainingAmount1, expectedAccountStatus1);

    verifyAccountAndGet(accountsClient, SECOND_ACCOUNT_ID, expectedPaymentStatus2,
      expectedRemainingAmount2, expectedAccountStatus2);

    verifyThatEventWasSent(EventType.FEE_FINE_BALANCE_CHANGED, new JsonObject()
      .put("userId", account1.getUserId())
      .put("feeFineId", account1.getId())
      .put("feeFineTypeId", account1.getFeeFineId())
      .put("balance", expectedRemainingAmount1)
      .put("loanId", account1.getLoanId()));

    verifyThatEventWasSent(EventType.FEE_FINE_BALANCE_CHANGED, new JsonObject()
      .put("userId", account2.getUserId())
      .put("feeFineId", account2.getId())
      .put("feeFineTypeId", account2.getFeeFineId())
      .put("balance", expectedRemainingAmount2)
      .put("loanId", account2.getLoanId()));

    verifyThatEventWasSent(EventType.LOAN_RELATED_FEE_FINE_CLOSED, new JsonObject()
      .put("loanId", account2.getLoanId())
      .put("feeFineId", account2.getId()));
    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS);

    fetchLogEventPayloads(getOkapi()).forEach(payload -> assertThat(payload,
      is(either(feeFineActionLogEventPayload(account1, request, action.getPartialResult(),
          expectedActionAmount, expectedRemainingAmount1))
        .or(feeFineActionLogEventPayload(account2, request, action.getFullResult(),
        expectedActionAmount, expectedRemainingAmount2)))));
  }

  private Account createAccount(String accountId, double amount) {
    return new Account()
      .withId(accountId)
      .withOwnerId(randomId())
      .withUserId(USER_ID)
      .withBarcode("barcode")
      .withItemId(randomId())
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(amount)
      .withRemaining(amount)
      .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
      .withStatus(new Status().withName("Open"));
  }

  private void postAccount(Account account) {
    accountsClient.create(account)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
  }

  private static DefaultBulkActionRequest createRequest(String amount, List<String> accountIds) {
    return new DefaultBulkActionRequest()
      .withAmount(amount)
      .withAccountIds(accountIds)
      .withPaymentMethod("Cash")
      .withServicePointId(randomId())
      .withTransactionInfo("Check #12345")
      .withUserName("Folio, Tester")
      .withNotifyPatron(false)
      .withComments("STAFF : staff comment \\n PATRON : patron comment");
  }

  private static String createRequestJson(String amount, List<String> accountIds) {
    return toJson(createRequest(amount, accountIds));
  }

  private static String toJson(Object object) {
    return JsonObject.mapFrom(object).encodePrettily();
  }

  private void verifyThatEventWasSent(EventType eventType, JsonObject eventPayload) {
    Event event = new Event()
      .withEventType(eventType.name())
      .withEventPayload(eventPayload.encode())
      .withEventMetadata(new EventMetadata()
        .withPublishedBy(PubSubClientUtils.getModuleId())
        .withTenantId(TENANT_NAME)
        .withEventTTL(1));

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> getOkapi().verify(postRequestedFor(urlPathEqualTo("/pubsub/publish"))
        .withRequestBody(equalToJson(toJson(event), true, true))
      ));
  }
}
