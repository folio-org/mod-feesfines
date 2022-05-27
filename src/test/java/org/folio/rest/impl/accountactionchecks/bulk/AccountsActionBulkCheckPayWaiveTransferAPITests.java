package org.folio.rest.impl.accountactionchecks.bulk;

import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckPayClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckTransferClient;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCheckWaiveClient;

import java.util.stream.Stream;

import org.folio.rest.domain.Action;
import org.folio.rest.impl.accountactionchecks.AccountsActionChecksAPITestsBase;
import org.folio.rest.utils.ResourceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AccountsActionBulkCheckPayWaiveTransferAPITests extends AccountsActionChecksAPITestsBase {

  private final ResourceClient accountsBulkCheckPayClient = buildAccountBulkCheckPayClient();
  private final ResourceClient accountsBulkCheckWaiveClient = buildAccountBulkCheckWaiveClient();
  private final ResourceClient accountsBulkCheckTransferClient = buildAccountBulkCheckTransferClient();

  @BeforeEach
  public void setUp() {
    firstAccount = createAccount();
    secondAccount = createAccount();
  }

  public static Stream<Arguments> parameters() {
    return Stream.of(Arguments.of(PAY), Arguments.of(WAIVE), Arguments.of(TRANSFER));
  }

  private ResourceClient getAccountsBulkClient(Action action) {
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

  @ParameterizedTest
  @MethodSource("parameters")
  public void bulkCheckAmountShouldBeAllowed(Action action) {
    actionShouldBeAllowed(true, getAccountsBulkClient(action), "7.87");
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void bulkCheckAmountShouldNotBeAllowedWithExceededAmount(Action action) {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(true, getAccountsBulkClient(action));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void bulkCheckAmountShouldNotBeAllowedWithNegativeAmount(Action action) {
    actionShouldNotBeAllowed(true, getAccountsBulkClient(action), "-5.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void bulkCheckAmountShouldNotBeAllowedWithZeroAmount(Action action) {
    actionShouldNotBeAllowed(true, getAccountsBulkClient(action), "0.0",
      ERROR_MESSAGE_MUST_BE_POSITIVE);
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void bulkCheckAmountShouldBeNumeric(Action action) {
    actionShouldNotBeAllowed(true, getAccountsBulkClient(action), "abc",
      ERROR_MESSAGE_INVALID_AMOUNT);
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void bulkCheckAmountShouldNotFailForNonExistentAccount(Action action) {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckShouldNotFailForNonExistentAccount(true, getAccountsBulkClient(action));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  public void bulkCheckAmountShouldNotBeAllowedForClosedAccount(Action action) {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(true, getAccountsBulkClient(action));
  }
}
