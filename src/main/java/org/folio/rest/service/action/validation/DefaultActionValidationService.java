package org.folio.rest.service.action.validation;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

public class DefaultActionValidationService extends ActionValidationService {

  public DefaultActionValidationService(AccountRepository accountRepository) {
    super(accountRepository);
  }

  @Override
  protected void validateAmountMaximum(Account account, MonetaryValue requestedAmount) {
    if (requestedAmount.isGreaterThan(new MonetaryValue(account.getRemaining()))) {
      throw new FailedValidationException("Requested amount exceeds remaining amount");
    }
  }

  @Override
  protected MonetaryValue calculateRemainingBalance(MonetaryValue requestedAmount,
    MonetaryValue remainingAmount) {

    return remainingAmount.subtract(requestedAmount);
  }

}
