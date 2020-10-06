package org.folio.rest.impl.accountactionchecks;

import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckWaiveClient;
import static org.folio.rest.utils.ResourceClients.feeFineActionsClient;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.utils.ResourceClient;
import org.junit.Before;
import org.junit.Test;

public class AccountsActionChecksAPITests extends AccountsActionChecksAPITestsBase {

  private static final double REQUESTED_AMOUNT = 1.23;
  private static final String ERROR_MESSAGE_MUST_BE_POSITIVE = "Amount must be positive";
  private static final String ERROR_MESSAGE_INVALID_AMOUNT = "Invalid amount entered";

  private ResourceClient accountsCheckPayClient;
  private ResourceClient accountsCheckWaiveClient;
  private ResourceClient accountsCheckTransferClient;
  private ResourceClient accountsCheckRefundClient;


  @Before
  public void setUp() {
    firstAccount = createAccount();
    secondAccount = createAccount();
    accountsCheckPayClient = buildAccountCheckPayClient(firstAccount.getId());
    accountsCheckWaiveClient = buildAccountCheckWaiveClient(firstAccount.getId());
    accountsCheckTransferClient = buildAccountCheckTransferClient(firstAccount.getId());
    accountsCheckRefundClient = buildAccountCheckRefundClient(firstAccount.getId());
  }


  @Test
  public void checkPayAmountShouldBeAllowed() {
    actionShouldBeAllowed(false, accountsCheckPayClient, "3.32");
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
}
