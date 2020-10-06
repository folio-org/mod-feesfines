package org.folio.rest.impl.accountactionchecks.bulk;

import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckWaiveClient;

import org.folio.rest.domain.Action;
import org.folio.rest.impl.accountactionchecks.AccountsActionChecksAPITestsBase;
import org.folio.rest.utils.ResourceClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AccountsActionBulkCheckPayWaiveTransferAPITests extends AccountsActionChecksAPITestsBase {

  private final Action action;
  private ResourceClient bulkClient;

  private final ResourceClient accountsBulkCheckPayClient = buildAccountBulkCheckPayClient();
  private final ResourceClient accountsBulkCheckWaiveClient = buildAccountBulkCheckWaiveClient();
  private final ResourceClient accountsBulkCheckTransferClient = buildAccountBulkCheckTransferClient();


  @Before
  public void setUp() {
    firstAccount = createAccount();
    secondAccount = createAccount();
    bulkClient = getAccountsBulkClient();
  }

  public AccountsActionBulkCheckPayWaiveTransferAPITests(Action action) {
    this.action = action;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Object[] parameters() {
    return new Object[]{PAY, WAIVE, TRANSFER};
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
        throw new IllegalArgumentException(
          "Failed to get ResourceClient for action: " + action.name());
    }
  }

  @Test
  public void bulkCheckAmountShouldBeAllowed() {
    actionShouldBeAllowed(true, bulkClient, "7.87");
  }

  @Test
  public void bulkCheckAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(true, bulkClient);
  }

  @Test
  public void bulkCheckAmountShouldNotBeAllowedWithNegativeAmount() {
    actionShouldNotBeAllowed(true, bulkClient, "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void bulkCheckAmountShouldNotBeAllowedWithZeroAmount() {
    actionShouldNotBeAllowed(true, bulkClient, "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @Test
  public void bulkCheckAmountShouldBeNumeric() {
    actionShouldNotBeAllowed(true, bulkClient, "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
  }

  @Test
  public void bulkCheckAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckShouldNotFailForNonExistentAccount(true, bulkClient);
  }

  @Test
  public void bulkCheckAmountShouldNotBeAllowedForClosedAccount() {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(true, bulkClient);
  }
}
