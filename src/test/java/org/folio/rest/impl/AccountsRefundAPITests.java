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
import static org.folio.rest.utils.LogEventUtils.fetchLogEventPayloads;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountsRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountWaiveClient;
import static org.folio.rest.utils.ResourceClients.feeFineActionsClient;
import static org.folio.test.support.matcher.LogEventMatcher.notCreditOrRefundActionLogEventPayload;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;

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
  private static final String THIRD_ACCOUNT_ID = randomId();
  private static final String[] TWO_ACCOUNT_IDS = {FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID};

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

  private final MonetaryValue[] BALANCE_NEGATIVE = {
        new MonetaryValue(0.0),
        new MonetaryValue(-1.0),
        new MonetaryValue(-2.0),
        new MonetaryValue(-3.0),
        new MonetaryValue(-4.0),
        new MonetaryValue(-5.0),
        new MonetaryValue(-6.0)
      };

  private final MonetaryValue[] BALANCE_POSITIVE = {
        new MonetaryValue(0.0),
        new MonetaryValue(1.0),
        new MonetaryValue(2.0),
        new MonetaryValue(3.0),
        new MonetaryValue(4.0),
        new MonetaryValue(5.0),
        new MonetaryValue(6.0),
        new MonetaryValue(7.0),
  };

  @Before
  public void beforeEach() {
    removeAllFromTable(FEE_FINE_ACTIONS);
    removeAllFromTable("accounts");
  }

  @Test
  public void fullRefundOfClosedAccountWithPayment() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(5.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(5.0);

    MonetaryValue amountMinus5 = new MonetaryValue(-5.0);
    MonetaryValue amount0 = new MonetaryValue(0.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, refundAmount, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, amount0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
  }


  @Test
  public void fullRefundOfClosedAccountWithTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(5.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(5.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[5], transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[0], transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfClosedAccountWithPaymentAndTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(2.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(5.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[3], payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[5], transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[2], payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[0], transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfOpenAccountWithPayment() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(3.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[1], payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[2], payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfOpenAccountWithTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(3.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(3.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[1], transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[2], transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfOpenAccountWithPaymentAndTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(7.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(2.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(5.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[2], payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[4], transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[1], payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[1], transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getFullResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfClosedAccountWithPayment() {

    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(5.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(3.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[3], refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[0], refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfClosedAccountWithTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(5.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(3.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[3], refundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[0], refundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfClosedAccountWithPaymentAndTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(2.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(4.0);

    MonetaryValue transferRefundAmount = refundAmount.subtract(payAmount);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[3], payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[4], transferRefundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[1], payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[0], transferRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfOpenAccountWithPayment() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(2.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[0], refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[2], refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfOpenAccountWithTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(3.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(2.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[0], refundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[2], refundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void partialRefundOfOpenAccountWithPaymentAndTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(7.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(2.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(4.0);

    MonetaryValue transferRefundAmount = refundAmount.subtract(payAmount);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[2], payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[3], transferRefundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[0], payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_POSITIVE[1], transferRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);
  }

  @Test
  public void refundFailsWhenRequestedAmountExceedsRefundableAmount() {
    MonetaryValue initialAmount = new MonetaryValue(7.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(2.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(6.0);
    DefaultActionRequest request = createRequest(refundAmount);

    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
  }

  @Test
  public void refundFailsWhenThereAreNoRefundableActionsForAccount() {


    MonetaryValue initialAmount = new MonetaryValue(7.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(5.0);
    MonetaryValue refundAmount = new MonetaryValue(4.0);


    DefaultActionRequest request = createRequest(refundAmount);

    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
  }

  @Test
  public void return404WhenAccountDoesNotExist() {
    refundClient.post(toJson(createRequest(new MonetaryValue(10.0))))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(equalTo(format("Fee/fine ID %s not found", FIRST_ACCOUNT_ID)));
  }

  @Test
  public void return422WhenRequestedAmountIsNegative() {
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(0.0);
    MonetaryValue amount = new MonetaryValue(-1.0);

    DefaultActionRequest request = createRequest(amount);
    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
  }

  @Test
  public void return422WhenRequestedAmountIsZero() {
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(0.0);
    MonetaryValue amount = new MonetaryValue(0.0);


    DefaultActionRequest request = createRequest(amount);
    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
  }

  @Test
  public void return422WhenRequestedAmountIsInvalidString() {
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(0.0);
    MonetaryValue amount = new MonetaryValue(0.0);

    DefaultActionRequest request = createRequest(new MonetaryValue(0.0)).withAmount("eleven");
    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request, SC_UNPROCESSABLE_ENTITY, "Invalid amount entered");
  }

  @Test
  public void bulkRefundAmountIsDistributedBetweenAccountsEvenlyRecursively() {
    final MonetaryValue refundAmount = new MonetaryValue(15.0); // even split is 15/3=5 per account

    // Closed, served third, full payment refund, partial transfer refund

    MonetaryValue initialAmount1 = new MonetaryValue(10.0);
    MonetaryValue payAmount1 = new MonetaryValue(5.0);
    MonetaryValue transferAmount1 = new MonetaryValue(5.0);
    MonetaryValue  waiveAmount1  = new MonetaryValue(0.0);

    MonetaryValue expectedRemainingAmount1 = initialAmount1.subtract(payAmount1).subtract(transferAmount1).subtract(waiveAmount1);
    prepareAccount(FIRST_ACCOUNT_ID, initialAmount1, payAmount1, transferAmount1, waiveAmount1);

    // Open, served first, full payment refund, full transfer refund
    MonetaryValue initialAmount2 = new MonetaryValue(10.0);
    MonetaryValue payAmount2 = new MonetaryValue(2.0);
    MonetaryValue transferAmount2 = new MonetaryValue(1.0);
    MonetaryValue waiveAmount2 = new MonetaryValue(0.0);
    MonetaryValue expectedRemainingAmount2 = initialAmount2.subtract(payAmount2).subtract(transferAmount2).subtract(waiveAmount2);
    prepareAccount(SECOND_ACCOUNT_ID, initialAmount2, payAmount2, transferAmount2, waiveAmount2);

    // Open, served second, full payment refund, partial transfer refund
    MonetaryValue initialAmount3 = new MonetaryValue(10.0);
    MonetaryValue payAmount3 = new MonetaryValue(4.0);
    MonetaryValue transferAmount3 = new  MonetaryValue(3.0);
    MonetaryValue waiveAmount3 = new MonetaryValue(0.0);
    MonetaryValue expectedRemainingAmount3 = initialAmount3.subtract(payAmount3).subtract(transferAmount3).subtract(waiveAmount3);

    MonetaryValue amount1 = new MonetaryValue(1.0);
    MonetaryValue amount2 = new MonetaryValue(2.0);

    prepareAccount(THIRD_ACCOUNT_ID, initialAmount3, payAmount3, transferAmount3, waiveAmount3);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[5], payAmount1, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[6], amount1, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, BALANCE_NEGATIVE[1], payAmount1, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID,  BALANCE_POSITIVE[0], amount1, REFUND.getPartialResult(), REFUNDED_TO_BURSAR),

      feeFineActionMatcher(SECOND_ACCOUNT_ID,  BALANCE_POSITIVE[5], payAmount2, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(SECOND_ACCOUNT_ID,  BALANCE_POSITIVE[4], transferAmount2, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(SECOND_ACCOUNT_ID,  BALANCE_POSITIVE[6], payAmount2, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(SECOND_ACCOUNT_ID,  BALANCE_POSITIVE[7], transferAmount2, REFUND.getFullResult(), REFUNDED_TO_BURSAR),

      feeFineActionMatcher(THIRD_ACCOUNT_ID, BALANCE_NEGATIVE[1], payAmount3, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, BALANCE_NEGATIVE[3], amount2, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, BALANCE_POSITIVE[1], payAmount3, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, BALANCE_POSITIVE[3], amount2, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    Response response = bulkRefundClient.post(
      createBulkRequest(refundAmount, FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID, THIRD_ACCOUNT_ID));

    verifyBulkResponse(response, refundAmount, expectedFeeFineActions);
    verifyActions(18, expectedFeeFineActions); // 6 payments/transfer actions + 12 refund actions

    Account firstAccount = verifyAccountAndGet(
      FIRST_ACCOUNT_ID, expectedRemainingAmount1, CLOSED, REFUND.getPartialResult());

    Account secondAccount = verifyAccountAndGet(
      SECOND_ACCOUNT_ID, expectedRemainingAmount2, OPEN, REFUND.getFullResult());

    Account thirdAccount = verifyAccountAndGet(
      THIRD_ACCOUNT_ID, expectedRemainingAmount3, OPEN, REFUND.getPartialResult());

    verifyThatFeeFineBalanceChangedEventsWereSent(firstAccount, secondAccount, thirdAccount);

    fetchLogEventPayloads(getOkapi()).forEach(payload ->
      assertThat(payload, is(notCreditOrRefundActionLogEventPayload())));
  }

  @Test
  public void bulkRefundFailsWhenRequestedAmountExceedsRefundableAmount() {
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(2.0);
    MonetaryValue transferAmount = new MonetaryValue(3.0);
    MonetaryValue  waiveAmount  = new MonetaryValue(4.0);

    MonetaryValue amount = new MonetaryValue(11.0);

    DefaultBulkActionRequest request = createBulkRequest(amount, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request, SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE,
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkRefundFailsWhenThereAreNoRefundableActionsForAccount() {
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue  waiveAmount  = new MonetaryValue(4.0);

    MonetaryValue amount = new MonetaryValue(5.0);

    DefaultBulkActionRequest request = createBulkRequest(amount, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request, SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE,
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkReturn404WhenAccountDoesNotExist() {
    String errorMessageTemplate = "Fee/fine ID %s not found";
    MonetaryValue request = new MonetaryValue(10.0);
    bulkRefundClient.post(toJson(createBulkRequest(request, TWO_ACCOUNT_IDS)))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(isOneOf(
        format(errorMessageTemplate, FIRST_ACCOUNT_ID),
        format(errorMessageTemplate, SECOND_ACCOUNT_ID))
      );
  }

  @Test
  public void bulkReturn422WhenRequestedAmountIsNegative() {
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue  waiveAmount  = new MonetaryValue(0.0);

    MonetaryValue amount = new MonetaryValue(-1.0);

    DefaultBulkActionRequest request = createBulkRequest(amount, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive",
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkReturn422WhenRequestedAmountIsZero() {

    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue  waiveAmount  = new MonetaryValue(0.0);

    MonetaryValue amount = new MonetaryValue(0.0);

    DefaultBulkActionRequest request = createBulkRequest(amount, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive",
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkReturn422WhenRequestedAmountIsInvalidString() {

    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue  waiveAmount  = new MonetaryValue(0.0);

    MonetaryValue amount = new MonetaryValue(0.0);

    DefaultBulkActionRequest request = createBulkRequest(amount, TWO_ACCOUNT_IDS).withAmount("eleven");
    testBulkRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request, SC_UNPROCESSABLE_ENTITY, "Invalid amount entered",
      TWO_ACCOUNT_IDS);
  }

  private void testSingleAccountRefundSuccess(MonetaryValue initialAmount, MonetaryValue payAmount, MonetaryValue transferAmount,
    MonetaryValue waiveAmount, MonetaryValue requestedAmount, FeeFineStatus expectedStatus,
    String expectedPaymentStatus, List<Matcher<JsonObject>> expectedFeeFineActions) {

    MonetaryValue expectedRemainingAmount = initialAmount.subtract(payAmount).subtract(transferAmount).subtract(waiveAmount);

    int actionsCountBeforeRefund = prepareAccount(FIRST_ACCOUNT_ID, initialAmount, payAmount,
      transferAmount, waiveAmount);
    int totalExpectedActionsCount = actionsCountBeforeRefund + expectedFeeFineActions.size();

    Response response = refundClient.post(createRequest(requestedAmount));
    verifyResponse(response, requestedAmount);

    verifyActions(totalExpectedActionsCount, expectedFeeFineActions);

    Account accountAfterRefund = verifyAccountAndGet(FIRST_ACCOUNT_ID, expectedRemainingAmount,
      expectedStatus, expectedPaymentStatus);

    verifyThatFeeFineBalanceChangedEventsWereSent(accountAfterRefund);

    fetchLogEventPayloads(getOkapi()).forEach(payload ->
      assertThat(payload, is(notCreditOrRefundActionLogEventPayload())));
  }

  private void verifyResponse(Response response, MonetaryValue requestedAmount) {
    response.then()
      .statusCode(SC_CREATED)
      .body("accountId", is(FIRST_ACCOUNT_ID))
      .body("amount", is(requestedAmount.toString()));
  }

  private void verifyBulkResponse(Response response, MonetaryValue requestedAmount,
    List<Matcher<JsonObject>> expectedFeeFineActions, String... accountIds) {

    response.then()
      .statusCode(SC_CREATED)
      .body("accountIds", hasItems(accountIds))
      .body("amount", is(requestedAmount.toString()));

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

  private void testSingleRefundFailure(MonetaryValue initialAmount, MonetaryValue payAmount,
    MonetaryValue transferAmount, MonetaryValue waiveAmount, DefaultActionRequest request, int expectedStatus,
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

  private void testBulkRefundFailure(MonetaryValue initialAmount, MonetaryValue payAmount, MonetaryValue transferAmount,
    MonetaryValue waiveAmount, DefaultBulkActionRequest request, int expectedStatus, String errorMessage,
    String... accountIds) {

    int expectedActionsCount = 0;

    for (String accountId : accountIds) {
      expectedActionsCount += prepareAccount(
        accountId, initialAmount, payAmount, transferAmount, waiveAmount);
    }

    bulkRefundClient.post(request)
      .then()
      .statusCode(expectedStatus)
      .body("accountIds", hasItems(accountIds))
      .body("amount", is(request.getAmount()))
      .body("errorMessage", is(errorMessage));

    actionsClient.getAll()
      .then()
      .body(FEE_FINE_ACTIONS, hasSize(expectedActionsCount));
  }

  private int prepareAccount(String accountId, MonetaryValue initialAmount, MonetaryValue payAmount,
    MonetaryValue transferAmount, MonetaryValue waiveAmount) {

    if (payAmount.isNegative() || transferAmount.isNegative() || waiveAmount.isNegative()) {
      throw new IllegalArgumentException("Amounts can't be negative");
    }

    postAccount(createAccount(accountId, initialAmount, initialAmount));

    int performedActionsCount = 0;

    if (payAmount.isPositive()) {
      performAction(buildAccountPayClient(accountId), payAmount);
      performedActionsCount++;
    }
    if (waiveAmount.isPositive()) {
      performAction(buildAccountWaiveClient(accountId), waiveAmount);
      performedActionsCount++;
    }
    if (transferAmount.isPositive()) {
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

  private static DefaultActionRequest createRequest(MonetaryValue amount) {
    return new DefaultActionRequest()
      .withAmount(amount.toString())
      .withPaymentMethod(PAYMENT_METHOD)
      .withServicePointId(SERVICE_POINT_ID)
      .withUserName(USER_NAME)
      .withNotifyPatron(NOTIFY_PATRON)
      .withComments(COMMENTS);
  }

  private static DefaultBulkActionRequest createBulkRequest(MonetaryValue amount, String... accountIds) {
    return new DefaultBulkActionRequest()
      .withAccountIds(Arrays.asList(accountIds))
      .withAmount(amount.toString())
      .withPaymentMethod(PAYMENT_METHOD)
      .withServicePointId(SERVICE_POINT_ID)
      .withUserName(USER_NAME)
      .withNotifyPatron(NOTIFY_PATRON)
      .withComments(COMMENTS);
  }

  private static String toJson(Object object) {
    return JsonObject.mapFrom(object).encodePrettily();
  }

  private void verifyThatFeeFineBalanceChangedEventsWereSent(Account... accounts) {
    for (Account account : accounts) {
      verifyThatEventWasSent(EventType.FEE_FINE_BALANCE_CHANGED, new JsonObject()
        .put("userId", account.getUserId())
        .put("feeFineId", account.getId())
        .put("feeFineTypeId", account.getFeeFineId())
        .put("balance", account.getRemaining())
        .put("loanId", account.getLoanId()));
    }
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

  private static Account createAccount(String accountId, MonetaryValue amount, MonetaryValue remainingAmount) {
    return EntityBuilder.buildAccount(amount, remainingAmount)
      .withId(accountId)
      .withUserId(USER_ID);
  }

  private Account verifyAccountAndGet(String accountId, MonetaryValue expectedRemainingAmount,
    FeeFineStatus expectedStatus, String expectedPaymentStatus) {

    final Response getAccountByIdResponse = accountsClient.getById(accountId);

    getAccountByIdResponse
      .then()
      .statusCode(SC_OK)
      .body("remaining", is((float) expectedRemainingAmount.toDouble()))
      .body("status.name", is(expectedStatus.getValue()))
      .body("paymentStatus.name", is(expectedPaymentStatus));

    return getAccountByIdResponse.as(Account.class);
  }

  private ValidatableResponse performAction(ResourceClient resourceClient, MonetaryValue amount) {
    return resourceClient.post(createRequest(amount))
      .then()
      .statusCode(SC_CREATED);
  }

  public static Matcher<JsonObject> feeFineActionMatcher(String accountId, MonetaryValue balance,
    MonetaryValue amount, String actionType, String transactionInfo) {

    return FeeFineActionMatchers.feeFineAction(accountId, USER_ID, balance, amount, actionType,
      transactionInfo, USER_NAME, COMMENTS, NOTIFY_PATRON, SERVICE_POINT_ID, PAYMENT_METHOD);
  }

}
