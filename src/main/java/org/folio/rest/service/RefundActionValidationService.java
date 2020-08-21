package org.folio.rest.service;

import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

public class RefundActionValidationService extends DefaultActionValidationService {

  public RefundActionValidationService(AccountRepository accountRepository) {
    super(accountRepository);
  }

  @Override
  protected void validateAmountMaximum(Account account, double requestedAmount) {
    if (requestedAmount > account.getAmount() - account.getRemaining()) {
      throw new FailedValidationException("Requested amount exceeds maximum refund amount");
    }
  }

  @Override
  protected double calculateRemainingBalance(Account account, double requestedAmount) {
    return account.getRemaining() + requestedAmount;
  }
}
