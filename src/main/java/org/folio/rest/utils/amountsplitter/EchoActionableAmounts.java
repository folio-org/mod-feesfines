package org.folio.rest.utils.amountsplitter;

import java.util.Map;

import org.folio.rest.domain.MonetaryValue;

public class EchoActionableAmounts implements BulkActionAmountSplitterStrategy {
  @Override
  public Map<String, MonetaryValue> split(MonetaryValue totalRequestedAmount,
    Map<String, MonetaryValue> actionableAmounts) {

    return actionableAmounts;
  }
}
