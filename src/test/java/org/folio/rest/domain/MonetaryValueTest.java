package org.folio.rest.domain;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class MonetaryValueTest {

  @Test(expected = NullPointerException.class)
  public void stringConstructorThrowsExceptionWhenAmountIsNull() {
    new MonetaryValue((String) null);
  }

  @Test(expected = NullPointerException.class)
  public void doubleConstructorThrowsExceptionWhenAmountIsNull() {
    new MonetaryValue((Double) null);
  }

  @Test(expected = NullPointerException.class)
  public void bigDecimalConstructorThrowsExceptionWhenAmountIsNull() {
    new MonetaryValue((BigDecimal) null);
  }

  @Test
  @Parameters({ "0", "0.0", "0.00", "0.000", "0.005", "0.000000000000001" })
  public void monetaryValueIsZero(String value) {
    assertTrue(new MonetaryValue(value).isZero());
    assertTrue(new MonetaryValue("-" + value).isZero());
  }

  @Test
  @Parameters({ "1", "0.006", "0.0051", "0.0050000000000001" })
  public void monetaryValueIsNotZero(String value) {
    assertFalse(new MonetaryValue(value).isZero());
    assertFalse(new MonetaryValue("-" + value).isZero());
  }

  @Test
  @Parameters({ "1", "0.1", "0.01", "0.006", "0.0051", "0.0050000000000001" })
  public void monetaryValueIsPositive(String value) {
    assertTrue(new MonetaryValue(value).isPositive());
  }

  @Test
  @Parameters({ "-1", "0", "0.00", "0.000", "0.005", "0.000999999" })
  public void monetaryValueIsNotPositive(String value) {
    assertFalse(new MonetaryValue(value).isPositive());
  }

  @Test
  @Parameters({ "-1", "-0.1", "-0.01", "-0.006", "-0.0051", "-0.0050000000000001" })
  public void monetaryValueIsNegative(String value) {
    assertTrue(new MonetaryValue(value).isNegative());
  }

  @Test
  @Parameters({ "1", "0", "0.00", "0.000", "0.005", "-0.005", "0.000000000001", "-0.000000000001" })
  public void monetaryValueIsNotNegative(String value) {
    assertFalse(new MonetaryValue(value).isNegative());
  }

  @Test
  @Parameters({
    "0, 0.00",
    "0.0, 0.00",
    "0.00, 0.00",
    "0.000, 0.00",

    "-0, 0.00",
    "-0.0, 0.00",
    "-0.00, 0.00",
    "-0.000, 0.00",

    "1, 1.00",
    "0.1, 0.10",
    "0.01, 0.01",
    "0.001, 0.00",

    "-1, -1.00",
    "-0.1, -0.10",
    "-0.01, -0.01",
    "-0.001, 0.00",

    "0.005, 0.00",
    "0.0051, 0.01",
    "0.0050000000001, 0.01",

    "-0.005, 0.00",
    "-0.0051, -0.01",
    "-0.0050000000001, -0.01",

    "0.015, 0.02",
    "0.0149, 0.01",
    "0.0150000000001, 0.02",

    "-0.015, -0.02",
    "-0.0149, -0.01",
    "-0.0150000000001, -0.02",
  })
  public void toStringTest(String source, String expectedResult) {
    assertEquals(expectedResult, new MonetaryValue(source).toString());
  }

  @Test
  public void fromDouble() {
    MonetaryValue a = new MonetaryValue(1.11123d);
    a.add(MonetaryValue.ZERO);
  }

  @Test
  @Parameters({
    "0, 0.0",
    "0.0, 0.0",
    "0.00, 0.0",
    "0.000, 0.0",

    "-0, 0.0",
    "-0.0, 0.0",
    "-0.00, 0.0",
    "-0.000, 0.0",

    "1, 1.0",
    "0.1, 0.1",
    "0.01, 0.01",
    "0.001, 0.001",

    "-1, -1.0",
    "-0.1, -0.1",
    "-0.01, -0.01",
    "-0.001, -0.001",

    "0.005, 0.005",
    "0.0051, 0.0051",
    "0.0050000000001, 0.0050000000001",

    "-0.005, -0.005",
    "-0.0051, -0.0051",
    "-0.0050000000001, -0.0050000000001",

    "0.015, 0.015",
    "0.0149, 0.0149",
    "0.0150000000001, 0.0150000000001",

    "-0.015, -0.015",
    "-0.0149, -0.0149",
    "-0.0150000000001, -0.0150000000001",

    "10, 10.0",
    "10.0, 10.0",
    "10.00, 10.0",
    "100, 100.0",
    "100.0, 100.0",
    "100.00, 100.0",
  })
  public void toStringOriginalAmount(String source, String expectedResult) {
    assertEquals(expectedResult, new MonetaryValue(source).toStringOriginalAmount());
  }
}
