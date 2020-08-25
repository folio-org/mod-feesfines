package org.folio.rest.service.action.validation;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

public class RefundActionValidationService extends ActionValidationService {

  public RefundActionValidationService(AccountRepository accountRepository) {
    super(accountRepository);
  }

  @Override
  protected void validateAmountMaximum(Account account, MonetaryValue requestedAmount) {
    MonetaryValue maxAllowedRefund = new MonetaryValue(account.getAmount())
      .subtract(new MonetaryValue(account.getRemaining()));

    if (requestedAmount.isGreaterThan(maxAllowedRefund)) {
      throw new FailedValidationException("Requested amount exceeds maximum refund amount");
    }
  }

  @Override
  protected MonetaryValue calculateRemainingBalance(MonetaryValue requestedAmount,
    MonetaryValue remainingAmount) {

    return remainingAmount.add(requestedAmount);
  }

}
