package org.folio.rest.utils;

import java.util.Arrays;
import java.util.List;

import org.folio.rest.jaxrs.model.Feefineaction;

public class FeeFineActionHelper {

  private static final List<String> FEE_FINE_ACTION_TYPES = Arrays.asList(
    "Paid fully", "Paid partially",
    "Waived fully", "Waived partially",
    "Transferred fully", "Transferred partially",
    "Refunded fully", "Refunded partially",
    "Cancelled as error");

  private FeeFineActionHelper() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  public static boolean isCharge(Feefineaction action) {
    return !isAction(action) && action.getPaymentMethod() == null;
  }

  public static boolean isAction(Feefineaction action) {
    return FEE_FINE_ACTION_TYPES.contains(action.getTypeAction());
  }
}
