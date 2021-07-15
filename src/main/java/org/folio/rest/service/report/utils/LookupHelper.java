package org.folio.rest.service.report.utils;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.util.UuidUtil.isUuid;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.client.ConfigurationClient;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.UserGroupsClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.ServicePoint;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.repository.LostItemFeePolicyRepository;
import org.folio.rest.repository.OverdueFinePolicyRepository;
import org.folio.rest.service.LocationService;
import org.folio.rest.service.report.context.HasAccountInfo;
import org.folio.rest.service.report.context.HasItemInfo;
import org.folio.rest.service.report.context.HasLoanInfo;
import org.folio.rest.service.report.context.HasServicePointsInfo;
import org.folio.rest.service.report.context.HasUserInfo;
import org.joda.time.DateTime;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class LookupHelper {
  private static final Logger log = LogManager.getLogger(LookupHelper.class);

  private final LocationService locationService;

  private final ConfigurationClient configurationClient;
  private final InventoryClient inventoryClient;
  private final UsersClient usersClient;
  private final UserGroupsClient userGroupsClient;
  private final AccountRepository accountRepository;

  private final FeeFineActionRepository feeFineActionRepository;
  private final CirculationStorageClient circulationStorageClient;
  private final LostItemFeePolicyRepository lostItemFeePolicyRepository;
  private final OverdueFinePolicyRepository overdueFinePolicyRepository;

  public LookupHelper(Map<String, String> headers, Context context) {
    locationService = new LocationService(context.owner(), headers);

    configurationClient = new ConfigurationClient(context.owner(), headers);
    inventoryClient = new InventoryClient(context.owner(), headers);
    usersClient = new UsersClient(context.owner(), headers);
    userGroupsClient = new UserGroupsClient(context.owner(), headers);
    circulationStorageClient = new CirculationStorageClient(context.owner(), headers);

    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    accountRepository = new AccountRepository(context, headers);
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
    } else {
      return usersClient.fetchUserById(userId)
        .compose(user -> addUserToContext(ctx, user, account.getId(), userId))
        .otherwise(ctx);
    }
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

    return userGroupsClient.fetchUserGroupById(user.getPatronGroup())
      .map(userGroup -> ctx.getUserGroups().put(userGroup.getId(), userGroup))
      .map(ctx)
      .otherwise(ctx);
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
    else {
      if (ctx.getItems().containsKey(itemId)) {
        return succeededFuture(ctx);
      }
      else {
        return inventoryClient.getItemById(itemId)
          .map(item -> addItemToContext(ctx, item, accountId, itemId))
          .map(ctx)
          .otherwise(ctx);
      }
    }
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

  public <T extends HasItemInfo> Future<T> lookupLocationForAccount(T ctx, String accountId) {
    Account account = ctx.getAccountById(accountId);
    if (account == null) {
      return succeededFuture(ctx);
    }

    Item item = ctx.getItemByAccountId(accountId);
    if (item == null) {
      return succeededFuture(ctx);
    }

    String effectiveLocationId = item.getEffectiveLocationId();
    if (!isUuid(effectiveLocationId)) {
      log.info("Effective location ID {} is not a valid UUID - account {}", effectiveLocationId,
        accountId);
      return succeededFuture(ctx);
    }

    return inventoryClient.getLocationById(effectiveLocationId)
      .map(effectiveLocation ->
        ctx.updateAccountContextWithEffectiveLocation(accountId, effectiveLocation))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find location for account {}, effectiveLocationId is {}", accountId,
          effectiveLocationId);
        return ctx;
      });
  }

  public <T extends HasAccountInfo> Future<T> lookupFeeFineActionsForAccount(T ctx,
    String accountId) {

    if (!ctx.isAccountContextCreated(accountId)) {
      return succeededFuture(ctx);
    }

    return feeFineActionRepository.findActionsForAccount(accountId)
      .map(this::sortFeeFineActionsByDate)
      .map(ffa_list -> ctx.updateAccountContextWithActions(accountId, ffa_list))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find actions for account {}", accountId);
        return ctx;
      });
  }

  public <T extends HasAccountInfo> Future<T> lookupRefundPayTransferFeeFineActionsForAccount(T ctx,
    String accountId) {

    if (!ctx.isAccountContextCreated(accountId)) {
      return succeededFuture(ctx);
    }

    return feeFineActionRepository.findActionsOfTypesForAccount(accountId,
      List.of(REFUND, PAY, TRANSFER))
      .map(this::sortFeeFineActionsByDate)
      .map(ffa_list -> ctx.updateAccountContextWithActions(accountId, ffa_list))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find REFUND, PAY, TRANSFER actions for account {}", accountId);
        return ctx;
      });
  }

  public <T extends HasAccountInfo & HasServicePointsInfo> Future<T>
  lookupServicePointsForAllActionsInAccount(T ctx, String accountId) {

    List<Feefineaction> actions = ctx.getAccountFeeFineActions(accountId);
    if (actions == null || actions.isEmpty()) {
      return succeededFuture(ctx);
    }

    return actions.stream()
      .map(Feefineaction::getCreatedAt)
      .distinct()
      .reduce(succeededFuture(ctx),
        (f, a) -> f.compose(result -> lookupServicePointForFeeFineAction(ctx, a)),
        (a, b) -> succeededFuture(ctx));
  }

  private <T extends HasServicePointsInfo> Future<T> lookupServicePointForFeeFineAction(T ctx,
    String servicePointId) {

    if (servicePointId == null) {
      return succeededFuture(ctx);
    }

    if (!isUuid(servicePointId)) {
      log.info("Service point ID is not a valid UUID - {}", servicePointId);
      return succeededFuture(ctx);
    } else {
      if (ctx.getServicePoints().containsKey(servicePointId)) {
        return succeededFuture(ctx);
      } else {
        return inventoryClient.getServicePointById(servicePointId)
          .map(sp -> addServicePointToContext(ctx, sp, sp.getId()))
          .map(ctx)
          .otherwise(ctx);
      }
    }
  }

  private <T extends HasServicePointsInfo> Future<T> addServicePointToContext(T ctx,
    ServicePoint servicePoint, String servicePointId) {

    if (servicePoint == null) {
      log.error("Service point not found - service point {}", servicePointId);
    } else {
      ctx.getServicePoints().put(servicePoint.getId(), servicePoint);
    }

    return succeededFuture(ctx);
  }

  public <T extends HasLoanInfo & HasAccountInfo> Future<T> lookupLoanForAccount(T ctx,
    String accountId) {

    Account account = ctx.getAccountById(accountId);
    if (account == null) {
      return succeededFuture(ctx);
    }

    String loanId = account.getLoanId();
    if (!isUuid(loanId)) {
      log.info("Loan ID {} is not a valid UUID - account {}", loanId, accountId);
      return succeededFuture(ctx);
    }

    return circulationStorageClient.getLoanById(loanId)
      .onSuccess(loan -> ctx.updateAccountCtxWithLoan(accountId, loan))
      .compose(loan -> circulationStorageClient.getLoanPolicyById(loan.getLoanPolicyId()))
      .onSuccess(loanPolicy -> ctx.updateAccountCtxWithLoanPolicy(accountId, loanPolicy))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find loan for account {}, loan is {}", accountId,
          loanId);
        return ctx;
      });
  }

  public <T extends HasLoanInfo & HasAccountInfo> Future<T> lookupOverdueFinePolicyForAccount(
    T ctx, String accountId) {

    Loan loan = ctx.getLoanByAccountId(accountId);
    if (loan == null) {
      return succeededFuture(ctx);
    }

    String overdueFinePolicyId = loan.getOverdueFinePolicyId();
    if (!isUuid(overdueFinePolicyId)) {
      log.info("Overdue fine policy ID {} is not a valid UUID - account {}", overdueFinePolicyId,
        accountId);
      return succeededFuture(ctx);
    }

    return overdueFinePolicyRepository.getOverdueFinePolicyById(overdueFinePolicyId)
      .onSuccess(policy -> ctx.updateAccountCtxWithOverdueFinePolicy(accountId, policy))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find overdue fine policy for account {}, overdue fine policy is {}",
          accountId, overdueFinePolicyId);
        return ctx;
      });
  }

  public <T extends HasLoanInfo & HasAccountInfo> Future<T> lookupLostItemFeePolicyForAccount(
    T ctx, String accountId) {

    Loan loan = ctx.getLoanByAccountId(accountId);
    if (loan == null) {
      return succeededFuture(ctx);
    }

    String lostItemFeePolicyId = loan.getLostItemPolicyId();
    if (!isUuid(lostItemFeePolicyId)) {
      log.info("Lost item fee policy ID {} is not a valid UUID - account {}", lostItemFeePolicyId,
        accountId);
      return succeededFuture(ctx);
    }

    return lostItemFeePolicyRepository.getLostItemFeePolicyById(lostItemFeePolicyId)
      .onSuccess(policy -> ctx.updateAccountCtxWithLostItemFeePolicy(accountId, policy))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find lost item fee policy for account {}, lost item fee policy is {}",
          accountId, lostItemFeePolicyId);
        return ctx;
      });
  }

  private List<Feefineaction> sortFeeFineActionsByDate(List<Feefineaction> feeFineActions) {
    return feeFineActions.stream()
      .sorted(actionDateComparator())
      .collect(Collectors.toList());
  }

  private Comparator<Feefineaction> actionDateComparator() {
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
}