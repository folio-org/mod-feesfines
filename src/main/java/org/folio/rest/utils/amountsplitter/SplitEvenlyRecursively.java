package org.folio.rest.utils.amountsplitter;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.ActionException;
import org.folio.rest.jaxrs.model.Account;

public class SplitEvenlyRecursively implements BulkActionAmountSplitterStrategy {
  @Override
  public Map<String, MonetaryValue> split(MonetaryValue totalRequestedAmount,
    Collection<Account> accounts, Map<String, MonetaryValue> actionableAmounts) {

    int numberOfAccountsToProcess = accounts.size();
    BigDecimal amountToDistribute = totalRequestedAmount.getAmount();
    BigDecimal evenlySplitAmount = splitEvenly(amountToDistribute, numberOfAccountsToProcess);

    List<Account> accountsSorted = accounts.stream()
      .sorted(Comparator.comparingLong(
        account -> actionableAmounts.get(account.getId()).getAmount().longValue()))
      .collect(Collectors.toList());

    Map<String, MonetaryValue> result = new HashMap<>();

    for (Account account : accountsSorted) {
      if (amountToDistribute.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }

      BigDecimal actionableAmount = actionableAmounts.get(account.getId()).getAmount();
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
        result.put(account.getId(), new MonetaryValue(calculatedActionAmount));
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
}
