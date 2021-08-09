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
import static org.folio.rest.utils.JsonHelper.write;
import static org.folio.rest.utils.LogEventUtils.fetchLogEventPayloads;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountWaiveClient;
import static org.folio.rest.utils.ResourceClients.buildAccountsRefundClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfClosedAccountWithPaymentAndTransferPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfClosedAccountWithPaymentPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfClosedAccountWithTransferPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfOpenAccountWithPaymentAndTransferPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfOpenAccountWithPaymentPayloads;
import static org.folio.test.support.matcher.LogEventMatcher.partialRefundOfOpenAccountWithTransferPayloads;
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
import org.folio.test.support.ActionsAPITests;
import org.folio.test.support.EntityBuilder;
import org.folio.test.support.matcher.FeeFineActionMatchers;
import org.folio.util.PomUtils;
import org.folio.util.UuidUtil;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.vertx.core.json.JsonObject;

public class AccountsRefundAPITests extends ActionsAPITests {
  private static final String FIRST_ACCOUNT_ID = randomId();
  private static final String SECOND_ACCOUNT_ID = randomId();
  private static final String THIRD_ACCOUNT_ID = randomId();
  private static final String[] TWO_ACCOUNT_IDS = { FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID };

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
    MonetaryValue payAmount = new MonetaryValue(5.0);
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(5.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 5.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount,
      transferAmount, waiveAmount, refundAmount, CLOSED, REFUND.getFullResult(),
      expectedFeeFineActions);
  }

  @Test
  public void fullRefundOfClosedAccountWithTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(5.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(5.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 5.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
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
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 3.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 5.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
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
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 2.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 5.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON)
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
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 2.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 5.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
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
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 1.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 1.0, transferAmount, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 4.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 6.0, transferAmount, REFUND.getFullResult(), REFUNDED_TO_BURSAR)
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
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 3.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfClosedAccountWithPaymentPayloads())));
    assertThat(payloads, hasSize(4));
  }

  @Test
  public void partialRefundOfClosedAccountWithTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(5.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(3.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 3.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfClosedAccountWithTransferPayloads())));
    assertThat(payloads, hasSize(4));
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
      feeFineActionMatcher(FIRST_ACCOUNT_ID,  0.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID,  0.0, transferRefundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID,  3.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID,  4.0, transferRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      CLOSED, REFUND.getPartialResult(), expectedFeeFineActions);

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfClosedAccountWithPaymentAndTransferPayloads())));
    assertThat(payloads, hasSize(7));
  }

  @Test
  public void partialRefundOfOpenAccountWithPayment() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(2.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 2.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 4.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfOpenAccountWithPaymentPayloads())));
    assertThat(payloads, hasSize(4));
  }

  @Test
  public void partialRefundOfOpenAccountWithTransfer() {
    MonetaryValue initialAmount = new MonetaryValue(6.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(3.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(2.0);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 2.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 4.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount,
      refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(
      payload -> assertThat(payload, is(partialRefundOfOpenAccountWithTransferPayloads())));
    assertThat(payloads, hasSize(4));
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
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 1.0, payAmount, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 1.0, transferRefundAmount, CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 4.0, payAmount, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 5.0, transferRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
    );

    testSingleAccountRefundSuccess(initialAmount, payAmount, transferAmount, waiveAmount, refundAmount,
      OPEN, REFUND.getPartialResult(), expectedFeeFineActions);

    List<String> payloads = fetchLogEventPayloads(getOkapi());
    payloads.forEach(payload -> assertThat(payload, is(partialRefundOfOpenAccountWithPaymentAndTransferPayloads())));
    assertThat(payloads, hasSize(7));
  }

  @Test
  public void refundFailsWhenRequestedAmountExceedsRefundableAmount() {
    MonetaryValue initialAmount = new MonetaryValue(7.0);
    MonetaryValue payAmount = new MonetaryValue(3.0);
    MonetaryValue transferAmount = new MonetaryValue(2.0);
    MonetaryValue waiveAmount = new MonetaryValue(1.0);
    MonetaryValue refundAmount = new MonetaryValue(6.0);

    DefaultActionRequest request = createRefundRequest(refundAmount);

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

    DefaultActionRequest request = createRefundRequest(refundAmount);

    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE);
  }

  @Test
  public void return404WhenAccountDoesNotExist() {
    MonetaryValue amount = new MonetaryValue(10.0);
    refundClient.post(toJson(createRefundRequest(amount)))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(equalTo(format("Fee/fine ID %s not found", FIRST_ACCOUNT_ID)));
  }

  @Test
  public void return422WhenRequestedAmountIsNegative() {
    MonetaryValue amount = new MonetaryValue(-1.0);
    DefaultActionRequest request = createRefundRequest(amount);

    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(0.0);

    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
  }

  @Test
  public void return422WhenRequestedAmountIsZero() {
    MonetaryValue amount = new MonetaryValue(0.0);
    DefaultActionRequest request = createRefundRequest(amount);

    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(0.0);

    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, "Amount must be positive");
  }

  @Test
  public void return422WhenRequestedAmountIsInvalidString() {
    MonetaryValue amount = new MonetaryValue(0.0);
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue payAmount = new MonetaryValue(0.0);
    MonetaryValue transferAmount = new MonetaryValue(0.0);
    MonetaryValue waiveAmount = new MonetaryValue(0.0);

    DefaultActionRequest request = createRefundRequest(amount).withAmount("eleven");
    testSingleRefundFailure(initialAmount, payAmount, transferAmount, waiveAmount, request,
      SC_UNPROCESSABLE_ENTITY, "Invalid amount entered");
  }

  @Test
  public void bulkRefundAmountIsDistributedBetweenAccountsEvenlyRecursively() {
    double refundAmount = 15.0; // even split is 15/3=5 per account

    // Closed, served third, full payment refund, partial transfer refund
    MonetaryValue initialAmount1 = new MonetaryValue(10.0);
    MonetaryValue payAmount1 = new MonetaryValue(5.0);
    MonetaryValue transferAmount1 = new MonetaryValue(5.0);
    MonetaryValue waiveAmount1 = new MonetaryValue(0.0);
    MonetaryValue expectedRefundAmount1 = new MonetaryValue(6.0);
    MonetaryValue expectedRemainingAmount1 = initialAmount1.subtract(payAmount1)
      .subtract(transferAmount1)
      .subtract(waiveAmount1)
      .add(expectedRefundAmount1);
    prepareAccount(FIRST_ACCOUNT_ID, initialAmount1, payAmount1, transferAmount1, waiveAmount1);

    // Open, served first, full payment refund, full transfer refund
    MonetaryValue initialAmount2 = new MonetaryValue(10.0);
    MonetaryValue payAmount2 = new MonetaryValue(2.0);
    MonetaryValue transferAmount2 = new MonetaryValue(1.0);
    MonetaryValue waiveAmount2 = new MonetaryValue(0.0);
    MonetaryValue expectedRefundAmount2 = new MonetaryValue(3.0);
    MonetaryValue expectedRemainingAmount2 = initialAmount2.subtract(payAmount2)
      .subtract(transferAmount2)
      .subtract(waiveAmount2)
      .add(expectedRefundAmount2);
    prepareAccount(SECOND_ACCOUNT_ID, initialAmount2, payAmount2, transferAmount2, waiveAmount2);

    // Open, served second, full payment refund, partial transfer refund
    MonetaryValue initialAmount3 = new MonetaryValue(10.0);
    MonetaryValue payAmount3 = new MonetaryValue(4.0);
    MonetaryValue transferAmount3 = new MonetaryValue(3.0);
    MonetaryValue waiveAmount3 = new MonetaryValue(0.0);
    MonetaryValue expectedRefundAmount3 = new MonetaryValue(6.0);
    MonetaryValue expectedRemainingAmount3 = initialAmount3.subtract(payAmount3)
      .subtract(transferAmount3)
      .subtract(waiveAmount3)
      .add(expectedRefundAmount3);
    prepareAccount(THIRD_ACCOUNT_ID, initialAmount3, payAmount3, transferAmount3, waiveAmount3);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, payAmount1, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, new MonetaryValue(1.0), CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 5.0, payAmount1, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 6.0, new MonetaryValue(1.0), REFUND.getPartialResult(), REFUNDED_TO_BURSAR),

      feeFineActionMatcher(SECOND_ACCOUNT_ID, 7.0, payAmount2, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 7.0, transferAmount2, CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 9.0, payAmount2, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 10.0, transferAmount2, REFUND.getFullResult(), REFUNDED_TO_BURSAR),

      feeFineActionMatcher(THIRD_ACCOUNT_ID, 3.0, payAmount3, CREDIT.getFullResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, 3.0, new MonetaryValue(2.0), CREDIT.getPartialResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, 7.0, payAmount3, REFUND.getFullResult(), REFUNDED_TO_PATRON),
      feeFineActionMatcher(THIRD_ACCOUNT_ID, 9.0, new MonetaryValue(2.0), REFUND.getPartialResult(), REFUNDED_TO_BURSAR)
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
    DefaultBulkActionRequest request = createBulkRequest(11.0, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(10.0, 2.0, 3.0, 4.0, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE,
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkRefundFailsWhenThereAreNoRefundableActionsForAccount() {
    DefaultBulkActionRequest request = createBulkRequest(5.0, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(10.0, 0.0, 0.0, 4.0, request,
      SC_UNPROCESSABLE_ENTITY, ERROR_MESSAGE,
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
    testBulkRefundFailure(10.0, 0.0, 0.0, 0.0, request,
      SC_UNPROCESSABLE_ENTITY, "Amount must be positive",
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkReturn422WhenRequestedAmountIsZero() {
    DefaultBulkActionRequest request = createBulkRequest(0.0, TWO_ACCOUNT_IDS);
    testBulkRefundFailure(10.0, 0.0, 0.0, 0.0, request,
      SC_UNPROCESSABLE_ENTITY, "Amount must be positive",
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void bulkReturn422WhenRequestedAmountIsInvalidString() {
    DefaultBulkActionRequest request = createBulkRequest(0.0, TWO_ACCOUNT_IDS)
      .withAmount("eleven");
    testBulkRefundFailure(10, 0, 0, 0, request,
      SC_UNPROCESSABLE_ENTITY, "Invalid amount entered",
      TWO_ACCOUNT_IDS);
  }

  @Test
  public void refundForTransfersToMultipleTransferAccounts() {
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue refundAmount = initialAmount;

    MonetaryValue transferToBursar1 = new MonetaryValue(1.0);
    MonetaryValue transferToUniversity1 = new MonetaryValue(2.0);
    MonetaryValue transferToBursar2 = new MonetaryValue(3.0);
    MonetaryValue transferToUniversity2 = new MonetaryValue(4.0);

    postAccount(createAccount(FIRST_ACCOUNT_ID, initialAmount, initialAmount));

    ResourceClient transferClient = buildAccountTransferClient(FIRST_ACCOUNT_ID);
    performAction(transferClient, createRequest(transferToBursar1, TRANSFER_ACCOUNT_BURSAR));
    performAction(transferClient,
      createRequest(transferToUniversity1, TRANSFER_ACCOUNT_UNIVERSITY));
    performAction(transferClient, createRequest(transferToBursar2, TRANSFER_ACCOUNT_BURSAR));
    performAction(transferClient,
      createRequest(transferToUniversity2, TRANSFER_ACCOUNT_UNIVERSITY));

    MonetaryValue amountTransferredToBursar = transferToBursar1.add(transferToBursar2);
    MonetaryValue amountTransferredToUniversity = transferToUniversity1.add(transferToUniversity2);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, amountTransferredToBursar,
        CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, amountTransferredToUniversity,
        CREDIT.getFullResult(), REFUND_TO_UNIVERSITY),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 4.0, amountTransferredToBursar,
        REFUND.getFullResult(), REFUNDED_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 10.0, amountTransferredToUniversity,
        REFUND.getFullResult(), REFUNDED_TO_UNIVERSITY)
    );

    Response response = refundClient.post(createRefundRequest(refundAmount));

    verifyResponse(response, refundAmount, expectedFeeFineActions.size());
    verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID, REFUND.getFullResult(),
      new MonetaryValue(10.0), CLOSED.getValue());
    verifyActions(8, expectedFeeFineActions); // 4 transfers + 2 credits + 2 refunds
  }

  @Test
  public void bulkRefundForTransfersToMultipleTransferAccounts() {
    MonetaryValue initialAmount = new MonetaryValue(10.0);
    MonetaryValue refundAmount = initialAmount.multiply(new MonetaryValue(2.0));

    MonetaryValue transferToBursar1 = new MonetaryValue(1.0);
    MonetaryValue transferToUniversity1 = new MonetaryValue(2.0);
    MonetaryValue transferToBursar2 = new MonetaryValue(3.0);
    MonetaryValue transferToUniversity2 = new MonetaryValue(4.0);

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

    MonetaryValue amountTransferredToBursar = transferToBursar1.add(transferToBursar2);
    MonetaryValue amountTransferredToUniversity = transferToUniversity1.add(transferToUniversity2);

    List<Matcher<JsonObject>> expectedFeeFineActions = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, amountTransferredToBursar,
        CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, amountTransferredToUniversity,
        CREDIT.getFullResult(), REFUND_TO_UNIVERSITY),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 4.0, amountTransferredToBursar,
        REFUND.getFullResult(), REFUNDED_TO_BURSAR),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 10.0, amountTransferredToUniversity,
        REFUND.getFullResult(), REFUNDED_TO_UNIVERSITY),

      feeFineActionMatcher(SECOND_ACCOUNT_ID, 0.0, amountTransferredToBursar,
        CREDIT.getFullResult(), REFUND_TO_BURSAR),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 0.0, amountTransferredToUniversity,
        CREDIT.getFullResult(), REFUND_TO_UNIVERSITY),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 4.0, amountTransferredToBursar,
        REFUND.getFullResult(), REFUNDED_TO_BURSAR),
      feeFineActionMatcher(SECOND_ACCOUNT_ID, 10.0, amountTransferredToUniversity,
        REFUND.getFullResult(), REFUNDED_TO_UNIVERSITY)
    );

    Response response = bulkRefundClient.post(createBulkRequest(refundAmount.toDouble(), TWO_ACCOUNT_IDS));

    verifyBulkResponse(response, refundAmount.toDouble(), expectedFeeFineActions);
    verifyActions(16, expectedFeeFineActions); // 8 transfers + 4 credits + 4 refunds

    Account firstAccount = verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID,
      REFUND.getFullResult(), new MonetaryValue(10.0), CLOSED.getValue());

    Account secondAccount = verifyAccountAndGet(accountsClient, SECOND_ACCOUNT_ID,
      REFUND.getFullResult(), new MonetaryValue(10.0), CLOSED.getValue());

    verifyThatFeeFineBalanceChangedEventsWereSent(firstAccount, secondAccount);
  }

  @Test
  public void previouslyRefundedAmountIsConsideredWhenRepeatedlyRefundingSameAccount() {
    MonetaryValue initialAmount = new MonetaryValue(11.0);
    MonetaryValue payAmount = new MonetaryValue(6.0);
    MonetaryValue transferAmount = new MonetaryValue(5.0);
    MonetaryValue waiveAmount = new MonetaryValue(0.0);
    MonetaryValue refundAmount = new MonetaryValue(4.0);

    prepareAccount(FIRST_ACCOUNT_ID, initialAmount, payAmount, transferAmount, waiveAmount);

    // first refund attempt for 4.0

    List<Matcher<JsonObject>> expectedFeeFineActions1 = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 0.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 4.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    Response response1 = refundClient.post(createRefundRequest(refundAmount));
    verifyResponse(response1, refundAmount, 2);
    verifyActions(4, expectedFeeFineActions1);

    Account accountAfterRefund1 = verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID,
      REFUND.getPartialResult(), new MonetaryValue(4.0), CLOSED.getValue());

    verifyThatFeeFineBalanceChangedEventsWereSent(accountAfterRefund1);

    // second refund attempt for 4.0

    List<Matcher<JsonObject>> expectedFeeFineActions2 = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 4.0, refundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 8.0, refundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    Response response2 = refundClient.post(createRefundRequest(refundAmount));
    verifyResponse(response2, refundAmount, 2);
    verifyActions(6, expectedFeeFineActions2);

    Account accountAfterRefund2 = verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID,
      REFUND.getPartialResult(), new MonetaryValue(8.0), CLOSED.getValue());

    verifyThatFeeFineBalanceChangedEventsWereSent(accountAfterRefund2);

    // third refund attempt for 4.0, but only 3.0 is refundable: (6.0 paid) + (5.0 transferred) - (2*4.0 refunded)

    DefaultActionRequest invalidRefundRequest = createRefundRequest(refundAmount);

    refundClient.post(invalidRefundRequest)
      .then()
      .statusCode(422)
      .body("accountId", is(FIRST_ACCOUNT_ID))
      .body("amount", is(invalidRefundRequest.getAmount()))
      .body("errorMessage", is(ERROR_MESSAGE));

    // fourth refund attempt for 3.0

    MonetaryValue newRefundAmount = new MonetaryValue(3.0);

    List<Matcher<JsonObject>> expectedFeeFineActions4 = Arrays.asList(
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 8.0, newRefundAmount, CREDIT.getPartialResult(), REFUND_TO_PATRON),
      feeFineActionMatcher(FIRST_ACCOUNT_ID, 11.0, newRefundAmount, REFUND.getPartialResult(), REFUNDED_TO_PATRON)
    );

    Response response4 = refundClient.post(createRefundRequest(new MonetaryValue(3.0)));
    verifyResponse(response4, newRefundAmount, 2);
    verifyActions(8, expectedFeeFineActions4);

    Account accountAfterRefund4 = verifyAccountAndGet(accountsClient, FIRST_ACCOUNT_ID,
      REFUND.getPartialResult(), new MonetaryValue(11.0), CLOSED.getValue());

    verifyThatFeeFineBalanceChangedEventsWereSent(accountAfterRefund4);
  }

  private void testSingleAccountRefundSuccess(MonetaryValue initialAmount, MonetaryValue payAmount,
    MonetaryValue transferAmount,
    MonetaryValue waiveAmount, MonetaryValue requestedAmount, FeeFineStatus expectedStatus,
    String expectedPaymentStatus, List<Matcher<JsonObject>> expectedFeeFineActions) {

    MonetaryValue expectedRemainingAmount = initialAmount.subtract(payAmount)
      .subtract(transferAmount)
      .subtract(waiveAmount)
      .add(requestedAmount);

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

  private void verifyResponse(Response response, MonetaryValue requestedAmount,
    int expectedActionsCount) {
    response.then()
      .statusCode(SC_CREATED)
      .body("accountId", is(FIRST_ACCOUNT_ID))
      .body("amount", is(requestedAmount.toString()))
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

  private void testSingleRefundFailure(MonetaryValue initialAmount, MonetaryValue payAmount,
    MonetaryValue transferAmount, MonetaryValue waiveAmount, DefaultActionRequest request,
    int expectedStatus,
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

  private void testBulkRefundFailure(double initialAmount, double payAmount,
    double transferAmount,
    double waiveAmount, DefaultBulkActionRequest request, int expectedStatus,
    String errorMessage,
    String... accountIds) {

    int expectedActionsCount = 0;

    for (String accountId : accountIds) {
      expectedActionsCount += prepareAccount(
        accountId, new MonetaryValue(initialAmount), new MonetaryValue(payAmount),
        new MonetaryValue(transferAmount), new MonetaryValue(waiveAmount));
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
      performAction(buildAccountPayClient(accountId), createRequest(payAmount, PAYMENT_METHOD));
      performedActionsCount++;
    }
    if (waiveAmount.isPositive()) {
      performAction(buildAccountWaiveClient(accountId), createRequest(waiveAmount, WAIVE_REASON));
      performedActionsCount++;
    }
    if (transferAmount.isPositive()) {
      performAction(buildAccountTransferClient(accountId),
        createRequest(transferAmount, TRANSFER_ACCOUNT_BURSAR));
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

  private static DefaultActionRequest createRefundRequest(MonetaryValue amount) {
    return createRequest(amount, REFUND_REASON);
  }

  private static DefaultActionRequest createRequest(MonetaryValue amount, String paymentMethod) {
    return new DefaultActionRequest()
      .withAmount(String.valueOf(amount.toDouble()))
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
      JsonObject payload = new JsonObject();
      write(payload, "userId", account.getUserId());
      write(payload, "feeFineId", account.getId());
      write(payload, "feeFineTypeId", account.getFeeFineId());
      write(payload, "balance", account.getRemaining());
      write(payload, "loanId", account.getLoanId());

      verifyThatEventWasSent(EventType.FEE_FINE_BALANCE_CHANGED, payload);
    }
  }

  private void verifyThatEventWasSent(EventType eventType, JsonObject eventPayload) {
    Event event = new Event()
      .withEventType(eventType.name())
      .withEventPayload(eventPayload.toString())
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

  private static Account createAccount(String accountId, MonetaryValue amount,
    MonetaryValue remainingAmount) {

    return EntityBuilder.buildAccount(amount, remainingAmount)
      .withId(accountId)
      .withUserId(USER_ID);
  }

  private ValidatableResponse performAction(ResourceClient resourceClient,
    DefaultActionRequest request) {

    Response post = resourceClient.post(request);
    return post
      .then()
      .statusCode(SC_CREATED);
  }

  public static Matcher<JsonObject> feeFineActionMatcher(String accountId, double balance,
    MonetaryValue amount, String actionType, String transactionInfo) {

    return FeeFineActionMatchers.feeFineAction(accountId, USER_ID, balance, amount, actionType,
      transactionInfo, USER_NAME, COMMENTS, NOTIFY_PATRON, SERVICE_POINT_ID, REFUND_REASON);
  }

}
