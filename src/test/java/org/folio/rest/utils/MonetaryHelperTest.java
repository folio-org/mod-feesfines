package org.folio.rest.utils;

import static org.folio.rest.utils.MonetaryHelper.formatCurrency;
import static org.junit.Assert.*;

import org.junit.Test;

public class MonetaryHelperTest {
  @Test
  public void currencyIsFormattedCorrectly() {
    assertNull(formatCurrency(null));
    assertEquals("0.00", formatCurrency(0d));
    assertEquals("1.00", formatCurrency(1d));
    assertEquals("1.20", formatCurrency(1.2));
    assertEquals("1.23", formatCurrency(1.23));
    assertEquals("1.22", formatCurrency(1.224));
    assertEquals("1.23", formatCurrency(1.225));
    assertEquals("1.23", formatCurrency(1.226));
  }
}