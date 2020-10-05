package org.folio.rest.utils.amountsplitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.test.support.EntityBuilder;
import org.joda.time.DateTime;
import org.junit.Test;

public class SplitEquallyDistributeChronologicallyTest {

  @Test
  public void partialAmountShouldBeSplitCorrectly() {
    MonetaryValue requestedAmount = new MonetaryValue(50.0);
    List<Account> accounts = buildAccountsEqualSequential(10, 1, 1, new Date());
    Map<String, MonetaryValue> actionableAmounts = buildActionableAmountsEqualToRemaining(accounts);

    Map<String, MonetaryValue> splitAmount = new SplitEquallyDistributeChronologically()
      .split(requestedAmount, accounts, actionableAmounts);

    assertEquals(10, splitAmount.size());
    assertEquals(1, numberOfValueOccurrences(splitAmount, 1.0));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 2.0));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 3.0));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 4.0));
    assertEquals(3, numberOfValueOccurrences(splitAmount, 5.0));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 6.0));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 9.0));
    assertEquals(1, numberOfValueOccurrences(splitAmount, 10.0));
  }

  private List<Account> buildAccountsEqualSequential(int number, double startAmount, double step,
    Date startDate) {

    return IntStream.range(0, number)
      .mapToObj(num -> EntityBuilder.buildAccount()
        .withAmount(startAmount + num * step)
        .withRemaining(startAmount + num * step)
        .withMetadata(new Metadata().withCreatedDate(new DateTime(startDate).minusDays(num + 1).toDate()))
      )
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
