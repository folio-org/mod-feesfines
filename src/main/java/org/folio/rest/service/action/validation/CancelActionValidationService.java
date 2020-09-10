package org.folio.rest.service.action.validation;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.AccountHelper.isClosed;

import java.util.Map;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class CancelActionValidationService extends ActionValidationService {

  public CancelActionValidationService(AccountRepository accountRepository) {
    super(accountRepository);
  }

  public CancelActionValidationService(Map<String, String> headers, Context context) {
    super(headers, context);
  }

  @Override
  protected Future<Void> validateAmountMaximum(Account account, MonetaryValue requestedAmount) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected MonetaryValue calculateRemainingBalance(Account account, MonetaryValue requestedAmount) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void validateAccountStatus(Account account) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Future<ActionValidationResult> validate(Account account, String rawAmount) {

    validateIfAccountExists(account);

    if (isClosed(account)) {
      throw new FailedValidationException("Account is already closed");
    }

    MonetaryValue remainingAmount = new MonetaryValue("0.00");

    return succeededFuture(new ActionValidationResult(
      remainingAmount.getAmount().toString(), remainingAmount.toString()));
  }
}
