package org.folio.rest.service.report.utils;

import static io.vertx.core.Future.succeededFuture;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.util.UuidUtil.isUuid;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
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
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.repository.LostItemFeePolicyRepository;
import org.folio.rest.repository.OverdueFinePolicyRepository;
import org.folio.rest.service.report.FinancialTransactionsDetailReportContext;
import org.folio.rest.service.report.context.HasAccountInfo;
import org.folio.rest.service.report.context.HasItemInfo;
import org.folio.rest.service.report.context.HasUserInfo;
import org.folio.util.UuidUtil;
import org.joda.time.DateTime;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class LookupHelper {
  private static final Logger log = LogManager.getLogger(LookupHelper.class);

  private final InventoryClient inventoryClient;
  private final UsersClient usersClient;

  private final FeeFineActionRepository feeFineActionRepository;
  private final CirculationStorageClient circulationStorageClient;
  private final LostItemFeePolicyRepository lostItemFeePolicyRepository;
  private final OverdueFinePolicyRepository overdueFinePolicyRepository;

  public LookupHelper(Map<String, String> headers, Context context) {
    inventoryClient = new InventoryClient(context.owner(), headers);
    usersClient = new UsersClient(context.owner(), headers);
    circulationStorageClient = new CirculationStorageClient(context.owner(), headers);

    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    lostItemFeePolicyRepository = new LostItemFeePolicyRepository(context, headers);
    overdueFinePolicyRepository = new OverdueFinePolicyRepository(context, headers);
  }

  public <T extends HasUserInfo> Future<T> lookupUserForAccount(T ctx, Account account) {
    if (account == null) {
      return succeededFuture(ctx);
    }

    String userId = account.getUserId();
    if (!isUuid(userId)) {
      log.error("User ID {} is not a valid UUID - account {}", userId, account.getId());
      return succeededFuture(ctx);
    }

    if (ctx.getUsers().containsKey(userId)) {
      return succeededFuture(ctx);
    }

    return usersClient.fetchUserById(userId)
      .compose(user -> addUserToContext(ctx, user, account.getId(), userId))
      .otherwise(ctx);
  }

  public Future<FinancialTransactionsDetailReportContext> lookupUsersForAccounts(
    FinancialTransactionsDetailReportContext context) {

    Set<String> userIds = context.getActionsToAccounts().values()
      .stream()
      .filter(Objects::nonNull)
      .map(Account::getUserId)
      .collect(toSet());

    return usersClient.fetchUsers(userIds)
      .map(users -> mapBy(users, User::getId))
      .map(context::withUsers)
      .onFailure(t -> log.error("Failed to fetch users", t))
      .otherwise(context);
  }

  private <T extends HasUserInfo> Future<T> addUserToContext(T ctx, User user, String accountId,
    String userId) {

    if (user == null) {
      log.error("User not found - account {}, user {}", accountId, userId);
    } else {
      ctx.getUsers().put(user.getId(), user);
    }

    return succeededFuture(ctx);
  }

  public <T extends HasUserInfo> Future<T> lookupUserGroupForUser(T ctx, String accountId) {
    User user = ctx.getUserByAccountId(accountId);

    if (user == null || ctx.getUserGroupByAccountId(accountId) != null) {
      return succeededFuture(ctx);
    }

    return usersClient.fetchUserGroupById(user.getPatronGroup())
      .map(userGroup -> ctx.getUserGroups().put(userGroup.getId(), userGroup))
      .map(ctx)
      .otherwise(ctx);
  }

  public Future<FinancialTransactionsDetailReportContext> lookupGroupsForUsers(
    FinancialTransactionsDetailReportContext context) {

    Set<String> patronGroupIds = context.getUsers().values()
      .stream()
      .filter(Objects::nonNull)
      .map(User::getPatronGroup)
      .collect(toSet());

    return usersClient.fetchUserGroupsByIds(patronGroupIds)
      .map(groups -> mapBy(groups, UserGroup::getId))
      .map(context::withUserGroups)
      .onFailure(t -> log.error("Failed to fetch user groups", t))
      .otherwise(context);
  }

  public <T extends HasItemInfo> Future<T> lookupItemForAccount(T ctx, String accountId) {
    Account account = ctx.getAccountById(accountId);
    if (account == null) {
      return succeededFuture(ctx);
    }

    String itemId = account.getItemId();
    if (!isUuid(itemId)) {
      log.info("Item ID is not a valid UUID - account {}", accountId);
      return succeededFuture(ctx);
    }

    if (ctx.getItems().containsKey(itemId)) {
      return succeededFuture(ctx);
    }

    return inventoryClient.getItemById(itemId)
      .map(item -> addItemToContext(ctx, item, accountId, itemId))
      .map(ctx)
      .otherwise(ctx);
  }

  public Future<FinancialTransactionsDetailReportContext> lookupItemsForAccounts(
    FinancialTransactionsDetailReportContext context) {

    Set<String> itemIds = context.getActionsToAccounts().values()
      .stream()
      .filter(Objects::nonNull)
      .map(Account::getItemId)
      .collect(toSet());

    return inventoryClient.getItemsByIds(itemIds)
      .map(items -> mapBy(items, Item::getId))
      .map(context::withItems)
      .onFailure(t -> log.error("Failed to fetch items", t))
      .otherwise(context);
  }

  private <T extends HasItemInfo> T addItemToContext(T ctx, Item item, String accountId,
    String itemId) {

    if (item == null) {
      log.info("Item not found - account {}, item {}", accountId, itemId);
    } else {
      ctx.getItems().put(itemId, item);
    }
    return ctx;
  }

  public <T extends HasItemInfo> Future<T> lookupInstanceForAccount(T ctx, String accountId) {
    Account account = ctx.getAccountById(accountId);
    if (account == null) {
      return succeededFuture(ctx);
    }

    Item item = ctx.getItemByAccountId(accountId);
    if (item == null) {
      return succeededFuture(ctx);
    }

    String holdingsRecordId = item.getHoldingsRecordId();
    if (!isUuid(holdingsRecordId)) {
      log.info("Holdings record ID {} is not a valid UUID - account {}", holdingsRecordId,
        accountId);
      return succeededFuture(ctx);
    }

    return inventoryClient.getHoldingById(holdingsRecordId)
      .map(HoldingsRecord::getInstanceId)
      .compose(inventoryClient::getInstanceById)
      .map(instance -> ctx.updateAccountContextWithInstance(accountId, instance))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find instance for account {}, holdingsRecord is {}", accountId,
          holdingsRecordId);
        return ctx;
      });
  }

  public Future<FinancialTransactionsDetailReportContext> lookupInstancesForItems(
    FinancialTransactionsDetailReportContext context) {

    Set<String> holdingsRecordIds = context.getItems().values()
      .stream()
      .filter(Objects::nonNull)
      .map(Item::getHoldingsRecordId)
      .collect(toSet());

    return inventoryClient.getHoldingsByIds(holdingsRecordIds)
      .onFailure(t -> log.error("Failed to fetch holdings", t))
      .compose(holdings -> fetchInstancesForHoldings(context, holdings))
      .otherwise(context);
  }

  private Future<FinancialTransactionsDetailReportContext> fetchInstancesForHoldings(
    FinancialTransactionsDetailReportContext context, Collection<HoldingsRecord> holdingsRecords) {

    Map<String, String> holdingsIdToInstanceId = holdingsRecords.stream()
      .collect(toMap(HoldingsRecord::getId, HoldingsRecord::getInstanceId));

    return inventoryClient.getInstancesByIds(holdingsIdToInstanceId.values())
      .map(instances -> mapBy(instances, Instance::getId))
      .onSuccess(instances -> context.getActionsToAccounts()
        .values()
        .stream()
        .filter(Objects::nonNull)
        .forEach(account -> Optional.ofNullable(account.getItemId())
          .map(itemId -> context.getItems().get(itemId))
          .map(Item::getHoldingsRecordId)
          .map(holdingsIdToInstanceId::get)
          .map(instances::get)
          .ifPresent(instance -> context.updateAccountContextWithInstance(account.getId(), instance))))
      .onFailure(t -> log.error("Failed to fetch instances", t))
      .map(context);
  }

  public Future<FinancialTransactionsDetailReportContext> lookupLocationsForItems(
    FinancialTransactionsDetailReportContext context) {

    Set<String> locationIds = context.getItems().values()
      .stream()
      .filter(Objects::nonNull)
      .map(Item::getEffectiveLocationId)
      .collect(toSet());

    return inventoryClient.getLocationsByIds(locationIds)
      .onSuccess(locations -> {
        Map<String, Location> locationsById = mapBy(locations, Location::getId);
        context.getActionsToAccounts()
          .values()
          .stream()
          .filter(Objects::nonNull)
          .forEach(account -> Optional.ofNullable(account.getItemId())
            .map(itemId -> context.getItems().get(itemId))
            .map(Item::getEffectiveLocationId)
            .map(locationsById::get)
            .ifPresent(location -> context.updateAccountContextWithEffectiveLocation(account.getId(), location)));
      })
      .map(context)
      .onFailure(t -> log.error("Failed to fetch locations", t))
      .otherwise(context);
  }

  public Future<FinancialTransactionsDetailReportContext> lookupActionsForAccounts(
    FinancialTransactionsDetailReportContext context) {

    List<Account> accounts = context.getActionsToAccounts()
      .values()
      .stream()
      .filter(Objects::nonNull)
      .collect(toList());

    log.info("Fetching actions for accounts");

    return feeFineActionRepository.findActionsForAccounts(accounts)
      .onSuccess(actions -> log.info("Fetched {} actions", actions.size()))
      .onSuccess(rowSet -> rowSet.stream()
        .sorted(actionDateComparator())
        .collect(groupingBy(Feefineaction::getAccountId))
        .forEach(context::updateAccountContextWithActions))
      .map(context)
      .onFailure(t -> log.error("Failed to fetch actions for accounts", t))
      .otherwise(context);
  }

  public <T extends HasAccountInfo> Future<T> lookupRefundPayTransferFeeFineActionsForAccount(T ctx,
    String accountId) {

    if (!ctx.isAccountContextCreated(accountId)) {
      return succeededFuture(ctx);
    }

    return feeFineActionRepository.findActionsOfTypesForAccount(accountId,
        List.of(REFUND, PAY, TRANSFER))
      .map(this::sortFeeFineActionsByDate)
      .map(actions -> ctx.updateAccountContextWithActions(accountId, actions))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find REFUND, PAY, TRANSFER actions for account {}", accountId);
        return ctx;
      });
  }

  public Future<FinancialTransactionsDetailReportContext> lookupServicePointsForFeeFineActions(
    FinancialTransactionsDetailReportContext context) {

    Set<String> servicePointIds = context.getActionsToAccounts().keySet()
      .stream()
      .map(Feefineaction::getCreatedAt)
      .collect(toSet());

    return inventoryClient.getServicePointsByIds(servicePointIds)
      .map(servicePoints -> mapBy(servicePoints, ServicePoint::getId))
      .map(context::withServicePoints)
      .onFailure(t -> log.error("Failed to fetch service points", t))
      .otherwise(context);
  }

  public Future<FinancialTransactionsDetailReportContext> lookupLoansForAccounts(
    FinancialTransactionsDetailReportContext context) {

    Set<String> loanIds = context.getActionsToAccounts().values()
      .stream()
      .filter(Objects::nonNull)
      .map(Account::getLoanId)
      .collect(toSet());

    return circulationStorageClient.getLoansByIds(loanIds)
      .onFailure(t -> log.error("Failed to fetch loans", t))
      .onSuccess(loans -> {
        Map<String, Loan> loansById = mapBy(loans, Loan::getId);
        context.getActionsToAccounts()
          .values()
          .stream()
          .filter(Objects::nonNull)
          .forEach(account -> Optional.ofNullable(account.getLoanId())
            .map(loansById::get)
            .ifPresent(loan -> context.updateAccountContextWithLoan(account.getId(), loan)));
      })
      .compose(loans -> lookupPoliciesForLoans(context, loans))
      .otherwise(context);
  }

  private Future<FinancialTransactionsDetailReportContext> lookupPoliciesForLoans(
    FinancialTransactionsDetailReportContext context, Collection<Loan> loans) {

    return succeededFuture(loans)
      .compose(l -> lookupLoanPoliciesForLoans(context, loans))
      .compose(l -> lookupOverdueFinePoliciesForLoans(context, loans))
      .compose(l -> lookupLostItemFeePoliciesForLoans(context, loans));
  }

  private Future<FinancialTransactionsDetailReportContext> lookupLoanPoliciesForLoans(
    FinancialTransactionsDetailReportContext context, Collection<Loan> loans) {

    Map<String, String> loanIdToPolicyId = loans.stream()
      .collect(toMap(Loan::getId, Loan::getLoanPolicyId));

    return circulationStorageClient.getLoanPoliciesByIds(loanIdToPolicyId.values())
      .onSuccess(policies -> {
        Map<String, LoanPolicy> policiesById = mapBy(policies, LoanPolicy::getId);
        context.getActionsToAccounts()
          .values()
          .stream()
          .filter(Objects::nonNull)
          .forEach(account -> Optional.ofNullable(account.getLoanId())
            .map(loanIdToPolicyId::get)
            .map(policiesById::get)
            .ifPresent(policy -> context.updateAccountContextWithLoanPolicy(account.getId(), policy)));
      })
      .map(context)
      .onFailure(t -> log.error("Failed to fetch loan policies", t))
      .otherwise(context);
  }

  private Future<FinancialTransactionsDetailReportContext> lookupOverdueFinePoliciesForLoans(
    FinancialTransactionsDetailReportContext context, Collection<Loan> loans) {

    Map<String, String> loanIdToPolicyId = loans.stream()
      .collect(toMap(Loan::getId, Loan::getOverdueFinePolicyId));

    return overdueFinePolicyRepository.getOverdueFinePoliciesByIds(loanIdToPolicyId.values())
      .onSuccess(policies -> {
        Map<String, OverdueFinePolicy> policiesById = mapBy(policies, OverdueFinePolicy::getId);
        context.getActionsToAccounts()
          .values()
          .stream()
          .filter(Objects::nonNull)
          .forEach(account -> Optional.ofNullable(account.getLoanId())
            .map(loanIdToPolicyId::get)
            .map(policiesById::get)
            .ifPresent(policy -> context.updateAccountContextWithOverdueFinePolicy(account.getId(),
              policy)));
      })
      .map(context)
      .onFailure(t -> log.error("Failed to fetch overdue fine policies", t))
      .otherwise(context);
  }

  private Future<FinancialTransactionsDetailReportContext> lookupLostItemFeePoliciesForLoans(
    FinancialTransactionsDetailReportContext context, Collection<Loan> loans) {

    Map<String, String> loanIdToPolicyId = loans.stream()
      .collect(toMap(Loan::getId, Loan::getLostItemPolicyId));

    return lostItemFeePolicyRepository.getLostItemFeePoliciesByIds(loanIdToPolicyId.values())
      .onSuccess(lostItemFeePolicies -> {
        Map<String, LostItemFeePolicy> policiesById = mapBy(lostItemFeePolicies, LostItemFeePolicy::getId);
        context.getActionsToAccounts()
          .values()
          .stream()
          .filter(Objects::nonNull)
          .forEach(account -> Optional.ofNullable(account.getLoanId())
            .map(loanIdToPolicyId::get)
            .map(policiesById::get)
            .ifPresent(policy -> context.updateAccountContextWithLostItemFeePolicy(account.getId(), policy)));
      })
      .map(context)
      .onFailure(t -> log.error("Failed to fetch lost item fee policies", t))
      .otherwise(context);
  }

  private List<Feefineaction> sortFeeFineActionsByDate(List<Feefineaction> feeFineActions) {
    return feeFineActions.stream()
      .sorted(actionDateComparator())
      .collect(toList());
  }

  private static Comparator<Feefineaction> actionDateComparator() {
    return (left, right) -> {
      if (left == null || right == null) {
        return 0;
      }

      Date leftDate = left.getDateAction();
      Date rightDate = right.getDateAction();

      if (leftDate == null || rightDate == null || leftDate.equals(rightDate)) {
        return 0;
      } else {
        return new DateTime(leftDate)
          .isAfter(new DateTime(rightDate)) ? 1 : -1;
      }
    };
  }

  private static <K, V> Map<K, V> mapBy(Collection<V> collection, Function<V, K> keyMapper) {
    return collection.stream()
      .collect(toMap(keyMapper, identity()));
  }
}