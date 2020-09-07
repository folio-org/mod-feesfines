package org.folio.rest.service.action;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;

public class ActionContext {
  private final String accountId;
  private final ActionRequest request;
  private final List<Feefineaction> feeFineActions;
  private MonetaryValue requestedAmount;
  private Account account;
  private boolean isFullAction;
  private boolean shouldCloseAccount;

  public ActionContext(String accountId, ActionRequest request) {
    this.accountId = accountId;
    this.request = request;
    this.feeFineActions = new ArrayList<>();
  }

  public ActionContext withAccount(Account account) {
    this.account = account;
    return this;
  }

  public ActionContext withFeeFineAction(Feefineaction feeFineAction) {
    this.feeFineActions.add(feeFineAction);
    return this;
  }

  public ActionContext withRequestedAmount(MonetaryValue requestedAmount) {
    this.requestedAmount = requestedAmount;
    return this;
  }

  public ActionContext withIsFullAction(boolean isFullAction) {
    this.isFullAction = isFullAction;
    return this;
  }

  public ActionContext withShouldCloseAccount(boolean shouldCloseAccount) {
    this.shouldCloseAccount = shouldCloseAccount;
    return this;
  }

  public String getAccountId() {
    return accountId;
  }

  public ActionRequest getRequest() {
    return request;
  }

  public Account getAccount() {
    return account;
  }

  public List<Feefineaction> getFeeFineActions() {
    return feeFineActions;
  }

  public MonetaryValue getRequestedAmount() {
    return requestedAmount;
  }

  public boolean isFullAction() {
    return isFullAction;
  }

  public boolean isShouldCloseAccount() {
    return shouldCloseAccount;
  }
}
