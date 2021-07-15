package org.folio.rest.service.report.context;

import java.util.List;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;

import io.vertx.core.Future;

public interface HasAccountInfo {
  Account getAccountById(String accountId);
  Future<Void> updateAccountContextWithActions(String accountId, List<Feefineaction> actions);
  boolean isAccountContextCreated(String accountId);
  List<Feefineaction> getAccountFeeFineActions(String accountId);
}
