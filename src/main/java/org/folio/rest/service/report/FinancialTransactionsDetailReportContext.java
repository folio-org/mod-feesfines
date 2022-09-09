package org.folio.rest.service.report;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.util.UuidUtil.isUuid;
import static org.joda.time.DateTimeZone.UTC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.jaxrs.model.ServicePoint;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;
import org.folio.rest.service.report.context.HasItemInfo;
import org.folio.rest.service.report.context.HasLoanInfo;
import org.folio.rest.service.report.context.HasServicePointsInfo;
import org.folio.rest.service.report.context.HasUserInfo;
import org.joda.time.DateTimeZone;

import io.vertx.core.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

@With
@AllArgsConstructor
@Getter
public class FinancialTransactionsDetailReportContext
  implements HasUserInfo, HasItemInfo, HasServicePointsInfo,
  HasLoanInfo {

  final DateTimeZone timeZone;
  final Map<Feefineaction, Account> actionsToAccounts;
  final Map<String, AccountContextData> accountContexts;
  final Map<String, User> users;
  final Map<String, UserGroup> userGroups;
  final Map<String, Item> items;
  final Map<String, ServicePoint> servicePoints;

  public FinancialTransactionsDetailReportContext() {
    timeZone = UTC;
    actionsToAccounts = new HashMap<>();
    accountContexts = new HashMap<>();
    users = new HashMap<>();
    userGroups = new HashMap<>();
    items = new HashMap<>();
    servicePoints = new HashMap<>();
  }

  AccountContextData getAccountContextById(String accountId) {
    if (accountId == null) {
      return null;
    }

    if (accountContexts.containsKey(accountId)) {
      return accountContexts.get(accountId);
    }

    Account account = actionsToAccounts.values().stream()
      .filter(a -> accountId.equals(a.getId()))
      .findFirst()
      .orElse(null);

    if (account != null) {
      AccountContextData accountContext = new AccountContextData().withAccount(
        account);
      accountContexts.put(accountId, accountContext);
      return accountContext;
    }

    return null;
  }

  @Override
  public Account getAccountById(String accountId) {
    AccountContextData accountContextData = getAccountContextById(
      accountId);
    return accountContextData == null ? null : accountContextData.account;
  }

  @Override
  public Item getItemByAccountId(String accountId) {
    Account account = getAccountById(accountId);

    if (account != null) {
      String itemId = account.getItemId();
      if (itemId != null && items.containsKey(itemId)) {
        return items.get(itemId);
      }
    }

    return null;
  }

  @Override
  public User getUserByAccountId(String accountId) {
    Account account = getAccountById(accountId);

    if (account != null && isUuid(account.getUserId())) {
      return users.get(account.getUserId());
    }

    return null;
  }

  @Override
  public UserGroup getUserGroupByAccountId(String accountId) {
    User user = getUserByAccountId(accountId);

    if (user != null && isUuid(user.getPatronGroup())) {
      return userGroups.get(user.getPatronGroup());
    }

    return null;
  }

  @Override
  public Future<Void> updateAccountContextWithInstance(String accountId, Instance instance) {
    accountContexts.put(accountId, getAccountContextById(accountId).withInstance(instance));
    return succeededFuture();
  }

  @Override
  public Future<Void> updateAccountContextWithEffectiveLocation(String accountId,
    Location effectiveLocation) {

    accountContexts.put(accountId,
      getAccountContextById(accountId).withEffectiveLocation(effectiveLocation.getName()));
    return succeededFuture();
  }

  @Override
  public Future<Void> updateAccountContextWithActions(String accountId,
    List<Feefineaction> actions) {

    AccountContextData accountContextData = getAccountContextById(
      accountId);
    if (accountContextData == null) {
      return succeededFuture();
    }

    accountContexts.put(accountId, accountContextData.withActions(actions));
    return succeededFuture();
  }

  @Override
  public boolean isAccountContextCreated(String accountId) {
    return getAccountContextById(accountId) != null;
  }

  @Override
  public List<Feefineaction> getAccountFeeFineActions(String accountId) {
    AccountContextData accountCtx = getAccountContextById(accountId);
    if (accountCtx == null) {
      return new ArrayList<>();
    }

    return accountCtx.getActions();
  }

  @Override
  public Loan getLoanByAccountId(String accountId) {
    AccountContextData accountContextData = getAccountContextById(
      accountId);
    if (accountContextData == null) {
      return null;
    }

    return accountContextData.loan;
  }

  @Override
  public void updateAccountContextWithLoan(String accountId, Loan loan) {
    AccountContextData accountContext = getAccountContextById(accountId);
    if (accountContext == null) {
      return;
    }

    accountContexts.put(accountId, accountContext.withLoan(loan));
  }

  @Override
  public void updateAccountContextWithLoanPolicy(String accountId, LoanPolicy loanPolicy) {
    AccountContextData accountContext = getAccountContextById(accountId);
    if (accountContext == null) {
      return;
    }

    accountContexts.put(accountId, accountContext.withLoanPolicy(loanPolicy));
  }

  @Override
  public void updateAccountContextWithOverdueFinePolicy(String accountId,
    OverdueFinePolicy overdueFinePolicy) {

    AccountContextData accountContext = getAccountContextById(accountId);
    if (accountContext == null) {
      return;
    }

    accountContexts.put(accountId, accountContext.withOverdueFinePolicy(overdueFinePolicy));
  }

  @Override
  public void updateAccountContextWithLostItemFeePolicy(String accountId,
    LostItemFeePolicy lostItemFeePolicy) {

    AccountContextData accountContext = getAccountContextById(accountId);
    if (accountContext == null) {
      return;
    }

    accountContexts.put(accountId, accountContext.withLostItemFeePolicy(lostItemFeePolicy));
  }
}
