package org.folio.rest.utils;

import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.domain.FeeFineStatus.OPEN;

import java.math.BigDecimal;

import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Status;

public class AccountHelper {
  private AccountHelper() {
    throw new UnsupportedOperationException("Utility class, do not instantiate");
  }

  public static boolean isClosedAndHasZeroRemainingAmount(Account feeFine) {
    return isClosed(feeFine) && hasZeroRemainingAmount(feeFine);
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

  public static boolean hasZeroRemainingAmount(Account account) {
    final BigDecimal remaining = MonetaryHelper.monetize(account.getRemaining());
    return remaining == null || MonetaryHelper.isZero(remaining);
  }

}
