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
import static org.folio.rest.utils.ResourceClients.accountsRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountWaiveClient;
import static org.folio.rest.utils.ResourceClients.feeFineActionsClient;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.ActionRequestBak;
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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.json.JsonObject;

@RunWith(value = Parameterized.class)
public class AccountsBulkRefundAPITests extends ApiTests {
//  private static final String ACCOUNT_ID = randomId();
//  private static final String SECOND_ACCOUNT_ID = randomId();
//  private static final String USER_ID = randomId();
//  private static final String FEE_FINE_ACTIONS = "feefineactions";
//  private static final String ERROR_MESSAGE =
//    "Refund amount must be greater than zero and less than or equal to Selected amount";
//
//  private static final String REFUND_TO_PATRON = "Refund to patron";
//  private static final String REFUND_TO_BURSAR = "Refund to Bursar";
//  private static final String REFUNDED_TO_PATRON = "Refunded to patron";
//  private static final String REFUNDED_TO_BURSAR = "Refunded to Bursar";
//
//  private final ResourceClient actionsClient = feeFineActionsClient();
//  private final ResourceClient payClient = buildAccountPayClient(ACCOUNT_ID);
//  private final ResourceClient waiveClient = buildAccountWaiveClient(ACCOUNT_ID);
//  private final ResourceClient transferClient = buildAccountTransferClient(ACCOUNT_ID);
//  private final ResourceClient refundClient = accountsRefundClient(ACCOUNT_ID);
//  private final ResourceClient bulkRefundClient = buildAccountBulkRefundClient();
//
//  private final Mode mode;
//
//  public AccountsBulkRefundAPITests(Mode mode) {
//    this.mode = mode;
//  }
//
//  @Parameterized.Parameters(name = "{0}")
//  public static Object[] parameters() {
//    return new Object[] { Mode.SINGLE, Mode.BULK };
//  }
//
//  @Before
//  public void beforeEach() {
//    removeAllFromTable(FEE_FINE_ACTIONS);
//    removeAllFromTable("accounts");
//  }
//
//  @Test
//  public void fullRefundOfClosedAccountWithPayment() {
//    double initialAmount = 6.0;
//    double payAmount = 5.0;
//    double transferAmount = 0.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 5.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//     List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-5.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON, request),
//      feeFineActionMatcher(0.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void fullRefundOfClosedAccountWithTransfer() {
//    double initialAmount = 6.0;
//    double payAmount = 0.0;
//    double transferAmount = 5.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 5.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-5.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR, request),
//      feeFineActionMatcher(0.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void fullRefundOfClosedAccountWithPaymentAndTransfer() {
//    double initialAmount = 6.0;
//    double payAmount = 3.0;
//    double transferAmount = 2.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 5.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-3.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON, request),
//      feeFineActionMatcher(-5.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR, request),
//      feeFineActionMatcher(-2.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON, request),
//      feeFineActionMatcher( 0.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void fullRefundOfOpenAccountWithPayment() {
//    double initialAmount = 6.0;
//    double payAmount = 3.0;
//    double transferAmount = 0.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 3.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-1.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON, request),
//      feeFineActionMatcher(2.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void fullRefundOfOpenAccountWithTransfer() {
//    double initialAmount = 6.0;
//    double payAmount = 0.0;
//    double transferAmount = 3.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 3.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-1.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR, request),
//      feeFineActionMatcher(2.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void fullRefundOfOpenAccountWithPaymentAndTransfer() {
//    double initialAmount = 7.0;
//    double payAmount = 3.0;
//    double transferAmount = 2.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 5.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-2.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON, request),
//      feeFineActionMatcher(-4.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR, request),
//      feeFineActionMatcher(-1.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON, request),
//      feeFineActionMatcher(1.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void partialRefundOfClosedAccountWithPayment() {
//    double initialAmount = 6.0;
//    double payAmount = 5.0;
//    double transferAmount = 0.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 3.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-3.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON, request),
//      feeFineActionMatcher(0.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void partialRefundOfClosedAccountWithTransfer() {
//    double initialAmount = 6.0;
//    double payAmount = 0.0;
//    double transferAmount = 5.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 3.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-3.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR, request),
//      feeFineActionMatcher(0.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void partialRefundOfClosedAccountWithPaymentAndTransfer() {
//    double initialAmount = 6.0;
//    double payAmount = 3.0;
//    double transferAmount = 2.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 4.0;
//
//    double transferRefundAmount = refundAmount - payAmount;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-3.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON, request),
//      feeFineActionMatcher(-4.0, transferRefundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR, request),
//      feeFineActionMatcher(-1.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON, request),
//      feeFineActionMatcher( 0.0, transferRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void partialRefundOfOpenAccountWithPayment() {
//    double initialAmount = 6.0;
//    double payAmount = 3.0;
//    double transferAmount = 0.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 2.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(0.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON, request),
//      feeFineActionMatcher(2.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void partialRefundOfOpenAccountWithTransfer() {
//    double initialAmount = 6.0;
//    double payAmount = 0.0;
//    double transferAmount = 3.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 2.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(0.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR, request),
//      feeFineActionMatcher(2.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void partialRefundOfOpenAccountWithPaymentAndTransfer() {
//    double initialAmount = 7.0;
//    double payAmount = 3.0;
//    double transferAmount = 2.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 4.0;
//
//    double transferRefundAmount = refundAmount - payAmount;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
//      feeFineActionMatcher(-2.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON, request),
//      feeFineActionMatcher(-3.0, transferRefundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR, request),
//      feeFineActionMatcher(0.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON, request),
//      feeFineActionMatcher(1.0, transferRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR, request)
//    );
//
//    testRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
//  }
//
//  @Test
//  public void refundFailsWhenRequestedAmountExceedsRefundableAmount() {
//    double initialAmount = 7.0;
//    double payAmount = 3.0;
//    double transferAmount = 2.0;
//    double waiveAmount = 1.0;
//    double refundAmount = 6.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    testRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
//  }
//
//  @Test
//  public void refundFailsWhenThereAreNoRefundableActionsForAccount() {
//    double initialAmount = 7.0;
//    double payAmount = 0.0;
//    double transferAmount = 0.0;
//    double waiveAmount = 5.0;
//    double refundAmount = 4.0;
//
//    ActionRequestBak request = createRequest(refundAmount);
//
//    testRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
//      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
//  }
//
//  @Test
//  public void return404WhenAccountDoesNotExist() {
//    refundClient.post(toJson(createRequest(10.0)))
//      .then()
//      .statusCode(HttpStatus.SC_NOT_FOUND)
//      .contentType(ContentType.TEXT)
//      .body(equalTo(format("Fee/fine ID %s not found", ACCOUNT_ID)));
//  }
//
//  @Test
//  public void return422WhenRequestedAmountIsNegative() {
//    testRefundFailure(10, 0, 0, 0, createRequest(-1.0)
//      , SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
//  }
//
//  @Test
//  public void return422WhenRequestedAmountIsZero() {
//    testRefundFailure(10, 0, 0, 0, createRequest(0.0),
//      SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
//  }
//
//  @Test
//  public void return422WhenRequestedAmountIsInvalidString() {
//    DefaultActionRequest request = createRequest(0.0).withAmount("eleven");
//    testRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Invalid amount entered");
//  }
//
//  private void testRefundSuccess(double initialAmount, double payAmount, double transferAmount,
//    double waiveAmount, ActionRequestBak request, FeeFineStatus expectedStatus,
//    String expectedPaymentStatus, List<Matcher<JsonObject>> expectedFeeFineActions) {
//
//    int expectedActionCount = prepareAccountAndActions(
//      initialAmount, payAmount, transferAmount, waiveAmount);
//
//    refundClient.post(request)
//      .then()
//      .statusCode(SC_CREATED)
//      .body("accountId", is(ACCOUNT_ID))
//      .body("amount", is(request.getAmount()));
//
//    expectedActionCount += expectedFeeFineActions.size();
//
//    ValidatableResponse getAllFeeFineActionsResponse = actionsClient.getAll()
//      .then()
//      .body(FEE_FINE_ACTIONS, hasSize(expectedActionCount));
//
//    for (Matcher<JsonObject> feeFineAction : expectedFeeFineActions) {
//      getAllFeeFineActionsResponse.body(FEE_FINE_ACTIONS, hasItem(feeFineAction));
//    }
//
//    double expectedRemainingAmount = initialAmount - payAmount - transferAmount - waiveAmount;
//
//    Account accountAfterRefund = verifyAccountAndGet(
//      expectedRemainingAmount, expectedStatus, expectedPaymentStatus);
//
//    verifyThatFeeFineBalanceChangedEventWasSent(accountAfterRefund);
//  }
//
//  private void testRefundFailure(double initialAmount, double payAmount, double transferAmount,
//    double waiveAmount, ActionRequestBak request, int expectedStatus, String errorMessage) {
//
//    int expectedActionCount = prepareAccountAndActions(
//      initialAmount, payAmount, transferAmount, waiveAmount);
//
//    refundClient.post(request)
//      .then()
//      .statusCode(expectedStatus)
//      .body("accountId", is(ACCOUNT_ID))
//      .body("amount", is(request.getAmount()))
//      .body("errorMessage", is(errorMessage));
//
//    actionsClient.getAll()
//      .then()
//      .body(FEE_FINE_ACTIONS, hasSize(expectedActionCount));
//  }
//
//  private int prepareAccountAndActions(double initialAmount, double payAmount,
//    double transferAmount, double waiveAmount) {
//
//    if (payAmount < 0 || transferAmount < 0 || waiveAmount < 0) {
//      throw new IllegalArgumentException("Amounts can't be negative");
//    }
//
//    postAccount(createAccount(initialAmount, initialAmount));
//
//    int expectedActionCount = 0;
//
//    if (payAmount > 0) {
//      performAction(payClient, payAmount);
//      expectedActionCount++;
//    }
//    if (waiveAmount > 0) {
//      performAction(waiveClient, waiveAmount);
//      expectedActionCount++;
//    }
//    if (transferAmount > 0) {
//      performAction(transferClient, transferAmount);
//      expectedActionCount++;
//    }
//
//    actionsClient.getAll()
//      .then()
//      .body(FEE_FINE_ACTIONS, hasSize(expectedActionCount));
//
//    return expectedActionCount;
//  }
//
//  private void postAccount(Account account) {
//    accountsClient.create(account)
//      .then()
//      .statusCode(SC_CREATED)
//      .contentType(JSON);
//  }
//
//  private ActionRequestBak createRequest(double amount) {
//    switch (mode) {
//    case SINGLE:
//      return new DefaultActionRequest()
//      .withAmount(new MonetaryValue(amount).toString())
//      .withPaymentMethod("Cash")
//      .withServicePointId(randomId())
//      .withUserName("Folio, Tester")
//      .withNotifyPatron(false)
//      .withComments("STAFF : staff comment \\n PATRON : patron comment");
//    case BULK:
//      return new DefaultBulkActionRequest()
//        .withAccountIds(Arrays.asList(ACCOUNT_ID, SECOND_ACCOUNT_ID))
//        .withAmount(new MonetaryValue(amount).toString())
//        .withPaymentMethod("Cash")
//        .withServicePointId(randomId())
//        .withUserName("Folio, Tester")
//        .withNotifyPatron(false)
//        .withComments("STAFF : staff comment \\n PATRON : patron comment");
//    }
//
//    return new DefaultActionRequest()
//      .withAmount(new MonetaryValue(amount).toString())
//      .withPaymentMethod("Cash")
//      .withServicePointId(randomId())
//      .withUserName("Folio, Tester")
//      .withNotifyPatron(false)
//      .withComments("STAFF : staff comment \\n PATRON : patron comment");
//  }
//
//  private static String toJson(Object object) {
//    return JsonObject.mapFrom(object).encodePrettily();
//  }
//
//  private void verifyThatFeeFineBalanceChangedEventWasSent(Account account) {
//    verifyThatEventWasSent(EventType.FEE_FINE_BALANCE_CHANGED, new JsonObject()
//      .put("userId", account.getUserId())
//      .put("feeFineId", account.getId())
//      .put("feeFineTypeId", account.getFeeFineId())
//      .put("balance", account.getRemaining())
//      .put("loanId", account.getLoanId()));
//  }
//
//  private void verifyThatEventWasSent(EventType eventType, JsonObject eventPayload) {
//    Event event = new Event()
//      .withEventType(eventType.name())
//      .withEventPayload(eventPayload.encode())
//      .withEventMetadata(new EventMetadata()
//        .withPublishedBy(PubSubClientUtils.constructModuleName())
//        .withTenantId(TENANT_NAME)
//        .withEventTTL(1));
//
//    Awaitility.await()
//      .atMost(5, TimeUnit.SECONDS)
//      .untilAsserted(() -> getOkapi().verify(postRequestedFor(urlPathEqualTo("/pubsub/publish"))
//        .withRequestBody(equalToJson(toJson(event), true, true))
//      ));
//  }
//
//  private Account createAccount(double amount, double remainingAmount) {
//    return EntityBuilder.buildAccount(amount, remainingAmount)
//      .withId(ACCOUNT_ID)
//      .withUserId(USER_ID);
//  }
//
//  private Account verifyAccountAndGet(double expectedRemainingAmount,
//    FeeFineStatus expectedStatus, String expectedPaymentStatus) {
//
//    final Response getAccountByIdResponse = accountsClient.getById(ACCOUNT_ID);
//
//    getAccountByIdResponse
//      .then()
//      .statusCode(SC_OK)
//      .body("remaining", is((float) expectedRemainingAmount))
//      .body("status.name", is(expectedStatus.getValue()))
//      .body("paymentStatus.name", is(expectedPaymentStatus));
//
//    return getAccountByIdResponse.as(Account.class);
//  }
//
//  private ValidatableResponse performAction(ResourceClient resourceClient, double amount) {
//    return resourceClient.post(createRequest(amount))
//      .then()
//      .statusCode(SC_CREATED);
//  }
//
//  public static Matcher<JsonObject> feeFineActionMatcher(double balance, double amount,
//    String actionType, String transactionInfo, ActionRequestBak request) {
//
//    return FeeFineActionMatchers.feeFineAction(ACCOUNT_ID, USER_ID, balance, amount, actionType,
//      transactionInfo, request);
//  }
//
//  private enum Mode {
//    SINGLE, BULK
//  }
}
