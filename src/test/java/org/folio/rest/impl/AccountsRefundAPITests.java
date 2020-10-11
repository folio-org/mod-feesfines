package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.domain.FeeFineStatus.OPEN;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountsRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountWaiveClient;
import static org.folio.rest.utils.ResourceClients.feeFineActionsClient;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.EventType;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.DefaultActionRequest;
import org.folio.rest.jaxrs.model.DefaultBulkActionRequest;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.folio.test.support.matcher.FeeFineActionMatchers;
import org.folio.util.pubsub.PubSubClientUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.json.JsonObject;

public class AccountsRefundAPITests extends ApiTests {
  private static final String FIRST_ACCOUNT_ID = randomId();
  private static final String SECOND_ACCOUNT_ID = randomId();
  private static final String USER_ID = randomId();
  private static final String SERVICE_POINT_ID = randomId();

  private static final String PAYMENT_METHOD = "Cash";
  private static final String USER_NAME = "Folio, Tester";
  private static final String COMMENTS = "STAFF : staff comment \\n PATRON : patron comment";
  private static final boolean NOTIFY_PATRON = false;

  private static final String FEE_FINE_ACTIONS = "feefineactions";
  private static final String ERROR_MESSAGE =
    "Refund amount must be greater than zero and less than or equal to Selected amount";

  private static final String REFUND_TO_PATRON = "Refund to patron";
  private static final String REFUND_TO_BURSAR = "Refund to Bursar";
  private static final String REFUNDED_TO_PATRON = "Refunded to patron";
  private static final String REFUNDED_TO_BURSAR = "Refunded to Bursar";

  private final ResourceClient actionsClient = feeFineActionsClient();
  private final ResourceClient refundClient = buildAccountsRefundClient(FIRST_ACCOUNT_ID);
  private final ResourceClient bulkRefundClient = buildAccountBulkRefundClient();

  @Before
  public void beforeEach() {
    removeAllFromTable(FEE_FINE_ACTIONS);
    removeAllFromTable("accounts");
  }

  @Test
  public void fullRefundOfClosedAccountWithPayment() {
    double initialAmount = 6.0;
    double payAmount = 5.0;
    double transferAmount = 0.0;
    double waiveAmount = 1.0;
    double refundAmount = 5.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -5.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfClosedAccountWithTransfer() {
    double initialAmount = 6.0;
    double payAmount = 0.0;
    double transferAmount = 5.0;
    double waiveAmount = 1.0;
    double refundAmount = 5.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -5.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfClosedAccountWithPaymentAndTransfer() {
    double initialAmount = 6.0;
    double payAmount = 3.0;
    double transferAmount = 2.0;
    double waiveAmount = 1.0;
    double refundAmount = 5.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -3.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -5.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -2.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID,  0.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfOpenAccountWithPayment() {
    double initialAmount = 6.0;
    double payAmount = 3.0;
    double transferAmount = 0.0;
    double waiveAmount = 1.0;
    double refundAmount = 3.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -1.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 2.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfOpenAccountWithTransfer() {
    double initialAmount = 6.0;
    double payAmount = 0.0;
    double transferAmount = 3.0;
    double waiveAmount = 1.0;
    double refundAmount = 3.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -1.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 2.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfOpenAccountWithPaymentAndTransfer() {
    double initialAmount = 7.0;
    double payAmount = 3.0;
    double transferAmount = 2.0;
    double waiveAmount = 1.0;
    double refundAmount = 5.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -2.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -4.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -1.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 1.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfClosedAccountWithPayment() {
    double initialAmount = 6.0;
    double payAmount = 5.0;
    double transferAmount = 0.0;
    double waiveAmount = 1.0;
    double refundAmount = 3.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -3.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfClosedAccountWithTransfer() {
    double initialAmount = 6.0;
    double payAmount = 0.0;
    double transferAmount = 5.0;
    double waiveAmount = 1.0;
    double refundAmount = 3.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -3.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfClosedAccountWithPaymentAndTransfer() {
    double initialAmount = 6.0;
    double payAmount = 3.0;
    double transferAmount = 2.0;
    double waiveAmount = 1.0;
    double refundAmount = 4.0;

    double transferRefundAmount = refundAmount - payAmount;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -3.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -4.0, transferRefundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -1.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID,  0.0, transferRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfOpenAccountWithPayment() {
    double initialAmount = 6.0;
    double payAmount = 3.0;
    double transferAmount = 0.0;
    double waiveAmount = 1.0;
    double refundAmount = 2.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 2.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfOpenAccountWithTransfer() {
    double initialAmount = 6.0;
    double payAmount = 0.0;
    double transferAmount = 3.0;
    double waiveAmount = 1.0;
    double refundAmount = 2.0;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 2.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfOpenAccountWithPaymentAndTransfer() {
    double initialAmount = 7.0;
    double payAmount = 3.0;
    double transferAmount = 2.0;
    double waiveAmount = 1.0;
    double refundAmount = 4.0;

    double transferRefundAmount = refundAmount - payAmount;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -2.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -3.0, transferRefundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 1.0, transferRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void refundFailsWhenRequestedAmountExceedsRefundableAmount() {
    double initialAmount = 7.0;
    double payAmount = 3.0;
    double transferAmount = 2.0;
    double waiveAmount = 1.0;
    double refundAmount = 6.0;

    DefaultActionRequest request = createRequest(refundAmount);

    testSingleAccountRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
  }

  @Test
  public void refundFailsWhenThereAreNoRefundableActionsForAccount() {
    double initialAmount = 7.0;
    double payAmount = 0.0;
    double transferAmount = 0.0;
    double waiveAmount = 5.0;
    double refundAmount = 4.0;

    DefaultActionRequest request = createRequest(refundAmount);

    testSingleAccountRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
  }

  @Test
  public void return404WhenAccountDoesNotExist() {
    refundClient.post(toJson(createRequest(10.0)))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(equalTo(format("Fee/fine ID %s not found", FIRST_ACCOUNT_ID)));
  }

  @Test
  public void return422WhenRequestedAmountIsNegative() {
    DefaultActionRequest request = createRequest(-1.0);
    testSingleAccountRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
  }

  @Test
  public void return422WhenRequestedAmountIsZero() {
    DefaultActionRequest request = createRequest(0.0);
    testSingleAccountRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
  }

  @Test
  public void return422WhenRequestedAmountIsInvalidString() {
    DefaultActionRequest request = createRequest(0.0).withAmount("eleven");
    testSingleAccountRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Invalid amount entered");
  }

  @Test
  public void bulkRefund() {
    double initialAmount = 6.0;
    double payAmount = 5.0;
    double transferAmount = 0.0;
    double waiveAmount = 1.0;
    double refundAmount = 10.0;

    double expectedRemainingAmount = initialAmount - payAmount - transferAmount - waiveAmount;

    prepareAccount(FIRST_ACCOUNT_ID, initialAmount, payAmount, transferAmount, waiveAmount);
    prepareAccount(SECOND_ACCOUNT_ID, initialAmount, payAmount, transferAmount, waiveAmount);

    Response response = bulkRefundClient.post(createBulkRequest(refundAmount));

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -5.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, -5.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 0.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON)
    );

    verifyBulkResponse(response, refundAmount, expectedFeeFineActions);
    verifyActions(8, expectedFeeFineActions);

    Account firstAccount = verifyAccountAndGet(FIRST_ACCOUNT_ID, expectedRemainingAmount,
      CLOSED, REFUND.getFullResult());

    Account secondAccount = verifyAccountAndGet(SECOND_ACCOUNT_ID, expectedRemainingAmount,
      CLOSED, REFUND.getFullResult());

    verifyThatFeeFineBalanceChangedEventWasSent(firstAccount);
    verifyThatFeeFineBalanceChangedEventWasSent(secondAccount);
  }

  private void testSingleAccountRefundSuccess(double initialAmount, double payAmount, double transferAmount,
    double waiveAmount, double requestedAmount, FeeFineStatus expectedStatus,
    String expectedPaymentStatus, List<Matcher<JsonObject>> expectedFeeFineActions) {

    double expectedRemainingAmount = initialAmount - payAmount - transferAmount - waiveAmount;

    int actionsCountBeforeRefund = prepareAccount(FIRST_ACCOUNT_ID, initialAmount, payAmount,
      transferAmount, waiveAmount);
    int totalExpectedActionsCount = actionsCountBeforeRefund + expectedFeeFineActions.size();

    Response response = refundClient.post(createRequest(requestedAmount));
    verifyResponse(response, requestedAmount);

    verifyActions(totalExpectedActionsCount, expectedFeeFineActions);

    Account accountAfterRefund = verifyAccountAndGet(FIRST_ACCOUNT_ID, expectedRemainingAmount,
      expectedStatus, expectedPaymentStatus);

    verifyThatFeeFineBalanceChangedEventWasSent(accountAfterRefund);
  }

  private void verifyResponse(Response response, double requestedAmount) {
    response.then()
      .statusCode(SC_CREATED)
      .body("accountId", is(FIRST_ACCOUNT_ID))
      .body("amount", is(new MonetaryValue(requestedAmount).toString()));
  }

  private void verifyBulkResponse(Response response, double requestedAmount,
    List<Matcher<JsonObject>> expectedFeeFineActions) {

    response.then()
      .statusCode(SC_CREATED)
      .body("accountIds", hasItems(FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID))
      .body("amount", is(new MonetaryValue(requestedAmount).toString()));

    for (Matcher<JsonObject> feeFineAction : expectedFeeFineActions) {
      response.then().body(FEE_FINE_ACTIONS, hasItem(feeFineAction));
    }
  }

  private void verifyActions(int expectedTotalFeeFineActionsCount,
    List<Matcher<JsonObject>> expectedFeeFineActions) {

    final Response getAllFeeFineActionsResponse = actionsClient.getAll();

    getAllFeeFineActionsResponse.then()
      .body(FEE_FINE_ACTIONS, hasSize(expectedTotalFeeFineActionsCount));

    for (Matcher<JsonObject> feeFineAction : expectedFeeFineActions) {
      getAllFeeFineActionsResponse.then().body(FEE_FINE_ACTIONS, hasItem(feeFineAction));
    }
  }

  private void testSingleAccountRefundFailure(double initialAmount, double payAmount,
    double transferAmount, double waiveAmount, DefaultActionRequest request, int expectedStatus,
    String errorMessage) {

    int expectedActionCount = prepareAccount(FIRST_ACCOUNT_ID,
      initialAmount, payAmount, transferAmount, waiveAmount);

    refundClient.post(request)
      .then()
      .statusCode(expectedStatus)
      .body("accountId", is(FIRST_ACCOUNT_ID))
      .body("amount", is(request.getAmount()))
      .body("errorMessage", is(errorMessage));

    actionsClient.getAll()
      .then()
      .body(FEE_FINE_ACTIONS, hasSize(expectedActionCount));
  }

  private int prepareAccount(String accountId, double initialAmount, double payAmount,
    double transferAmount, double waiveAmount) {

    if (payAmount < 0 || transferAmount < 0 || waiveAmount < 0) {
      throw new IllegalArgumentException("Amounts can't be negative");
    }

    postAccount(createAccount(accountId, initialAmount, initialAmount));

    int performedActionsCount = 0;

    if (payAmount > 0) {
      performAction(buildAccountPayClient(accountId), payAmount);
      performedActionsCount++;
    }
    if (waiveAmount > 0) {
      performAction(buildAccountWaiveClient(accountId), waiveAmount);
      performedActionsCount++;
    }
    if (transferAmount > 0) {
      performAction(buildAccountTransferClient(accountId), transferAmount);
      performedActionsCount++;
    }

    return performedActionsCount;
  }

  private void postAccount(Account account) {
    accountsClient.create(account)
      .then()
      .statusCode(SC_CREATED)
      .contentType(JSON);
  }

  private static DefaultActionRequest createRequest(double amount) {
    return new DefaultActionRequest()
      .withAmount(new MonetaryValue(amount).toString())
      .withPaymentMethod(PAYMENT_METHOD)
      .withServicePointId(SERVICE_POINT_ID)
      .withUserName(USER_NAME)
      .withNotifyPatron(NOTIFY_PATRON)
      .withComments(COMMENTS);
  }

  private static DefaultBulkActionRequest createBulkRequest(double amount) {
    return new DefaultBulkActionRequest()
      .withAccountIds(Arrays.asList(FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID))
      .withAmount(new MonetaryValue(amount).toString())
      .withPaymentMethod(PAYMENT_METHOD)
      .withServicePointId(SERVICE_POINT_ID)
      .withUserName(USER_NAME)
      .withNotifyPatron(NOTIFY_PATRON)
      .withComments(COMMENTS);
  }

  private static String toJson(Object object) {
    return JsonObject.mapFrom(object).encodePrettily();
  }

  private void verifyThatFeeFineBalanceChangedEventWasSent(Account account) {
    verifyThatEventWasSent(EventType.FEE_FINE_BALANCE_CHANGED, new JsonObject()
      .put("userId", account.getUserId())
      .put("feeFineId", account.getId())
      .put("feeFineTypeId", account.getFeeFineId())
      .put("balance", account.getRemaining())
      .put("loanId", account.getLoanId()));
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

  private Account createAccount(String accountId, double amount, double remainingAmount) {
    return EntityBuilder.buildAccount(amount, remainingAmount)
      .withId(accountId)
      .withUserId(USER_ID);
  }

  private Account verifyAccountAndGet(String accountId, double expectedRemainingAmount,
    FeeFineStatus expectedStatus, String expectedPaymentStatus) {

    final Response getAccountByIdResponse = accountsClient.getById(accountId);

    getAccountByIdResponse
      .then()
      .statusCode(SC_OK)
      .body("remaining", is((float) expectedRemainingAmount))
      .body("status.name", is(expectedStatus.getValue()))
      .body("paymentStatus.name", is(expectedPaymentStatus));

    return getAccountByIdResponse.as(Account.class);
  }

  private ValidatableResponse performAction(ResourceClient resourceClient, double amount) {
    return resourceClient.post(createRequest(amount))
      .then()
      .statusCode(SC_CREATED);
  }

  public static Matcher<JsonObject> feeFineActionMatcher(String accountId, double balance,
    double amount, String actionType, String transactionInfo) {

    return FeeFineActionMatchers.feeFineAction(accountId, USER_ID, balance, amount, actionType,
      transactionInfo, USER_NAME, COMMENTS, NOTIFY_PATRON, SERVICE_POINT_ID, PAYMENT_METHOD);
  }

}
