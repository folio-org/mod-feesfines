package org.folio.rest.service.report.context;

import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;

public interface HasLoanInfo {
  Loan getLoanByAccountId(String accountId);
  void updateAccountCtxWithLoan(String accountId, Loan loan);
  void updateAccountCtxWithLoanPolicy(String accountId, LoanPolicy loanPolicy);
  void updateAccountCtxWithOverdueFinePolicy(String accountId, OverdueFinePolicy overdueFinePolicy);
  void updateAccountCtxWithLostItemFeePolicy(String accountId, LostItemFeePolicy lostItemFeePolicy);
}
