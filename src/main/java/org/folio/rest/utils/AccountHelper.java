package org.folio.rest.utils;

import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.domain.FeeFineStatus.OPEN;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.tools.utils.MetadataUtil;

public class AccountHelper {
  private static final Logger log = LogManager.getLogger(AccountHelper.class);

  private AccountHelper() {
    throw new UnsupportedOperationException("Utility class, do not instantiate");
  }

  public static boolean isClosedAndHasZeroRemainingAmount(Account account) {
    return isClosed(account) && account.getRemaining().isZero();
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

  public static void populateMetadata(Account account, Map<String, String> headers) {
    try {
      MetadataUtil.populateMetadata(account, headers);
    } catch (ReflectiveOperationException e) {
      log.error("Failed to populate Metadata for Account {}: {}", account.getId(), e.getMessage());
    }
  }
}
