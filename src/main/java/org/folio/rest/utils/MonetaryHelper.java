package org.folio.rest.utils;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MonetaryHelper {
  private static final int SCALE = 2;
  private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  private MonetaryHelper(){
    throw new UnsupportedOperationException("Do not instantiate");
  }

  public static BigDecimal monetize(String value) {
    return monetize(from(value));
  }

  public static BigDecimal monetize(Double value) {
    return monetize(from(value));
  }

  public static BigDecimal monetize(BigDecimal value) {
    return value == null ? null : value.setScale(SCALE, ROUNDING_MODE);
  }

  public static boolean isZero(BigDecimal value) {
    return ZERO.compareTo(value) == 0;
  }

  public static boolean isPositive(BigDecimal value) {
    return ZERO.compareTo(value) < 0;
  }

  public static boolean isNegative(BigDecimal value) {
    return ZERO.compareTo(value) > 0;
  }

  public static boolean isNotPositive(BigDecimal value) {
    return !isPositive(value);
  }

  public static boolean isNotNegative(BigDecimal value) {
    return !isNegative(value);
  }

  private static BigDecimal from(String value) {
    return value == null ? null : new BigDecimal(value);
  }

  private static BigDecimal from(Double value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }

  static String formatCurrency(Double value) {
    BigDecimal bigDecimal = monetize(value);
    return bigDecimal == null ? null : bigDecimal.toString();
  }

}
