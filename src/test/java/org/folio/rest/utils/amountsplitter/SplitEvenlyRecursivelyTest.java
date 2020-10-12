package org.folio.rest.utils.amountsplitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.test.support.EntityBuilder;
import org.joda.time.DateTime;
import org.junit.Test;

public class SplitEvenlyRecursivelyTest {

  @Test
  public void shouldBeSplitCorrectlyWithFullAmountsForSomeFines() {
    MonetaryValue requestedAmount = new MonetaryValue(200.0);
    List<Account> accounts = buildAccounts(100, 1, 150);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmountsEqualToRemaining(accounts);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, accounts, actionableAmounts);

    assertEquals(3, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 1.0));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 99.5));
  }

  @Test
  public void shouldBeSplitCorrectlyWithPartialAmountsForAllFines() {
    MonetaryValue requestedAmount = new MonetaryValue(50.0);
    List<Account> accounts = buildAccounts(100, 100, 100);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmountsEqualToRemaining(accounts);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, accounts, actionableAmounts);

    assertEquals(3, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 16.66));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 16.67));
  }

  private List<Account> buildAccounts(double... amounts) {
    return Arrays.stream(amounts)
      .mapToObj(amount -> EntityBuilder.buildAccount()
        .withAmount(amount)
        .withRemaining(amount))
      .collect(Collectors.toList());
  }

  private Map<String, MonetaryValue> buildActionableAmountsEqualToRemaining(List<Account> accounts) {
    return accounts.stream()
      .collect(Collectors.toMap(Account::getId,
        account -> new MonetaryValue(BigDecimal.valueOf(account.getRemaining()))));
  }

  private long numberOfValueOccurrences(Map<String, MonetaryValue> splitAmount, double value) {
    return splitAmount.values().stream()
      .mapToDouble(MonetaryValue::toDouble)
      .filter(Double.valueOf(value)::equals)
      .count();
  }
}
