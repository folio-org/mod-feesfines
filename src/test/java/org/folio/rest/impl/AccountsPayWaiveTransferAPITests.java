package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.domain.MonetaryValue.ZERO;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.OUTSTANDING;
import static org.folio.rest.utils.LogEventUtils.fetchLogEventPayloads;
import static org.folio.rest.utils.ResourceClients.buildAccountPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountWaiveClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.test.support.matcher.FeeFineActionMatchers.feeFineAction;
import static org.folio.test.support.matcher.LogEventMatcher.feeFineActionLogEventPayload;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.EventType;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActionFailureResponse;
import org.folio.rest.jaxrs.model.DefaultActionRequest;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ActionsAPITests;
import org.folio.util.PomUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

public class AccountsPayWaiveTransferAPITests extends ActionsAPITests {
  private static final String ACCOUNT_ID = randomId();
  private static final String FEE_FINE_ACTIONS = "feefineactions";

  private final ResourceClient actionsClient = buildFeeFineActionsClient();

  public static Stream<Arguments> parameters() {
    return Stream.of(Arguments.of(PAY), Arguments.of(WAIVE), Arguments.of(TRANSFER));
  }

  @BeforeEach
  public void beforeEach() {
    removeAllFromTable(FEE_FINE_ACTIONS);
    removeAllFromTable("accounts");
//    resourceClient = getClient();
  }

  private ResourceClient getClient(Action action) {
    switch (action) {
    case PAY:
      return buildAccountPayClient(ACCOUNT_ID);
    case WAIVE:
      return buildAccountWaiveClient(ACCOUNT_ID);
    case TRANSFER:
      return buildAccountTransferClient(ACCOUNT_ID);
    default:
      throw new IllegalArgumentException(
        "Failed to get ResourceClient for action: " + action.name());
    }
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void return404WhenAccountDoesNotExist(Action action) {
    ResourceClient resourceClient = getClient(action);
    resourceClient.post(createRequestJson(String.valueOf(10.0)))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(equalTo(format("Fee/fine ID %s not found", ACCOUNT_ID)));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void return422WhenRequestedAmountIsNegative(Action action) {
    testRequestWithNonPositiveAmount(-1.0, action);
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void return422WhenRequestedAmountIsZero(Action action) {
    testRequestWithNonPositiveAmount(0.0, action);
  }

  private void testRequestWithNonPositiveAmount(double amount, Action action) {
    postAccount(createAccount(1.0));

    String amountString = String.valueOf(amount);

    ActionFailureResponse expectedResponse = new ActionFailureResponse()
      .withAmount(amountString)
      .withAccountId(ACCOUNT_ID)
      .withErrorMessage("Amount must be positive");

    getClient(action).post(createRequestJson(amountString))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(equalTo(toJson(expectedResponse)));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void return422WhenRequestedAmountIsInvalidString(Action action) {
    postAccount(createAccount(1.0));

    String invalidAmount = "eleven";

    ActionFailureResponse expectedResponse = new ActionFailureResponse()
      .withAmount(invalidAmount)
      .withAccountId(ACCOUNT_ID)
      .withErrorMessage("Invalid amount entered");

    getClient(action).post(createRequestJson(invalidAmount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(equalTo(toJson(expectedResponse)));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void return422WhenAccountIsClosed(Action action) {
    return422WhenAccountIsEffectivelyClosed(new MonetaryValue(0.0), action);
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void return422WhenAccountIsEffectivelyClosed(Action action) {
    // will be rounded to 0.00 (2 decimal places) when compared to zero
    return422WhenAccountIsEffectivelyClosed(new MonetaryValue(0.004987654321), action);
  }

  private void return422WhenAccountIsEffectivelyClosed(MonetaryValue remainingAmount,
    Action action) {

    Account account = createAccount(
      remainingAmount.add(new MonetaryValue(1.0)).toDouble()).withRemaining(
      remainingAmount);
    account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
    postAccount(account);

    String requestedAmount = "1.23";

    ActionFailureResponse expectedResponse = new ActionFailureResponse()
      .withAmount(requestedAmount)
      .withAccountId(ACCOUNT_ID)
      .withErrorMessage("Fee/fine is already closed");

    getClient(action).post(createRequestJson(requestedAmount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(equalTo(toJson(expectedResponse)));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void longDecimalsAreHandledCorrectlyAndAccountIsClosed(Action action) {
    final Account account = createAccount(1.004987654321);
    postAccount(account);

    String requestedAmountString = "1.004123456789";
    String expectedPaymentStatus = action.getFullResult();
    MonetaryValue expectedRemainingAmount = ZERO;

    final DefaultActionRequest request = createRequest(requestedAmountString);

    getClient(action).post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body("amount", is("1.00"))
      .body("remainingAmount", is(expectedRemainingAmount.toString()))
      .body("accountId", is(ACCOUNT_ID));

    actionsClient.getAll()
      .then()
      .body(FEE_FINE_ACTIONS, hasSize(1))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("amountAction", is(1.0f)),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is(expectedPaymentStatus))
      )));

    verifyAccountAndGet(accountsClient, ACCOUNT_ID, expectedPaymentStatus, expectedRemainingAmount,
      "Closed");

    assertThat(fetchLogEventPayloads(getOkapi()).get(0),
      is(feeFineActionLogEventPayload(account, request, action.getFullResult(), 1.0,
        0.0)));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void longDecimalsAreHandledCorrectly(Action action) {
    final Account account = createAccount(1.23987654321); // should be rounded to 1.24
    postAccount(account);

    String requestedAmountString = "1.004987654321"; // should be rounded to 1.00
    String expectedPaymentStatus = action.getPartialResult();
    MonetaryValue expectedRemainingAmount = new MonetaryValue(0.24);

    final DefaultActionRequest request = createRequest(requestedAmountString);

    getClient(action).post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body("amount", is("1.00"))
      .body("remainingAmount", is(expectedRemainingAmount.toString()))
      .body("accountId", is(ACCOUNT_ID));

    actionsClient.getAll()
      .then()
      .body(FEE_FINE_ACTIONS, hasSize(1))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("amountAction", is(1.00f)),
        hasJsonPath("balance", is(0.24f)), // 1.24 - 1.00
        hasJsonPath("typeAction", is(expectedPaymentStatus))
      )));

    verifyAccountAndGet(accountsClient, ACCOUNT_ID, expectedPaymentStatus, expectedRemainingAmount,
      "Open");

    assertThat(fetchLogEventPayloads(getOkapi()).get(0),
      is(feeFineActionLogEventPayload(account, request, action.getPartialResult(),
        1.0,0.24)));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void partialActionCreatesActionAndUpdatesAccount(Action action) {
    paymentCreatesActionAndUpdatesAccount(false, action);
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void fullActionCreatesActionAndClosesAccount(Action action) {
    paymentCreatesActionAndUpdatesAccount(true, action);
  }

  private void paymentCreatesActionAndUpdatesAccount(boolean terminalAction,
    Action action) {

    MonetaryValue accountBalanceBefore = new MonetaryValue(3.45);
    MonetaryValue requestedAmount = terminalAction ?
      accountBalanceBefore :
      accountBalanceBefore.subtract(new MonetaryValue(1.0));
    MonetaryValue expectedAccountBalanceAfter = accountBalanceBefore.subtract(requestedAmount);

    final Account account = createAccount(accountBalanceBefore.toDouble());
    postAccount(account);

    String expectedPaymentStatus = terminalAction ?
      action.getFullResult() :
      action.getPartialResult();
    String expectedAccountStatus = terminalAction ? "Closed" : "Open";
    String requestedAmountString = String.valueOf(requestedAmount);

    final DefaultActionRequest request = createRequest(requestedAmountString);

    getClient(action).post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body("amount", is(requestedAmountString))
      .body("remainingAmount", is(expectedAccountBalanceAfter.toString()))
      .body("accountId", is(ACCOUNT_ID))
      .body(FEE_FINE_ACTIONS, hasSize(1));

    actionsClient.getAll()
      .then()
      .body(FEE_FINE_ACTIONS, hasSize(1))
      .body(FEE_FINE_ACTIONS, hasItem(
        feeFineAction(ACCOUNT_ID, account.getUserId(), expectedAccountBalanceAfter.toDouble(), requestedAmount,
          expectedPaymentStatus, request.getTransactionInfo(), request))
      );

    verifyAccountAndGet(accountsClient, ACCOUNT_ID, expectedPaymentStatus,
      expectedAccountBalanceAfter, expectedAccountStatus);

    verifyThatEventWasSent(EventType.FEE_FINE_BALANCE_CHANGED, new JsonObject()
      .put("userId", account.getUserId())
      .put("feeFineId", account.getId())
      .put("feeFineTypeId", account.getFeeFineId())
      .put("balance", account.getRemaining())
      .put("loanId", account.getLoanId()));

    if (terminalAction && account.getLoanId() != null) {
      verifyThatEventWasSent(EventType.LOAN_RELATED_FEE_FINE_CLOSED, new JsonObject()
        .put("loanId", account.getLoanId())
        .put("feeFineId", account.getId()));
    }

    assertThat(fetchLogEventPayloads(getOkapi()).get(0),
      is(feeFineActionLogEventPayload(account, request,
        terminalAction ? action.getFullResult() : action.getPartialResult(),
        requestedAmount.toDouble(), expectedAccountBalanceAfter.toDouble())));
  }

  private Account createAccount(double amount) {
    return new Account()
      .withId(ACCOUNT_ID)
      .withOwnerId(randomId())
      .withUserId(randomId())
      .withBarcode("barcode")
      .withItemId(randomId())
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(new MonetaryValue(amount))
      .withRemaining(new MonetaryValue(amount))
      .withPaymentStatus(new PaymentStatus().withName(OUTSTANDING))
      .withStatus(new Status().withName("Open"));
  }

  private void postAccount(Account account) {
    accountsClient.create(account)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
  }

  private static DefaultActionRequest createRequest(String amount) {
    return new DefaultActionRequest()
      .withAmount(amount)
      .withPaymentMethod("Cash")
      .withServicePointId(randomId())
      .withTransactionInfo("Check #12345")
      .withUserName("Folio, Tester")
      .withNotifyPatron(false)
      .withComments("STAFF : staff comment \\n PATRON : patron comment");
  }

  private static String createRequestJson(String amount) {
    return toJson(createRequest(amount));
  }

  private static String toJson(Object object) {
    return JsonObject.mapFrom(object).encodePrettily();
  }

  private void verifyThatEventWasSent(EventType eventType, JsonObject eventPayload) {
    Event event = new Event()
      .withEventType(eventType.name())
      .withEventPayload(eventPayload.encode())
      .withEventMetadata(new EventMetadata()
        .withPublishedBy(PomUtils.getModuleId())
        .withTenantId(TENANT_NAME)
        .withEventTTL(1));

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> getOkapi().verify(postRequestedFor(urlPathEqualTo("/pubsub/publish"))
        .withRequestBody(equalToJson(toJson(event), true, true))
      ));
  }
}
