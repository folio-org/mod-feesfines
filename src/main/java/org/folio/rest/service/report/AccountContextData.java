package org.folio.rest.service.report;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@AllArgsConstructor
@Getter
@With
public class AccountContextData {
  final Account account;
  final List<Feefineaction> actions;
  final HoldingsRecord holdingsRecord;
  final Instance instance;
  final String effectiveLocation;
  final Loan loan;
  final LoanPolicy loanPolicy;
  final OverdueFinePolicy overdueFinePolicy;
  final LostItemFeePolicy lostItemFeePolicy;

  public AccountContextData() {
    account = null;
    actions = new ArrayList<>();
    holdingsRecord = null;
    instance = null;
    effectiveLocation = "";
    loan = null;
    loanPolicy = null;
    overdueFinePolicy = null;
    lostItemFeePolicy = null;
  }
}
