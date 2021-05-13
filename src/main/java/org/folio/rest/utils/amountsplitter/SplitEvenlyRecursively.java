package org.folio.rest.utils.amountsplitter;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.ActionException;

public class SplitEvenlyRecursively implements BulkActionAmountSplitterStrategy {

  @Override
  public Map<String, MonetaryValue> split(MonetaryValue totalRequestedAmount,
    Map<String, MonetaryValue> actionableAmounts) {

    Map<String, MonetaryValue> result = new HashMap<>();

    if (actionableAmounts.isEmpty()) {
      return result;
    }

    int numberOfAccountsToProcess = actionableAmounts.size();
    BigDecimal amountToDistribute = totalRequestedAmount.getAmount();
    BigDecimal evenlySplitAmount = splitEvenly(amountToDistribute, numberOfAccountsToProcess);


    for (String key : sortByValue(actionableAmounts).keySet()) {
      if (amountToDistribute.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }

      BigDecimal actionableAmount = actionableAmounts.get(key).getAmount();
      numberOfAccountsToProcess--;

      BigDecimal calculatedActionAmount;
      if (actionableAmount.compareTo(evenlySplitAmount) >= 0) {
        calculatedActionAmount = evenlySplitAmount.min(amountToDistribute);
        amountToDistribute = amountToDistribute.subtract(calculatedActionAmount);
      } else {
        calculatedActionAmount = actionableAmount.min(amountToDistribute);
        amountToDistribute = amountToDistribute.subtract(calculatedActionAmount);
        evenlySplitAmount = splitEvenly(amountToDistribute, numberOfAccountsToProcess);
      }

      if (calculatedActionAmount.compareTo(BigDecimal.ZERO) > 0) {
        result.put(key, new MonetaryValue(calculatedActionAmount));
      }
    }

    distributeRemainder(actionableAmounts, result, amountToDistribute);
    validateResult(totalRequestedAmount, result);

    return result;
  }

  private void distributeRemainder(Map<String, MonetaryValue> actionableAmounts,
    Map<String, MonetaryValue> calculatedAmounts, BigDecimal amountToDistribute) {

    MonetaryValue undistributedRemainder = new MonetaryValue(amountToDistribute);
    if (!undistributedRemainder.isPositive()) {
      return;
    }

    final MonetaryValue remainderDistributionIncrement = new MonetaryValue(
      ONE.movePointLeft(amountToDistribute.scale()));
    Set<String> accountIds = calculatedAmounts.keySet();

    for (String accountId : accountIds) {
      MonetaryValue actionableAmount = actionableAmounts.get(accountId);
      MonetaryValue calculatedAmountPlusRemainder = calculatedAmounts.get(accountId)
        .add(remainderDistributionIncrement);

      if (actionableAmount.isGreaterThanOrEquals(calculatedAmountPlusRemainder)) {
        calculatedAmounts.replace(accountId, calculatedAmountPlusRemainder);
        undistributedRemainder = undistributedRemainder.subtract(remainderDistributionIncrement);
      }

      if (!undistributedRemainder.isPositive()) {
        break;
      }
    }
  }

  private void validateResult(MonetaryValue requestedAmount,
    Map<String, MonetaryValue> calculatedAmounts) {

    MonetaryValue totalCalculatedAmount = calculatedAmounts.values().stream()
      .reduce(new MonetaryValue(ZERO), MonetaryValue::add);

    if (requestedAmount.getAmount().compareTo(totalCalculatedAmount.getAmount()) != 0) {
      throw new ActionException("Failed to split requested amount correctly");
    }
  }

  private BigDecimal splitEvenly(BigDecimal amount, int numberOfPieces) {
    return amount.divide(new BigDecimal(numberOfPieces), RoundingMode.FLOOR);
  }

  private static Map<String, MonetaryValue> sortByValue(Map<String, MonetaryValue> map) {
    return map.entrySet()
      .stream()
      .sorted(comparingByValue(comparing(MonetaryValue::getAmount)))
      .collect(toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (e1, e2) -> e1,
        LinkedHashMap::new
      ));
  }
}
