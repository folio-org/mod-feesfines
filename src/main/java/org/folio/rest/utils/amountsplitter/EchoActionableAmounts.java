package org.folio.rest.utils.amountsplitter;

import java.util.List;
import java.util.Map;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;

public class EchoActionableAmounts implements BulkActionAmountSplitterStrategy {
  @Override
  public Map<String, MonetaryValue> split(MonetaryValue totalRequestedAmount,
    List<Account> accounts, Map<String, MonetaryValue> actionableAmounts) {

    return actionableAmounts;
  }
}
