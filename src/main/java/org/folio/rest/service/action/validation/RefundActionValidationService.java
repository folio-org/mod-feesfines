package org.folio.rest.service.action.validation;

import java.util.Map;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.repository.FeeFineActionRepository;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class RefundActionValidationService extends ActionValidationService {
  private final FeeFineActionRepository feeFineActionRepository;

  public RefundActionValidationService(Map<String, String> headers, Context context) {
    super(headers, context);
    this.feeFineActionRepository = new FeeFineActionRepository(headers, context);
  }

  @Override
  protected void validateAccountStatus(Account account) {
    // doing nothing as closed fee/fine can also be refunded
  }

  @Override
  protected Future<Void> validateAmountMaximum(Account account, MonetaryValue requestedAmount) {
    return getRefundableAmount(account)
      .map(refundableAmount -> {
        if (requestedAmount.isGreaterThan(refundableAmount)) {
          throw new FailedValidationException(
            "Refund amount must be greater than zero and less than or equal to Selected amount");
        }
        return null;
      });
  }

  private Future<MonetaryValue> getRefundableAmount(Account account) {
    return feeFineActionRepository.findRefundableActionsForAccount(account.getId())
      .map(actions -> actions.stream()
        .mapToDouble(Feefineaction::getAmountAction)
        .sum())
      .map(MonetaryValue::new);
  }

  @Override
  protected Future<MonetaryValue> calculateRemainingBalance(Account account,
    MonetaryValue requestedAmount) {

    return getRefundableAmount(account)
      .map(refundableAmount -> refundableAmount.subtract(requestedAmount));
  }

}
