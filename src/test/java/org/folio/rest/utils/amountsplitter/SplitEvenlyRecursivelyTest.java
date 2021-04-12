package org.folio.rest.utils.amountsplitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.folio.rest.domain.MonetaryValue;
import org.junit.Test;

public class SplitEvenlyRecursivelyTest {

  @Test
  public void shouldCoverOneAccountFullyAndDistributeRemainderEvenly() {
    MonetaryValue requestedAmount = new MonetaryValue(200.0);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmounts(100.0, 1.0, 150.0);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, actionableAmounts);

    assertEquals(3, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 1.0));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 99.5));
  }

  @Test
  public void shouldCoverOneAccountFullyAndDistributeRemainderAsEvenlyAsPossible() {
    MonetaryValue requestedAmount = new MonetaryValue(50.0);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmounts(
      15.0, 15.0, 15.0, 15.0, 7.5);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, actionableAmounts);

    assertEquals(5, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 7.5));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 10.63));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 10.62));
  }

  @Test
  public void shouldCoverAllAccountsPartiallyWithOnePieceSmallerThanTheRest() {
    MonetaryValue requestedAmount = new MonetaryValue(50.0);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmounts(100.0, 100.0, 100.0);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, actionableAmounts);

    assertEquals(3, splitAmount.size());
    assertEquals(2, numberOfValueOccurrences(splitAmount, 16.67));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 16.66));
  }

  @Test
  public void shouldCoverAllAccountsPartiallyWithOnePieceLargerThanTheRest() {
    MonetaryValue requestedAmount = new MonetaryValue(15.01);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmounts(5.0, 6.0, 7.0, 8.0);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, actionableAmounts);

    assertEquals(4, splitAmount.size());
    assertEquals(3, numberOfValueOccurrences(splitAmount, 3.75));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 3.76));
  }

  @Test
  public void shouldFullyCoverMultipleAccountsAndDistributeRemainderAsEvenlyAsPossible1() {
    MonetaryValue requestedAmount = new MonetaryValue(24.56);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmounts(
      1.23, 2.34, 3.45, 4.56, 5.67, 6.78, 7.89);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, actionableAmounts);

    assertEquals(7, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 1.23));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 2.34));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 3.45));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 4.38));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 4.39));
  }

  @Test
  public void shouldFullyCoverMultipleAccountsAndDistributeRemainderAsEvenlyAsPossible2() {
    MonetaryValue requestedAmount = new MonetaryValue(24.52);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmounts(
      1.23, 2.34, 3.45, 4.56, 5.67, 6.78, 7.89);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, actionableAmounts);

    assertEquals(7, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 1.23));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 2.34));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 3.45));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 4.37));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 4.38));
  }

  private Map<String, MonetaryValue> buildActionableAmounts(Double... amounts) {
    return Arrays.stream(amounts)
      .collect(Collectors.toMap(
        amount -> UUID.randomUUID().toString(),
        MonetaryValue::new));
  }

  private long numberOfValueOccurrences(Map<String, MonetaryValue> splitAmount, double value) {
    return splitAmount.values().stream()
      .mapToDouble(MonetaryValue::toDouble)
      .filter(Double.valueOf(value)::equals)
      .count();
  }
}
