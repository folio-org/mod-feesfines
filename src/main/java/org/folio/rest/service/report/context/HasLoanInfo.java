package org.folio.rest.service.report.context;

import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;

public interface HasLoanInfo {
  Loan getLoanByAccountId(String accountId);

  void updateAccountContextWithLoan(String accountId, Loan loan);

  void updateAccountContextWithLoanPolicy(String accountId, LoanPolicy loanPolicy);

  void updateAccountContextWithOverdueFinePolicy(String accountId, OverdueFinePolicy overdueFinePolicy);

  void updateAccountContextWithLostItemFeePolicy(String accountId, LostItemFeePolicy lostItemFeePolicy);
}
