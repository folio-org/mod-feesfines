package org.folio.rest.service.action;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.domain.ActionRequest;
import org.folio.rest.domain.BulkActionRequest;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;

public class BulkActionContext {
  private final List<String> accountIds;
  private final BulkActionRequest request;
  private final List<Feefineaction> feeFineActions;
  private MonetaryValue requestedAmount;
  private List<Account> accounts;
  private boolean shouldCloseAccount;

  public BulkActionContext(List<String> accountIds, BulkActionRequest request) {
    this.accountIds = accountIds;
    this.request = request;
    this.feeFineActions = new ArrayList<>();
  }

  public BulkActionContext withAccounts(List<Account> accounts) {
    this.accounts = accounts;
    return this;
  }

  public BulkActionContext withFeeFineAction(Feefineaction feeFineAction) {
    this.feeFineActions.add(feeFineAction);
    return this;
  }

  public BulkActionContext withRequestedAmount(MonetaryValue requestedAmount) {
    this.requestedAmount = requestedAmount;
    return this;
  }

  public BulkActionContext withShouldCloseAccount(boolean shouldCloseAccount) {
    this.shouldCloseAccount = shouldCloseAccount;
    return this;
  }

  public List<String> getAccountIds() {
    return accountIds;
  }

  public ActionRequest getRequest() {
    return request;
  }

  public List<Account> getAccounts() {
    return accounts;
  }

  public List<Feefineaction> getFeeFineActions() {
    return feeFineActions;
  }

  public MonetaryValue getRequestedAmount() {
    return requestedAmount;
  }

  public boolean isShouldCloseAccount() {
    return shouldCloseAccount;
  }
}
