package org.folio.rest.domain;

import org.folio.rest.jaxrs.model.Account;

import io.vertx.core.json.JsonObject;

public final class AccountWithLoanClosedEvent {
  private final String loanId;
  private final String accountId;

  public AccountWithLoanClosedEvent(String loanId, String accountId) {
    this.loanId = loanId;
    this.accountId = accountId;
  }

  public String getLoanId() {
    return loanId;
  }

  public String getAccountId() {
    return accountId;
  }

  public String toJsonString() {
    return JsonObject.mapFrom(this).toString();
  }

  public static AccountWithLoanClosedEvent forAccount(Account account) {
    return new AccountWithLoanClosedEvent(account.getLoanId(), account.getId());
  }
}
