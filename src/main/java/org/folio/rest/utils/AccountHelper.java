package org.folio.rest.utils;

import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.domain.FeeFineStatus.OPEN;

import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Status;

public class AccountHelper {
  private AccountHelper() {
    throw new UnsupportedOperationException("Utility class, do not instantiate");
  }

  public static boolean isClosedAndHasZeroRemainingAmount(Account account) {
    return isClosed(account) && new MonetaryValue(account.getRemaining()).isZero();
  }

  public static boolean isClosed(Account account) {
    return isInStatus(account, CLOSED);
  }

  public static boolean isOpen(Account account) {
    return isInStatus(account, OPEN);
  }

  private static boolean isInStatus(Account account, FeeFineStatus feeFineStatus) {
    final Status accountStatus = account.getStatus();
    return accountStatus != null
      && feeFineStatus.getValue().equalsIgnoreCase(accountStatus.getName());
  }

}
