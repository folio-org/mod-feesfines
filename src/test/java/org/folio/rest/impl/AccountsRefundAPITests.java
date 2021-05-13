package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.domain.FeeFineStatus.OPEN;
import static org.folio.rest.utils.LogEventUtils.fetchLogEventPayloads;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountWaiveClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.rest.utils.ResourceClients.buildAccountsRefundClient;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfClosedAccountWithPaymentPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfClosedAccountWithTransferPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfClosedAccountWithPaymentAndTransferPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfOpenAccountWithPaymentPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfOpenAccountWithTransferPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfOpenAccountWithPaymentAndTransferPayloads;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.json.JsonObject;
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
import org.folio.test.support.ActionsAPITests;
import org.folio.test.support.EntityBuilder;
import org.folio.test.support.matcher.FeeFineActionMatchers;
import org.folio.util.pubsub.PubSubClientUtils;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class AccountsRefundAPITests extends ActionsAPITests {
  private static final String FIRST_ACCOUNT_ID = randomId();
  private static final String SECOND_ACCOUNT_ID = randomId();
  private static final String THIRD_ACCOUNT_ID = randomId();
  private static final String[] TWO_ACCOUNT_IDS = {FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID};

  private static final String USER_ID = randomId();
  private static final String SERVICE_POINT_ID = randomId();

  private static final String PAYMENT_METHOD = "Test payment method";
  private static final String WAIVE_REASON = "Test waive reason";
  private static final String REFUND_REASON = "Test refund reason";
  private static final String TRANSFER_ACCOUNT_BURSAR = "Bursar";
  private static final String TRANSFER_ACCOUNT_UNIVERSITY = "University";

  private static final String USER_NAME = "Folio, Tester";
  private static final String COMMENTS = "STAFF : staff comment \\n PATRON : patron comment";
  private static final boolean NOTIFY_PATRON = false;

  private static final String FEE_FINE_ACTIONS = "feefineactions";
  private static final String ERROR_MESSAGE =
    "Refund amount must be greater than zero and less than or equal to Selected amount";

  private static final String REFUND_TO_PATRON = "Refund to patron";
  private static final String REFUNDED_TO_PATRON = "Refunded to patron";
  private static final String REFUND_TO_BURSAR = "Refund to Bursar";
  private static final String REFUNDED_TO_BURSAR = "Refunded to Bursar";
  private static final String REFUND_TO_UNIVERSITY = "Refund to University";
  private static final String REFUNDED_TO_UNIVERSITY = "Refunded to University";

  private final ResourceClient actionsClient = buildFeeFineActionsClient();
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

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfClosedAccountWithPaymentPayloads())));
    assertThat(payloads, hasSize(4));
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

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfClosedAccountWithTransferPayloads())));
    assertThat(payloads, hasSize(4));
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

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfClosedAccountWithPaymentAndTransferPayloads())));
    assertThat(payloads, hasSize(7));
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

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfOpenAccountWithPaymentPayloads())));
    assertThat(payloads, hasSize(4));
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

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfOpenAccountWithTransferPayloads())));
    assertThat(payloads, hasSize(4));
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

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfOpenAccountWithPaymentAndTransferPayloads())));
    assertThat(payloads, hasSize(7));
  }

  @Test
  public void refundFailsWhenRequestedAmountExceedsRefundableAmount() {
    double initialAmount = 7.0;
    double payAmount = 3.0;
    double transferAmount = 2.0;
    double waiveAmount = 1.0;
    double refundAmount = 6.0;

    DefaultActionRequest request = createRefundRequest(refundAmount);

    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
  }

  @Test
  public void refundFailsWhenThereAreNoRefundableActionsForAccount() {
    double initialAmount = 7.0;
    double payAmount = 0.0;
    double transferAmount = 0.0;
    double waiveAmount = 5.0;
    double refundAmount = 4.0;

    DefaultActionRequest request = createRefundRequest(refundAmount);

    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
  }

  @Test
  public void return404WhenAccountDoesNotExist() {
    refundClient.post(toJson(createRefundRequest(10.0)))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(equalTo(format("Fee/fine ID %s not found", FIRST_ACCOUNT_ID)));
  }

  @Test
  public void return422WhenRequestedAmountIsNegative() {
    DefaultActionRequest request = createRefundRequest(-1.0);
    testSingleRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
  }

  @Test
  public void return422WhenRequestedAmountIsZero() {
    DefaultActionRequest request = createRefundRequest(0.0);
    testSingleRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
  }

  @Test
  public void return422WhenRequestedAmountIsInvalidString() {
    DefaultActionRequest request = createRefundRequest(0.0).withAmount("eleven");
    testSingleRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Invalid amount entered");
  }

  @Test
  public void bulkRefundAmountIsDistributedBetweenAccountsEvenlyRecursively() {
    final double refundAmount = 15.0; // even split is 15/3=5 per account

    // Closed, served third, full payment refund, partial transfer refund
    double initialAmount1 = 10.0;
    double payAmount1 = 5.0;
    double transferAmount1 = 5.0;
    double waiveAmount1 = 0.0;
    double expectedRemainingAmount1 = initialAmount1 - payAmount1 - transferAmount1 - waiveAmount1;
    prepareAccount(FIRST_ACCOUNT_ID, initialAmount1, payAmount1, transferAmount1, waiveAmount1);

    // Open, served first, full payment refund, full transfer refund
    double initialAmount2 = 10.0;
    double payAmount2 = 2.0;
    double transferAmount2 = 1.0;
    double waiveAmount2 = 0.0;
    double expectedRemainingAmount2 = initialAmount2 - payAmount2 - transferAmount2 - waiveAmount2;
    prepareAccount(SECOND_ACCOUNT_ID, initialAmount2, payAmount2, transferAmount2, waiveAmount2);

    // Open, served second, full payment refund, partial transfer refund
    double initialAmount3 = 10.0;
    double payAmount3 = 4.0;
    double transferAmount3 = 3.0;
    double waiveAmount3 = 0.0;
    double expectedRemainingAmount3 = initialAmount3 - payAmount3 - transferAmount3 - waiveAmount3;
    prepareAccount(THIRD_ACCOUNT_ID, initialAmount3, payAmount3, transferAmount3, waiveAmount3);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -5.0, payAmount1, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -6.0, 1.0, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -1.0, payAmount1, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, 1.0, REFUND.getPartialResult(), REFUNDED_TO_BURSAR),

      feeFineActionMatcher(SECOND_ACCOUNT_ID, 5.0, payAmount2, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 4.0, transferAmount2, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 6.0, payAmount2, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 7.0, transferAmount2, REFUND.getFullResult(), REFUNDED_TO_BURSAR),

      feeFineActionMatcher(THIRD_ACCOUNT_ID, -1.0, payAmount3, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, -3.0, 2.0, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, 1.0, payAmount3, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, 3.0, 2.0, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    Response response = bulkRefundClient.post(
      createBulkRequest(refundAmount, FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID, THIRD_ACCOUNT_ID));

    verifyBulkResponse(response, refundAmount, expectedFeeFineActions);
    verifyActions(18, expectedFeeFineActions); // 6 payments/transfer actions + 12 refund actions

    Account firstAccount = verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID,
      REFUND.getPartialResult(), expectedRemainingAmount1, CLOSED.getValue());

    Account secondAccount = verifyAccountAndGet(accountsClient, SECOND_ACCOUNT_ID,
      REFUND.getFullResult(), expectedRemainingAmount2, OPEN.getValue());

    Account thirdAccount = verifyAccountAndGet(accountsClient, THIRD_ACCOUNT_ID,
      REFUND.getPartialResult(), expectedRemainingAmount3, OPEN.getValue());

    verifyThatFeeFineBalanceChangedEventsWereSent(firstAccount, secondAccount, thirdAccount);
  }

  @Test
  public void bulkRefundFailsWhenRequestedAmountExceedsRefundableAmount() {
    DefaultBulkActionRequest request = createBulkRequest(11, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(10, 2, 3, 4, request, SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE,
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkRefundFailsWhenThereAreNoRefundableActionsForAccount() {
    DefaultBulkActionRequest request = createBulkRequest(5, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(10, 0, 0, 4, request, SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE,
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkReturn404WhenAccountDoesNotExist() {
    String errorMessageTemplate = "Fee/fine ID %s not found";

    bulkRefundClient.post(toJson(createBulkRequest(10.0, TWO_ACCOUNT_IDS)))
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
    DefaultBulkActionRequest request = createBulkRequest(-1.0, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive",
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkReturn422WhenRequestedAmountIsZero() {
    DefaultBulkActionRequest request = createBulkRequest(0.0, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Amount must be positive",
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkReturn422WhenRequestedAmountIsInvalidString() {
    DefaultBulkActionRequest request = createBulkRequest(0.0, TWO_ACCOUNT_IDS).withAmount("eleven");
    testBulkRefundFailure(10, 0, 0, 0, request, SC_UNPROCESSABLE_ENTITY, "Invalid amount entered",
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void refundForTransfersToMultipleTransferAccounts() {
    double initialAmount = 10.0;
    double refundAmount = initialAmount;

    double transferToBursar1 = 1.0;
    double transferToUniversity1 = 2.0;
    double transferToBursar2 = 3.0;
    double transferToUniversity2 = 4.0;

    postAccount(createAccount(FIRST_ACCOUNT_ID, initialAmount, initialAmount));

    ResourceClient transferClient = buildAccountTransferClient(FIRST_ACCOUNT_ID);
    performAction(transferClient, createRequest(transferToBursar1, TRANSFER_ACCOUNT_BURSAR));
    performAction(transferClient, createRequest(transferToUniversity1, TRANSFER_ACCOUNT_UNIVERSITY));
    performAction(transferClient, createRequest(transferToBursar2, TRANSFER_ACCOUNT_BURSAR));
    performAction(transferClient, createRequest(transferToUniversity2, TRANSFER_ACCOUNT_UNIVERSITY));

    double amountTransferredToBursar = transferToBursar1 + transferToBursar2;
    double amountTransferredToUniversity = transferToUniversity1 + transferToUniversity2;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -4.0, amountTransferredToBursar,
        CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -10.0, amountTransferredToUniversity,
        CREDIT.getFullResult(), REFUND_TO_UNIVERSITY),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -6.0, amountTransferredToBursar,
        REFUND.getFullResult(), REFUNDED_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, amountTransferredToUniversity,
        REFUND.getFullResult(), REFUNDED_TO_UNIVERSITY)
    );

    Response response = refundClient.post(createRefundRequest(refundAmount));

    verifyResponse(response, refundAmount, expectedFeeFineActions.size());
    verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID, REFUND.getFullResult(), 0.0, CLOSED.getValue());
    verifyActions(8, expectedFeeFineActions); // 4 transfers + 2 credits + 2 refunds
  }

  @Test
  public void bulkRefundForTransfersToMultipleTransferAccounts() {
    double initialAmount = 10.0;
    double refundAmount = initialAmount * 2;

    double transferToBursar1 = 1.0;
    double transferToUniversity1 = 2.0;
    double transferToBursar2 = 3.0;
    double transferToUniversity2 = 4.0;

    postAccount(createAccount(FIRST_ACCOUNT_ID, initialAmount, initialAmount));
    ResourceClient transferClient1 = buildAccountTransferClient(FIRST_ACCOUNT_ID);
    performAction(transferClient1, createRequest(transferToBursar1, TRANSFER_ACCOUNT_BURSAR));
    performAction(transferClient1, createRequest(transferToUniversity1, TRANSFER_ACCOUNT_UNIVERSITY));
    performAction(transferClient1, createRequest(transferToBursar2, TRANSFER_ACCOUNT_BURSAR));
    performAction(transferClient1, createRequest(transferToUniversity2, TRANSFER_ACCOUNT_UNIVERSITY));

    postAccount(createAccount(SECOND_ACCOUNT_ID, initialAmount, initialAmount));
    ResourceClient transferClient2 = buildAccountTransferClient(SECOND_ACCOUNT_ID);
    performAction(transferClient2, createRequest(transferToBursar1, TRANSFER_ACCOUNT_BURSAR));
    performAction(transferClient2, createRequest(transferToUniversity1, TRANSFER_ACCOUNT_UNIVERSITY));
    performAction(transferClient2, createRequest(transferToBursar2, TRANSFER_ACCOUNT_BURSAR));
    performAction(transferClient2, createRequest(transferToUniversity2, TRANSFER_ACCOUNT_UNIVERSITY));

    double amountTransferredToBursar = transferToBursar1 + transferToBursar2;
    double amountTransferredToUniversity = transferToUniversity1 + transferToUniversity2;

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -4.0, amountTransferredToBursar,
        CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -10.0, amountTransferredToUniversity,
        CREDIT.getFullResult(), REFUND_TO_UNIVERSITY),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, -6.0, amountTransferredToBursar,
        REFUND.getFullResult(), REFUNDED_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, amountTransferredToUniversity,
        REFUND.getFullResult(), REFUNDED_TO_UNIVERSITY),

      feeFineActionMatcher(SECOND_ACCOUNT_ID, -4.0, amountTransferredToBursar,
        CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, -10.0, amountTransferredToUniversity,
        CREDIT.getFullResult(), REFUND_TO_UNIVERSITY),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, -6.0, amountTransferredToBursar,
        REFUND.getFullResult(), REFUNDED_TO_BURSAR),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 0.0, amountTransferredToUniversity,
        REFUND.getFullResult(), REFUNDED_TO_UNIVERSITY)
    );

    Response response = bulkRefundClient.post(createBulkRequest(refundAmount, TWO_ACCOUNT_IDS));

    verifyBulkResponse(response, refundAmount, expectedFeeFineActions);
    verifyActions(16, expectedFeeFineActions); // 8 transfers + 4 credits + 4 refunds

    Account firstAccount = verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID,
      REFUND.getFullResult(), 0.0, CLOSED.getValue());

    Account secondAccount = verifyAccountAndGet(accountsClient, SECOND_ACCOUNT_ID,
      REFUND.getFullResult(), 0.0, CLOSED.getValue());

    verifyThatFeeFineBalanceChangedEventsWereSent(firstAccount, secondAccount);
  }

  private void testSingleAccountRefundSuccess(double initialAmount, double payAmount, double transferAmount,
    double waiveAmount, double requestedAmount, FeeFineStatus expectedStatus,
    String expectedPaymentStatus, List<Matcher<JsonObject>> expectedFeeFineActions) {

    double expectedRemainingAmount = initialAmount - payAmount - transferAmount - waiveAmount;

    int actionsCountBeforeRefund = prepareAccount(FIRST_ACCOUNT_ID, initialAmount, payAmount,
      transferAmount, waiveAmount);
    int totalExpectedActionsCount = actionsCountBeforeRefund + expectedFeeFineActions.size();

    Response response = refundClient.post(createRefundRequest(requestedAmount));
    verifyResponse(response, requestedAmount, expectedFeeFineActions.size());

    verifyActions(totalExpectedActionsCount, expectedFeeFineActions);

    Account accountAfterRefund = verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID,
      expectedPaymentStatus, expectedRemainingAmount, expectedStatus.getValue());

    verifyThatFeeFineBalanceChangedEventsWereSent(accountAfterRefund);
  }

  private void verifyResponse(Response response, double requestedAmount, int expectedActionsCount) {
    response.then()
      .statusCode(SC_CREATED)
      .body("accountId", is(FIRST_ACCOUNT_ID))
      .body("amount", is(new MonetaryValue(requestedAmount).toString()))
      .body(FEE_FINE_ACTIONS, hasSize(expectedActionsCount));
  }

  private void verifyBulkResponse(Response response, double requestedAmount,
    List<Matcher<JsonObject>> expectedFeeFineActions, String... accountIds) {

    response.then()
      .statusCode(SC_CREATED)
      .body("accountIds", hasItems(accountIds))
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

  private void testSingleRefundFailure(double initialAmount, double payAmount,
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

  private void testBulkRefundFailure(double initialAmount, double payAmount, double transferAmount,
    double waiveAmount, DefaultBulkActionRequest request, int expectedStatus, String errorMessage,
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

  private int prepareAccount(String accountId, double initialAmount, double payAmount,
    double transferAmount, double waiveAmount) {

    if (payAmount < 0 || transferAmount < 0 || waiveAmount < 0) {
      throw new IllegalArgumentException("Amounts can't be negative");
    }

    postAccount(createAccount(accountId, initialAmount, initialAmount));

    int performedActionsCount = 0;

    if (payAmount > 0) {
      performAction(buildAccountPayClient(accountId), createRequest(payAmount, PAYMENT_METHOD));
      performedActionsCount++;
    }
    if (waiveAmount > 0) {
      performAction(buildAccountWaiveClient(accountId), createRequest(waiveAmount, WAIVE_REASON));
      performedActionsCount++;
    }
    if (transferAmount > 0) {
      performAction(buildAccountTransferClient(accountId), createRequest(transferAmount, TRANSFER_ACCOUNT_BURSAR));
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

  private static DefaultActionRequest createRefundRequest(double amount) {
    return createRequest(amount, REFUND_REASON);
  }

  private static DefaultActionRequest createRequest(double amount, String paymentMethod) {
    return new DefaultActionRequest()
      .withAmount(new MonetaryValue(amount).toString())
      .withPaymentMethod(paymentMethod)
      .withServicePointId(SERVICE_POINT_ID)
      .withUserName(USER_NAME)
      .withNotifyPatron(NOTIFY_PATRON)
      .withComments(COMMENTS);
  }

  private static DefaultBulkActionRequest createBulkRequest(double amount, String... accountIds) {
    return new DefaultBulkActionRequest()
      .withAccountIds(Arrays.asList(accountIds))
      .withAmount(new MonetaryValue(amount).toString())
      .withPaymentMethod(REFUND_REASON)
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

  private static Account createAccount(String accountId, double amount, double remainingAmount) {
    return EntityBuilder.buildAccount(amount, remainingAmount)
      .withId(accountId)
      .withUserId(USER_ID);
  }

  private ValidatableResponse performAction(ResourceClient resourceClient, double amount) {
    return performAction(resourceClient, createRefundRequest(amount));
  }

  private ValidatableResponse performAction(ResourceClient resourceClient,
    DefaultActionRequest request) {

    Response post = resourceClient.post(request);
    return post
      .then()
      .statusCode(SC_CREATED);
  }

  public static Matcher<JsonObject> feeFineActionMatcher(String accountId, double balance,
    double amount, String actionType, String transactionInfo) {

    return FeeFineActionMatchers.feeFineAction(accountId, USER_ID, balance, amount, actionType,
      transactionInfo, USER_NAME, COMMENTS, NOTIFY_PATRON, SERVICE_POINT_ID, REFUND_REASON);
  }

}
