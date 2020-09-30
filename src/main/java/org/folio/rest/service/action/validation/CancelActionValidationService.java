package org.folio.rest.service.action.validation;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Collections.singletonMap;
import static org.folio.rest.utils.AccountHelper.isClosed;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class CancelActionValidationService extends ActionValidationService {

  public CancelActionValidationService(Map<String, String> headers, Context context) {
    super(headers, context);
  }

  @Override
  protected void validateAccountStatuses(List<Account> accounts) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Future<ActionValidationResult> validate(String accountId, Account account,
    String rawAmount) {

    validateIfAccountsExist(singletonMap(accountId, account));

    if (isClosed(account)) {
      throw new FailedValidationException("Fee/fine is already closed");
    }

    MonetaryValue remainingAmount = new MonetaryValue(BigDecimal.ZERO);

    return succeededFuture(new ActionValidationResult(remainingAmount, remainingAmount));
  }

  @Override
  protected Future<Void> validateAmountMaximum(List<Account> accounts,
    MonetaryValue requestedAmount) {

    throw new UnsupportedOperationException();
  }

  @Override
  protected Future<MonetaryValue> calculateRemainingBalance(List<Account> accounts,
    MonetaryValue requestedAmount) {

    throw new UnsupportedOperationException();
  }
}
