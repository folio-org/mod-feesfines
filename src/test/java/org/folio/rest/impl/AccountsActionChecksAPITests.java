package org.folio.rest.impl;

import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckWaiveClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckWaiveClient;
import static org.folio.rest.utils.ResourceClients.feeFineActionsClient;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.BulkCheckActionRequest;
import org.folio.rest.jaxrs.model.CheckActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AccountsActionChecksAPITests extends ApiTests {

  private static final String ACCOUNTS_TABLE = "accounts";

  private static final double ACCOUNT_INITIAL_AMOUNT = 9.00;
  private static final double ACCOUNT_REMAINING_AMOUNT = 4.55;
  private static final double REQUESTED_AMOUNT = 1.23;
  private static final String REQUESTED_AMOUNT_STRING = String.valueOf(REQUESTED_AMOUNT);
  private static final String ERROR_MESSAGE_MUST_BE_POSITIVE = "Amount must be positive";
  private static final String ERROR_MESSAGE_INVALID_AMOUNT= "Invalid amount entered";
  private static final String ERROR_MESSAGE_ALREADY_CLOSED= "Fee/fine is already closed";

  private final Action action;
  private ResourceClient bulkClient;

  private Account firstAccount;
  private Account secondAccount;
  private ResourceClient accountsCheckPayClient;
  private ResourceClient accountsCheckWaiveClient;
  private ResourceClient accountsCheckTransferClient;
  private ResourceClient accountsCheckRefundClient;
  private final ResourceClient accountsBulkCheckPayClient = buildAccountBulkCheckPayClient();
  private final ResourceClient accountsBulkCheckWaiveClient = buildAccountBulkCheckWaiveClient();
  private final ResourceClient accountsBulkCheckTransferClient = buildAccountBulkCheckTransferClient();



  @Before
  public void setUp() {
    firstAccount = createAccount();
    secondAccount = createAccount();
    accountsCheckPayClient = buildAccountCheckPayClient(firstAccount.getId());
    accountsCheckWaiveClient = buildAccountCheckWaiveClient(firstAccount.getId());
    accountsCheckTransferClient = buildAccountCheckTransferClient(firstAccount.getId());
    accountsCheckRefundClient = buildAccountCheckRefundClient(firstAccount.getId());
    bulkClient = getAccountsBulkClient();
  }

  public AccountsActionChecksAPITests(Action action) {
    this.action = action;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Object[] parameters() {
    return new Object[] { PAY, WAIVE, TRANSFER };
  }

  private ResourceClient getAccountsBulkClient() {
    switch (action) {
      case PAY:
        return accountsBulkCheckPayClient;
      case WAIVE:
        return accountsBulkCheckWaiveClient;
      case TRANSFER:
        return accountsBulkCheckTransferClient;
      default:
        throw new IllegalArgumentException("Failed to get ResourceClient for action: " + action.name());
    }
  }

  @Test
  public void checkPayAmountShouldBeAllowed() {
    actionShouldBeAllowed(false, accountsCheckPayClient, "3.32");
  }

  @Test
  public void bulkCheckAmountShouldBeAllowed() {
    actionShouldBeAllowed(true, bulkClient, "7.87");
  }

  @Test
  public void checkWaiveAmountShouldBeAllowed() {
    actionShouldBeAllowed(false, accountsCheckWaiveClient, "3.32");
  }

  @Test
  public void checkTransferAmountShouldBeAllowed() {
    actionShouldBeAllowed(false, accountsCheckTransferClient, "3.32");
  }

  @Test
  public void checkRefundAmountShouldBeAllowed() {
    double expectedRemainingAmount = 0.77;

    final Feefineaction feeFineAction = new Feefineaction()
      .withAccountId(firstAccount.getId())
      .withUserId(firstAccount.getUserId())
      .withAmountAction((REQUESTED_AMOUNT + expectedRemainingAmount) / 2);

    feeFineActionsClient()
      .post(feeFineAction.withTypeAction(PAY.getPartialResult()))
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    feeFineActionsClient()
      .post(feeFineAction.withTypeAction(TRANSFER.getPartialResult()))
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    actionShouldBeAllowed(false, accountsCheckRefundClient,
      String.valueOf(expectedRemainingAmount));
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(false, accountsCheckPayClient);
  }

  @Test
  public void bulkCheckAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(true, bulkClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(false, accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(false, accountsCheckTransferClient);
  }

  @Test
  public void checkRefundAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckRefundAmountShouldNotBeAllowedWithExceededAmount(accountsCheckRefundClient);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithNegativeAmount() {
    actionShouldNotBeAllowed(false, accountsCheckPayClient, "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void bulkCheckAmountShouldNotBeAllowedWithNegativeAmount() {
    actionShouldNotBeAllowed(true, bulkClient, "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithNegativeAmount() {
    actionShouldNotBeAllowed(false, accountsCheckWaiveClient, "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkTransferAmountShouldNotBeAllowedWithNegativeAmount() {
    actionShouldNotBeAllowed(false, accountsCheckTransferClient, "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkRefundAmountShouldNotBeAllowedWithNegativeAmount() {
    actionShouldNotBeAllowed(false, accountsCheckRefundClient, "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithZeroAmount() {
    actionShouldNotBeAllowed(false, accountsCheckPayClient, "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void bulkCheckAmountShouldNotBeAllowedWithZeroAmount() {
    actionShouldNotBeAllowed(true, bulkClient, "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithZeroAmount() {
    actionShouldNotBeAllowed(false, accountsCheckWaiveClient, "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkTransferAmountShouldNotBeAllowedWithZeroAmount() {
    actionShouldNotBeAllowed(false, accountsCheckTransferClient, "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkRefundAmountShouldNotBeAllowedWithZeroAmount() {
    actionShouldNotBeAllowed(false, accountsCheckRefundClient, "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkPayAmountShouldBeNumeric() {
    actionShouldNotBeAllowed(false, accountsCheckPayClient, "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
  }

  @Test
  public void bulkCheckAmountShouldBeNumeric() {
    actionShouldNotBeAllowed(true, bulkClient, "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
  }

  @Test
  public void checkWaiveAmountShouldBeNumeric() {
    actionShouldNotBeAllowed(false, accountsCheckWaiveClient, "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
  }

  @Test
  public void checkTransferAmountShouldBeNumeric() {
    actionShouldNotBeAllowed(false, accountsCheckTransferClient, "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
  }

  @Test
  public void checkRefundAmountShouldBeNumeric() {
    actionShouldNotBeAllowed(false, accountsCheckRefundClient, "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
  }

  @Test
  public void checkPayAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckShouldNotFailForNonExistentAccount(false, accountsCheckPayClient);
  }

  @Test
  public void bulkCheckAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckShouldNotFailForNonExistentAccount(true, bulkClient);
  }

  @Test
  public void checkWaiveAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckShouldNotFailForNonExistentAccount(false, accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckShouldNotFailForNonExistentAccount(false, accountsCheckTransferClient);
  }

  @Test
  public void checkRefundAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckShouldNotFailForNonExistentAccount(false, accountsCheckRefundClient);
  }


  @Test
  public void checkPayAmountShouldNotBeAllowedForClosedAccount() {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(false, accountsCheckPayClient);
  }

  @Test
  public void bulkCheckAmountShouldNotBeAllowedForClosedAccount() {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(true, bulkClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedForClosedAccount() {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(false, accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotBeAllowedForClosedAccount() {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(false, accountsCheckTransferClient);
  }

  @Test
  public void checkPayAmountShouldHandleLongDecimalsCorrectly() {
    successfulActionCheckHandlesLongDecimalsCorrectly(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldHandleLongDecimalsCorrectly() {
    successfulActionCheckHandlesLongDecimalsCorrectly(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldHandleLongDecimalsCorrectly() {
    successfulActionCheckHandlesLongDecimalsCorrectly(accountsCheckTransferClient);
  }

  @Test
  public void failedCheckPayReturnsInitialRequestedAmount() {
    failedActionCheckReturnsInitialRequestedAmount(accountsCheckPayClient);
  }

  @Test
  public void failedCheckWaiveReturnsInitialRequestedAmount() {
    failedActionCheckReturnsInitialRequestedAmount(accountsCheckWaiveClient);
  }

  @Test
  public void failedCheckTransferReturnsInitialRequestedAmount() {
    failedActionCheckReturnsInitialRequestedAmount(accountsCheckTransferClient);
  }

  private void actionShouldBeAllowed(boolean bulk, ResourceClient actionCheckClient,
    String remaining) {

    actionCheckClient.attemptCreate(createRequest(bulk, REQUESTED_AMOUNT_STRING))
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("allowed", is(true))
      .body("accountIds", bulk ? is(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
        : nullValue())
      .body("amount", is(REQUESTED_AMOUNT_STRING))
      .body("remainingAmount", is(remaining));
  }

  private void actionCheckAmountShouldNotBeAllowedWithExceededAmount(boolean bulk,
    ResourceClient actionCheckClient) {

    String expectedErrorMessage = "Requested amount exceeds remaining amount";
    String amount = String.valueOf(bulk ? ACCOUNT_REMAINING_AMOUNT * 2 + 1 : REQUESTED_AMOUNT + 10);

    baseActionCheckAmountShouldNotBeAllowedWithExceededAmount(actionCheckClient,
      expectedErrorMessage, amount);
  }

  private void actionCheckRefundAmountShouldNotBeAllowedWithExceededAmount(
    ResourceClient actionCheckClient) {

    String expectedErrorMessage =
      "Refund amount must be greater than zero and less than or equal to Selected amount";

    baseActionCheckAmountShouldNotBeAllowedWithExceededAmount(
      actionCheckClient, expectedErrorMessage, String.valueOf(REQUESTED_AMOUNT + 10));
  }

  private void baseActionCheckAmountShouldNotBeAllowedWithExceededAmount(
    ResourceClient actionCheckClient, String expectedErrorMessage, String amount) {

    CheckActionRequest accountCheckRequest = new CheckActionRequest();
    accountCheckRequest.withAmount(amount);

    actionCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("errorMessage", is(expectedErrorMessage))
      .body("allowed", is(false))
      .body("amount", is(accountCheckRequest.getAmount()));
  }

  private void actionShouldNotBeAllowed(boolean bulk,
    ResourceClient accountsActionCheckClient, String amount, String errorMessage) {

    accountsActionCheckClient.attemptCreate(createRequest(bulk, amount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(containsString(errorMessage))
      .body("allowed", is(false))
      .body("accountIds", bulk ? is(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
        : nullValue())
      .body("amount", is(amount));
  }

  private void actionCheckShouldNotFailForNonExistentAccount(boolean bulk,
    ResourceClient actionCheckClient) {

    actionCheckClient.attemptCreate(createRequest(bulk, REQUESTED_AMOUNT_STRING))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  private void actionCheckAmountShouldNotBeAllowedForClosedAccount(boolean bulk,
    ResourceClient client) {

    closeAllAccounts();

    client.attemptCreate(createRequest(bulk, REQUESTED_AMOUNT_STRING))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("allowed", is(false))
      .body("accountIds", bulk ? is(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
        : nullValue())
      .body("amount", is(REQUESTED_AMOUNT_STRING))
      .body("errorMessage", is(ERROR_MESSAGE_ALREADY_CLOSED));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  private void closeAllAccounts() {
    Stream.of(firstAccount, secondAccount)
      .forEach(account -> {
        account.setRemaining(0.00);
        account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
        accountsClient.update(account.getId(), account);
      });
  }

  private Object createRequest(boolean bulk, String amount) {
    if (bulk) {
      return new BulkCheckActionRequest()
        .withAccountIds(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
        .withAmount(amount);
    } else {
      return new CheckActionRequest().withAmount(amount);
    }
  }

  private void successfulActionCheckHandlesLongDecimalsCorrectly(ResourceClient client) {
    firstAccount.setRemaining(1.235987654321); // will be rounded to 1.24
    accountsClient.update(firstAccount.getId(), firstAccount);

    client.attemptCreate(new CheckActionRequest().withAmount("1.004987654321")) // rounded to 1.00
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("allowed", is(true))
      .body("amount", is("1.00"))
      .body("remainingAmount", is("0.24")); // 1.24 - 1.00

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  private void failedActionCheckReturnsInitialRequestedAmount(ResourceClient client) {
    firstAccount.setRemaining(0.99);
    accountsClient.update(firstAccount.getId(), firstAccount);

    String requestedAmount = "1.004123456789"; // rounded to 1.00 when compared to account balance

    client.attemptCreate(new CheckActionRequest().withAmount(requestedAmount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("allowed", is(false))
      .body("amount", is(requestedAmount))
      .body("errorMessage", is("Requested amount exceeds remaining amount"));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  private Account createAccount() {
    Account accountToPost = EntityBuilder.buildAccount(ACCOUNT_INITIAL_AMOUNT,
      ACCOUNT_REMAINING_AMOUNT);
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
    return accountToPost;
  }
}
