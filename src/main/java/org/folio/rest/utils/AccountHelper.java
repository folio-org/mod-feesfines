package org.folio.rest.utils;

import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.domain.FeeFineStatus.OPEN;
import static org.apache.commons.lang.StringUtils.defaultString;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.tools.utils.MetadataUtil;

public class AccountHelper {
  private static final Logger log = LogManager.getLogger(AccountHelper.class);

  public static final String PATRON_COMMENTS_KEY = "PATRON";
  public static final String STAFF_COMMENTS_KEY = "STAFF";

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

  public static Map<String, String> parseFeeFineComments(String comments) {
    return Arrays.stream(defaultString(comments).split(" \n "))
      .map(s -> s.split(" : "))
      .filter(arr -> arr.length == 2)
      .map(strings -> Pair.of(strings[0], strings[1]))
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (s, s2) -> s));
  }

  public static void populateMetadata(Account account, Map<String, String> headers) {
    try {
      MetadataUtil.populateMetadata(account, headers);
    } catch (ReflectiveOperationException e) {
      log.error("Failed to populate Metadata for Account {}: {}", account.getId(), e.getMessage());
    }
  }
}
