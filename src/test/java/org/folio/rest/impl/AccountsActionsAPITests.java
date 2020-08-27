package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.utils.ResourceClients.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.EventType;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.ActionFailureResponse;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.utils.ResourceClient;
import org.folio.rest.utils.ResourceClients;
import org.folio.test.support.ApiTests;
import org.folio.util.pubsub.PubSubClientUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

@RunWith(value = Parameterized.class)
public class AccountsActionsAPITests extends ApiTests {
  private static final String ACCOUNT_ID = randomId();
  private final ResourceClient actionsClient = feeFineActionsClient();
  private final Action action;
  private ResourceClient resourceClient;

  public AccountsActionsAPITests(Action action) {
    this.action = action;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Object[] parameters() {
    return new Object[] { PAY, WAIVE, TRANSFER };
  }

  @Before
  public void beforeEach() {
    removeAllFromTable("feefineactions");
    removeAllFromTable("accounts");
    resourceClient = getClient();
  }

  private ResourceClient getClient() {
    switch (action) {
    case PAY:
      return accountsPayClient(ACCOUNT_ID);
    case WAIVE:
      return accountsWaiveClient(ACCOUNT_ID);
    case TRANSFER:
      return ResourceClients.accountsTransferClient(ACCOUNT_ID);
    default:
      throw new IllegalArgumentException("Failed to get ResourceClient for action: " + action.name());
    }
  }

  @Test
  public void return404WhenAccountDoesNotExist() {
    resourceClient.post(createRequestJson(String.valueOf(10.0)))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(equalTo("Fee/fine was not found"));
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
    postAccount(createAccount(1.0));

    String amountString = String.valueOf(amount);

    ActionFailureResponse expectedResponse = new ActionFailureResponse()
      .withAmount(amountString)
      .withAccountId(ACCOUNT_ID)
      .withErrorMessage("Amount must be positive");

    resourceClient.post(createRequestJson(amountString))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(equalTo(toJson(expectedResponse)));
  }

  @Test
  public void return422WhenRequestedAmountIsInvalidString() {
    postAccount(createAccount(1.0));

    String invalidAmount = "eleven";

    ActionFailureResponse expectedResponse = new ActionFailureResponse()
      .withAmount(invalidAmount)
      .withAccountId(ACCOUNT_ID)
      .withErrorMessage("Invalid amount entered");

    resourceClient.post(createRequestJson(invalidAmount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(equalTo(toJson(expectedResponse)));
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
    Account account = createAccount(remainingAmount + 1).withRemaining(remainingAmount);
    account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
    postAccount(account);

    String requestedAmount = "1.23";

    ActionFailureResponse expectedResponse = new ActionFailureResponse()
      .withAmount(requestedAmount)
      .withAccountId(ACCOUNT_ID)
      .withErrorMessage("Fee/fine is already closed");

    resourceClient.post(createRequestJson(requestedAmount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(equalTo(toJson(expectedResponse)));
  }

  @Test
  public void longDecimalsAreHandledCorrectlyAndAccountIsClosed() {
    double accountBalanceBeforeAction = 1.004987654321;
    final Account account = createAccount(accountBalanceBeforeAction);
    postAccount(account);

    String requestedAmountString = "1.004123456789";
    String expectedPaymentStatus = action.getFullResult();

    final ActionRequest request = createRequest(requestedAmountString);

    final String feeFineActionId  = resourceClient.post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body("amount", is("1.00"))
      .body("accountId", is(ACCOUNT_ID))
      .extract()
      .path("feeFineActionId");

    actionsClient.getById(feeFineActionId)
      .then()
      .body("amountAction", is(1.0f))
      .body("balance", is(0.0f))
      .body("typeAction", is(expectedPaymentStatus));

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .body("remaining", is(0.0f))
      .body("status.name", is("Closed"))
      .body("paymentStatus.name", is(expectedPaymentStatus));
  }

  @Test
  public void longDecimalsAreHandledCorrectly() {
    double accountBalanceBeforeAction = 1.23987654321; // should be rounded to 1.24
    final Account account = createAccount(accountBalanceBeforeAction);
    postAccount(account);

    String requestedAmountString = "1.004987654321"; // should be rounded to 1.00
    String expectedPaymentStatus = action.getPartialResult();

    final ActionRequest request = createRequest(requestedAmountString);

    final String feeFineActionId  = resourceClient.post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body("amount", is("1.00"))
      .body("accountId", is(ACCOUNT_ID))
      .extract()
      .path("feeFineActionId");

    actionsClient.getById(feeFineActionId)
      .then()
      .body("amountAction", is(1.00f))
      .body("balance", is(0.24f)) // 1.24 - 1.00
      .body("typeAction", is(expectedPaymentStatus));

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .body("remaining", is(0.24f))
      .body("status.name", is("Open"))
      .body("paymentStatus.name", is(expectedPaymentStatus));
  }

  @Test
  public void partialActionCreatesActionAndUpdatesAccount() {
    paymentCreatesActionAndUpdatesAccount(false);
  }

  @Test
  public void fullActionCreatesActionAndClosesAccount() {
    paymentCreatesActionAndUpdatesAccount(true);
  }

  private void paymentCreatesActionAndUpdatesAccount(boolean terminalAction) {
    double accountBalanceBefore = 3.45;
    double requestedAmount = terminalAction ? accountBalanceBefore : accountBalanceBefore - 1.0;
    double expectedAccountBalanceAfter = accountBalanceBefore - requestedAmount;

    final Account account = createAccount(accountBalanceBefore);
    postAccount(account);

    String expectedPaymentStatus = terminalAction ? action.getFullResult() : action.getPartialResult();
    String expectedAccountStatus = terminalAction ? "Closed" : "Open";
    String requestedAmountString = String.valueOf(requestedAmount);

    final ActionRequest request = createRequest(requestedAmountString);

    String feeFineActionId = resourceClient.post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body("amount", is(requestedAmountString))
      .body("accountId", is(ACCOUNT_ID))
      .extract()
      .path("feeFineActionId");

    actionsClient.getById(feeFineActionId)
      .then()
      .body("typeAction", is(expectedPaymentStatus))
      .body("comments", is(request.getComments()))
      .body("notify", is(request.getNotifyPatron()))
      .body("amountAction", is((float) requestedAmount))
      .body("balance", is((float) expectedAccountBalanceAfter))
      .body("transactionInformation", is(request.getTransactionInfo()))
      .body("createdAt", is(request.getServicePointId()))
      .body("source", is(request.getUserName()))
      .body("paymentMethod", is(request.getPaymentMethod()))
      .body("accountId", is(ACCOUNT_ID))
      .body("userId", is(account.getUserId()))
      .body("id", is(feeFineActionId))
      .body("dateAction", notNullValue(String.class));

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .body("remaining", is((float) expectedAccountBalanceAfter))
      .body("status.name", is(expectedAccountStatus))
      .body("paymentStatus.name", is(expectedPaymentStatus));

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
  }

  private Account createAccount(double amount) {
    return new Account()
      .withId(ACCOUNT_ID)
      .withOwnerId(randomId())
      .withUserId(randomId())
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

  private static ActionRequest createRequest(String amount) {
    return new ActionRequest()
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
        .withPublishedBy(PubSubClientUtils.constructModuleName())
        .withTenantId(TENANT_NAME)
        .withEventTTL(1));

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> getOkapi().verify(postRequestedFor(urlPathEqualTo("/pubsub/publish"))
        .withRequestBody(equalToJson(toJson(event), true, true))
      ));
  }
}
