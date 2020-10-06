package org.folio.rest.utils.amountsplitter;

import java.util.List;
import java.util.Map;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;

public interface BulkActionAmountSplitterStrategy {
  Map<String, MonetaryValue> split(MonetaryValue totalRequestedAmount, List<Account> accounts,
    Map<String, MonetaryValue> actionableAmounts);
}
