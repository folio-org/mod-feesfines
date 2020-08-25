package org.folio.rest.service.action.validation;

import org.folio.rest.jaxrs.model.Account;

import io.vertx.core.Future;

public interface ActionValidationService {
  Future<ActionValidationResult> validate(Account account, String amount);
  Future<ActionValidationResult> validate(String accountId, String amount);
}
