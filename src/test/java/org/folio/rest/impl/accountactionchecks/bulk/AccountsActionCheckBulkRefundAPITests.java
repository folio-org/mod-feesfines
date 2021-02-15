package org.folio.rest.impl.accountactionchecks.bulk;

import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckRefundClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCheckRefundClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;

import org.apache.http.HttpStatus;
import org.folio.rest.impl.accountactionchecks.AccountsActionChecksAPITestsBase;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.utils.ResourceClient;
import org.junit.Before;
import org.junit.Test;

public class AccountsActionCheckBulkRefundAPITests extends AccountsActionChecksAPITestsBase {

  private static final ResourceClient accountsBulkCheckRefundClient = buildAccountBulkCheckRefundClient();
  private ResourceClient accountsCheckRefundClient;

  @Before
  public void setUp() {
    firstAccount = createAccount();
    secondAccount = createAccount();
    accountsCheckRefundClient = buildAccountCheckRefundClient(firstAccount.getId());
  }

  @Test
  public void checkRefundAmountShouldBeAllowed() {
    double expectedRemainingAmount = 0.77;

    final Feefineaction feeFineAction = new Feefineaction()
      .withAccountId(firstAccount.getId())
      .withUserId(firstAccount.getUserId())
      .withAmountAction((REQUESTED_AMOUNT + expectedRemainingAmount) / 2);

    buildFeeFineActionsClient()
      .post(feeFineAction.withTypeAction(PAY.getPartialResult()))
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    buildFeeFineActionsClient()
      .post(feeFineAction.withTypeAction(TRANSFER.getPartialResult()))
      .then()
      .statusCode(HttpStatus.SC_CREATED);

    actionShouldBeAllowed(false, accountsCheckRefundClient,
      String.valueOf(expectedRemainingAmount));

    actionShouldBeAllowed(true, accountsBulkCheckRefundClient,
      String.valueOf(expectedRemainingAmount));
  }

  @Test
  public void checkRefundAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckRefundAmountShouldNotBeAllowedWithExceededAmount(false, accountsCheckRefundClient);
    actionCheckRefundAmountShouldNotBeAllowedWithExceededAmount(true, accountsBulkCheckRefundClient);
  }

  @Test
  public void checkRefundAmountShouldNotBeAllowedWithNegativeAmount() {
    actionShouldNotBeAllowed(false, accountsCheckRefundClient, "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
    actionShouldNotBeAllowed(true, accountsBulkCheckRefundClient, "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkRefundAmountShouldNotBeAllowedWithZeroAmount() {
    actionShouldNotBeAllowed(false, accountsCheckRefundClient, "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
    actionShouldNotBeAllowed(true, accountsBulkCheckRefundClient, "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void checkRefundAmountShouldBeNumeric() {
    actionShouldNotBeAllowed(false, accountsCheckRefundClient, "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
    actionShouldNotBeAllowed(true, accountsBulkCheckRefundClient, "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
  }

  @Test
  public void checkRefundAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckShouldNotFailForNonExistentAccount(false, accountsCheckRefundClient);
    actionCheckShouldNotFailForNonExistentAccount(true, accountsBulkCheckRefundClient);
  }
}
