package org.folio.rest.utils.amountsplitter;

import java.util.Map;

import org.folio.rest.domain.MonetaryValue;

public interface BulkActionAmountSplitterStrategy {
  Map<String, MonetaryValue> split(MonetaryValue totalRequestedAmount,
    Map<String, MonetaryValue> actionableAmounts);
}
