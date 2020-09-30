package org.folio.rest.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.assertj.core.data.MapEntry;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;

public class BulkActionAmountSplitter {
  public static Map<String, MonetaryValue> splitEqually(MonetaryValue totalRequestedAmount,
    List<Account> accounts, Map<String, MonetaryValue> actionableAmounts) {

    BigDecimal equalAmount = totalRequestedAmount.getAmount().divide(
      new BigDecimal(accounts.size()), RoundingMode.HALF_EVEN);

    AtomicReference<BigDecimal> totalExtraAmount = new AtomicReference<>(accounts.stream()
      .map(account -> {
        BigDecimal actionableAmount = actionableAmounts.get(account.getId()).getAmount();
        if (equalAmount.compareTo(actionableAmount) > 0) {
          return equalAmount.subtract(actionableAmount);
        } else {
          return BigDecimal.ZERO;
        }
      })
      .reduce(BigDecimal::add)
      .orElse(BigDecimal.ZERO));

    return accounts.stream()
      .sorted(Comparator.comparingLong(account -> account.getDateCreated().getTime()))
      .map(account -> {
        BigDecimal actionableAmount = actionableAmounts.get(account.getId()).getAmount();
        BigDecimal surplus = equalAmount.subtract(actionableAmount);

        BigDecimal calculatedActionAmount;
        if (surplus.compareTo(BigDecimal.ZERO) >= 0) {
          calculatedActionAmount = actionableAmount;
        }
        else {
          BigDecimal additionalAmount = surplus.abs().min(totalExtraAmount.get());
          totalExtraAmount.set(totalExtraAmount.get().subtract(additionalAmount));
          calculatedActionAmount = equalAmount.add(additionalAmount);
        }

        return MapEntry.entry(account.getId(), new MonetaryValue(calculatedActionAmount));
      })
      .collect(Collectors.toMap(MapEntry::getKey, MapEntry::getValue));
  }
}
