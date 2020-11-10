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
  public void shouldCoverOneAccountFullyAndDistributeRemainderEvenly() {
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
  public void shouldCoverOneAccountFullyAndDistributeRemainderAsEvenlyAsPossible() {
    MonetaryValue requestedAmount = new MonetaryValue(50.0);
    List<Account> accounts = buildAccounts(15, 15, 15, 15, 7.5);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmountsEqualToRemaining(accounts);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, accounts, actionableAmounts);

    assertEquals(5, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 7.5));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 10.63));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 10.62));
  }

  @Test
  public void shouldCoverAllAccountsPartiallyWithOnePieceSmallerThanTheRest() {
    MonetaryValue requestedAmount = new MonetaryValue(50.0);
    List<Account> accounts = buildAccounts(100, 100, 100);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmountsEqualToRemaining(accounts);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, accounts, actionableAmounts);

    assertEquals(3, splitAmount.size());
    assertEquals(2, numberOfValueOccurrences(splitAmount, 16.67));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 16.66));
  }

  @Test
  public void shouldCoverAllAccountsPartiallyWithOnePieceLargerThanTheRest() {
    MonetaryValue requestedAmount = new MonetaryValue(15.01);
    List<Account> accounts = buildAccounts(5, 6, 7, 8);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmountsEqualToRemaining(accounts);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, accounts, actionableAmounts);

    assertEquals(4, splitAmount.size());
    assertEquals(3, numberOfValueOccurrences(splitAmount, 3.75));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 3.76));
  }

  @Test
  public void shouldFullyCoverMultipleAccountsAndDistributeRemainderAsEvenlyAsPossible1() {
    MonetaryValue requestedAmount = new MonetaryValue(24.56);
    List<Account> accounts = buildAccounts(1.23, 2.34, 3.45, 4.56, 5.67, 6.78, 7.89);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmountsEqualToRemaining(accounts);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, accounts, actionableAmounts);

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
    List<Account> accounts = buildAccounts(1.23, 2.34, 3.45, 4.56, 5.67, 6.78, 7.89);
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmountsEqualToRemaining(accounts);

    Map<String, MonetaryValue> splitAmount = new SplitEvenlyRecursively()
      .split(requestedAmount, accounts, actionableAmounts);

    assertEquals(7, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 1.23));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 2.34));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 3.45));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 4.37));
    assertEquals(2, numberOfValueOccurrences(splitAmount, 4.38));
  }

  private List<Account> buildAccounts(double... amounts) {
    return Arrays.stream(amounts)
      .mapToObj(amount -> EntityBuilder.buildAccount(amount, amount))
      .collect(Collectors.toList());
  }

  private Map<String, MonetaryValue> buildActionableAmountsEqualToRemaining(List<Account> accounts) {
    return accounts.stream()
      .collect(Collectors.toMap(Account::getId,
        account -> new MonetaryValue(account.getRemaining())));
  }

  private long numberOfValueOccurrences(Map<String, MonetaryValue> splitAmount, double value) {
    return splitAmount.values().stream()
      .mapToDouble(MonetaryValue::toDouble)
      .filter(Double.valueOf(value)::equals)
      .count();
  }
}
