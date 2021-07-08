package org.folio.rest.service.report;

import static io.vertx.core.Future.succeededFuture;
import static java.math.BigDecimal.ZERO;
import static org.folio.rest.domain.Action.CANCEL;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.repository.FeeFineActionRepository.ORDER_BY_OWNER_SOURCE_DATE_ASC;
import static org.folio.rest.utils.FeeFineActionHelper.getPatronInfoFromComment;
import static org.folio.rest.utils.FeeFineActionHelper.getStaffInfoFromComment;
import static org.folio.rest.utils.PatronHelper.formatName;
import static org.folio.rest.utils.PatronHelper.getEmail;
import static org.folio.util.UuidUtil.isUuid;
import static org.joda.time.DateTimeZone.UTC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.UserGroupsClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Contributor;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReport;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReportEntry;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReportStats;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.jaxrs.model.ReportTotalsEntry;
import org.folio.rest.jaxrs.model.ServicePoint;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.repository.LostItemFeePolicyRepository;
import org.folio.rest.repository.OverdueFinePolicyRepository;
import org.folio.rest.service.LocationService;
import org.folio.rest.service.report.parameters.FinancialTransactionsDetailReportParameters;
import org.joda.time.DateTimeZone;

import io.vertx.core.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

public class FinancialTransactionsDetailReportService extends
  DateBasedReportService<FinancialTransactionsDetailReport, FinancialTransactionsDetailReportParameters> {

  private static final Logger log = LogManager.getLogger(FinancialTransactionsDetailReportService.class);

  private static final int REPORT_ROWS_LIMIT = 1_000_000;
  private static final String EMPTY_VALUE = "-";
  private static final Map<String, String> ACTION_NAMES = new HashMap<>();

  static {
    ACTION_NAMES.put(PAY.getFullResult(), "Payment");
    ACTION_NAMES.put(PAY.getPartialResult(), "Payment");
    ACTION_NAMES.put(WAIVE.getFullResult(), "Waive");
    ACTION_NAMES.put(WAIVE.getPartialResult(), "Waive");
    ACTION_NAMES.put(REFUND.getFullResult(), "Refund");
    ACTION_NAMES.put(REFUND.getPartialResult(), "Refund");
    ACTION_NAMES.put(TRANSFER.getFullResult(), "Transfer");
    ACTION_NAMES.put(TRANSFER.getPartialResult(), "Transfer");
    ACTION_NAMES.put(CANCEL.getFullResult(), "Cancelled");
    ACTION_NAMES.put(CANCEL.getPartialResult(), "Cancelled");
    ACTION_NAMES.put("Staff info only", "Staff info only");
  }

  private final InventoryClient inventoryClient;
  private final CirculationStorageClient circulationStorageClient;
  private final UsersClient usersClient;
  private final UserGroupsClient userGroupsClient;
  private final FeeFineActionRepository feeFineActionRepository;
  private final LostItemFeePolicyRepository lostItemFeePolicyRepository;
  private final OverdueFinePolicyRepository overdueFinePolicyRepository;

  public FinancialTransactionsDetailReportService(Map<String, String> headers, io.vertx.core.Context context) {
    super(headers, context);

    inventoryClient = new InventoryClient(context.owner(), headers);
    circulationStorageClient = new CirculationStorageClient(context.owner(), headers);
    usersClient = new UsersClient(context.owner(), headers);
    userGroupsClient = new UserGroupsClient(context.owner(), headers);
    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    lostItemFeePolicyRepository = new LostItemFeePolicyRepository(context, headers);
    overdueFinePolicyRepository = new OverdueFinePolicyRepository(context, headers);
  }

  @Override
  public Future<FinancialTransactionsDetailReport> build(
    FinancialTransactionsDetailReportParameters params) {

    return adjustDates(params)
      .compose(v -> buildWithAdjustedDates(params));
  }

  private Future<FinancialTransactionsDetailReport> buildWithAdjustedDates(
    FinancialTransactionsDetailReportParameters params) {

    log.info("Building financial transactions detail report with parameters: startDate={}, " +
        "endDate={}, owner={}, createdAt={}, tz={}", params.getStartDate(), params.getEndDate(),
      params.getFeeFineOwner(), params.getCreatedAt(), timeZone);

    List<String> actionTypes = List.of(PAY.getPartialResult(), PAY.getFullResult(),
      WAIVE.getPartialResult(), WAIVE.getFullResult(),
      TRANSFER.getPartialResult(), TRANSFER.getFullResult(),
      REFUND.getPartialResult(), REFUND.getFullResult(),
      CANCEL.getFullResult(),
      "Staff info only");

    Context ctx = new Context();

    return feeFineActionRepository.findFeeFineActionsAndAccounts(actionTypes,
      params.getStartDate(), params.getEndDate(), List.of(params.getFeeFineOwner()),
      params.getCreatedAt(), null, ORDER_BY_OWNER_SOURCE_DATE_ASC, REPORT_ROWS_LIMIT)
      .map(ctx::withActionsToAccounts)
      .compose(this::processAllFeeFineActions)
      .map(this::buildReport);
  }

  private Future<Context> processAllFeeFineActions(Context ctx) {
    return ctx.actionsToAccounts.keySet().stream().reduce(succeededFuture(ctx),
      (f, a) -> f.compose(result -> processSingleFeeFineAction(ctx, a)),
      (a, b) -> succeededFuture(ctx));
  }

  private Future<Context> processSingleFeeFineAction(Context ctx, Feefineaction feeFineAction) {
    Account account = ctx.actionsToAccounts.get(feeFineAction);
    String accountId = account.getId();

    return lookupFeeFineActionsForAccount(ctx, accountId)
      .compose(r -> lookupServicePointsForAllActionsInAccount(ctx, accountId))
      .compose(r -> lookupUserForAccount(ctx, account))
      .compose(r -> lookupUserGroupForUser(ctx, accountId))
      .compose(r -> lookupItemForAccount(ctx, accountId))
      .compose(r -> lookupInstanceForAccount(ctx, accountId))
      .compose(r -> lookupLocationForAccount(ctx, accountId))
      .compose(r -> lookupLoanForAccount(ctx, accountId))
      .compose(r -> lookupOverdueFinePolicyForAccount(ctx, accountId))
      .compose(r -> lookupLostItemFeePolicyForAccount(ctx, accountId));
  }

  private FinancialTransactionsDetailReport buildReport(Context ctx) {

    List<FinancialTransactionsDetailReportEntry> entryList = ctx.actionsToAccounts.keySet().stream()
      .map(action -> buildReportEntry(ctx, action))
      .collect(Collectors.toList());

    return new FinancialTransactionsDetailReport()
      .withReportData(entryList)
      .withReportStats(buildFinancialTransactionsDetailReportStats(ctx));
  }

  private FinancialTransactionsDetailReportEntry buildReportEntry(Context ctx,
    Feefineaction feeFineAction) {

    FinancialTransactionsDetailReportEntry entry = new FinancialTransactionsDetailReportEntry();

    if (feeFineAction != null) {
      ServicePoint createdAtServicePoint = ctx.servicePoints.get(feeFineAction.getCreatedAt());
      String createdAt = createdAtServicePoint == null ? "" : createdAtServicePoint.getName();

      entry = entry
        .withAction(ACTION_NAMES.get(feeFineAction.getTypeAction()))
        .withActionAmount(formatMonetaryValue(feeFineAction.getAmountAction()))
        .withActionDate(formatDate(feeFineAction.getDateAction()))
        .withActionCreatedAt(createdAt)
        .withActionSource(feeFineAction.getSource())
        .withActionStatus(feeFineAction.getTypeAction())
        .withActionAdditionalStaffInfo(getStaffInfoFromComment(feeFineAction))
        .withActionAdditionalPatronInfo(getPatronInfoFromComment(feeFineAction))
        .withPaymentMethod(List.of(PAY.getPartialResult(), PAY.getFullResult())
          .contains(feeFineAction.getTypeAction()) ? feeFineAction.getPaymentMethod() : "")
        .withTransactionInfo(feeFineAction.getTransactionInformation())
        .withWaiveReason(List.of(WAIVE.getPartialResult(), WAIVE.getFullResult())
          .contains(feeFineAction.getTypeAction()) ? feeFineAction.getPaymentMethod() : "")
        .withRefundReason(List.of(REFUND.getPartialResult(), REFUND.getFullResult())
          .contains(feeFineAction.getTypeAction()) ? feeFineAction.getPaymentMethod() : "")
        .withTransferAccount(List.of(TRANSFER.getPartialResult(), TRANSFER.getFullResult())
          .contains(feeFineAction.getTypeAction()) ? feeFineAction.getPaymentMethod() : "");

      Account account = ctx.actionsToAccounts.get(feeFineAction);
      if (account != null) {
        AccountContextData accountCtx = ctx.getAccountContextById(account.getId());
        if (accountCtx != null) {
          entry = entry
            .withFeeFineOwner(account.getFeeFineOwner())
            .withFeeFineType(account.getFeeFineType())
            .withBilledAmount(formatMonetaryValue(account.getAmount()))
            .withFeeFineId(account.getId())
            .withPatronId(account.getUserId())
            .withDueDate(formatDate(account.getDueDate()))
            .withReturnDate(formatDate(account.getReturnedDate()));

          User user = ctx.getUserByAccountId(account.getId());
          if (user != null) {
            entry = entry
              .withPatronName(formatName(user))
              .withPatronBarcode(user.getBarcode())
              .withPatronEmail(getEmail(user));

            UserGroup userGroup = ctx.getUserGroupByAccountId(account.getId());
            if (userGroup != null) {
              entry = entry
                .withPatronGroup(userGroup.getGroup());
            } else {
              log.error("Financial transactions detail report - user group is null, fee/fine action {}",
                feeFineAction.getId());
            }
          } else {
            log.error("Financial transactions detail report - user is null, fee/fine action {}",
              feeFineAction.getId());
          }

          Item item = ctx.getItemByAccountId(account.getId());
          if (item != null) {
            entry = entry
              .withItemId(item.getId())
              .withItemBarcode(item.getBarcode())
              .withCallNumber(item.getEffectiveCallNumberComponents().getCallNumber())
              .withHoldingsRecordId(item.getHoldingsRecordId())
              .withInstanceId(accountCtx.instance.getId())
              .withInstance(accountCtx.instance.getTitle())
              .withContributors(accountCtx.instance.getContributors().stream()
                .map(Contributor::getName)
                .collect(Collectors.joining(", ")))
              .withEffectiveLocation(accountCtx.effectiveLocation);
          }

          Feefineaction chargeAction = accountCtx.actions.stream().findFirst().orElse(null);
          if (chargeAction != null) {
            ServicePoint chargeActionCreatedAtServicePoint = ctx.servicePoints.get(
              chargeAction.getCreatedAt());
            String chargeActionCreatedAt = chargeActionCreatedAtServicePoint == null ? "" :
              chargeActionCreatedAtServicePoint.getName();

            entry = entry
              .withDateBilled(formatDate(chargeAction.getDateAction()))
              .withFeeFineCreatedAt(chargeActionCreatedAt)
              .withFeeFineSource(chargeAction.getSource());
          } else {
            log.error("Financial transactions detail report - charge action is null, fee/fine action {}",
              feeFineAction.getId());
          }

          Loan loan = accountCtx.loan;
          LoanPolicy loanPolicy = accountCtx.loanPolicy;
          if (loan != null && loanPolicy != null) {
            entry = entry
              .withLoanId(loan.getId())
              .withLoanDate(reformatLoanDate(loan.getLoanDate()))
              .withLoanPolicyId(loan.getLoanPolicyId())
              .withLoanPolicyName(loanPolicy.getName());

            OverdueFinePolicy overdueFinePolicy = accountCtx.overdueFinePolicy;
            if (overdueFinePolicy != null) {
              entry = entry
                .withOverdueFinePolicyId(overdueFinePolicy.getId())
                .withOverdueFinePolicyName(overdueFinePolicy.getName());
            }

            LostItemFeePolicy lostItemFeePolicy = accountCtx.lostItemFeePolicy;
            if (lostItemFeePolicy != null) {
              entry = entry
                .withLostItemPolicyId(lostItemFeePolicy.getId())
                .withLostItemPolicyName(lostItemFeePolicy.getName());
            }
          }
        } else {
          log.error("Financial transactions detail report - account is null, fee/fine action {}",
            feeFineAction.getId());
        }
      }
    }

    return entry;
  }

  private FinancialTransactionsDetailReportStats buildFinancialTransactionsDetailReportStats(
    Context ctx) {

    FinancialTransactionsDetailReportStats stats =
      new FinancialTransactionsDetailReportStats();

    List<Feefineaction> actions = new ArrayList<>(ctx.actionsToAccounts.keySet());

    // Fee/fine owner totals
    Function<Feefineaction, String> feeFineOwnerCategoryNameFunction = action -> {
      Account account = ctx.actionsToAccounts.get(action);
      if (account == null) {
        return EMPTY_VALUE;
      }
      return account.getFeeFineOwner();
    };
    calculateTotals(stats.getByFeeFineOwner(), actions, feeFineOwnerCategoryNameFunction,
      "Fee/fine owner totals");

    // Fee/fine type totals
    Function<Feefineaction, String> feeFineTypeCategoryNameFunction = action -> {
      Account account = ctx.actionsToAccounts.get(action);
      if (account == null) {
        return EMPTY_VALUE;
      }
      return account.getFeeFineType();
    };
    calculateTotals(stats.getByFeeFineType(), actions, feeFineTypeCategoryNameFunction,
      "Fee/fine type totals");

    // Action totals
    calculateTotals(stats.getByAction(), actions,
      action -> ACTION_NAMES.get(action.getTypeAction()), "Action totals");

    // Payment method totals
    calculateTotals(stats.getByPaymentMethod(), actions, Feefineaction::getPaymentMethod,
      "Payment method totals");

    // Waive reason totals
    calculateTotals(stats.getByWaiveReason(), actions,
      action -> List.of(WAIVE.getPartialResult(), WAIVE.getFullResult())
        .contains(action.getTypeAction()) ? action.getPaymentMethod() : "",
      "Waive reason totals");

    // Refund reason totals
    calculateTotals(stats.getByRefundReason(), actions,
      action -> List.of(REFUND.getPartialResult(), REFUND.getFullResult())
        .contains(action.getTypeAction()) ? action.getPaymentMethod() : "",
      "Refund reason totals");

    // Transfer account totals
    calculateTotals(stats.getByTransferAccount(), actions,
      action -> List.of(TRANSFER.getPartialResult(), TRANSFER.getFullResult())
        .contains(action.getTypeAction()) ? action.getPaymentMethod() : "",
      "Transfer account totals");

    return stats;
  }

  private void calculateTotals(List<ReportTotalsEntry> totalsEntries, List<Feefineaction> actions,
    Function<Feefineaction, String> categoryNameFunction, String totalsCategoryName) {

    List<String> categories = actions.stream()
      .map(categoryNameFunction)
      .filter(Objects::nonNull)
      .filter(category -> !category.isEmpty())
      .distinct()
      .collect(Collectors.toList());

    // Calculate categories
    categories.forEach(category -> totalsEntries.add(new ReportTotalsEntry()
      .withName(category)
      .withTotalAmount(actions.stream()
        .filter(filterByCategory(category, categoryNameFunction))
        .map(Feefineaction::getAmountAction)
        .filter(Objects::nonNull)
        .map(MonetaryValue::new)
        .reduce(MonetaryValue::add)
        .orElse(new MonetaryValue(ZERO))
        .toString())
      .withTotalCount(String.valueOf(actions.stream()
        .filter(filterByCategory(category, categoryNameFunction))
        .count()))));

    // Calculate total
    totalsEntries.add(new ReportTotalsEntry()
      .withName(totalsCategoryName)
      .withTotalAmount(actions.stream()
        .filter(filterByCategories(categories, categoryNameFunction))
        .map(Feefineaction::getAmountAction)
        .filter(Objects::nonNull)
        .map(MonetaryValue::new)
        .reduce(MonetaryValue::add)
        .orElse(new MonetaryValue(ZERO))
        .toString())
      .withTotalCount(String.valueOf(actions.stream()
        .filter(filterByCategories(categories, categoryNameFunction))
        .count())));
  }

  private Predicate<Feefineaction> filterByCategory(String category,
    Function<Feefineaction, String> categoryNameFunction) {

    return action -> category.equals(categoryNameFunction.apply(action));
  }

  private Predicate<Feefineaction> filterByCategories(List<String> categories,
    Function<Feefineaction, String> categoryNameFunction) {

    return action -> categories.contains(categoryNameFunction.apply(action));
  }

  private String formatMonetaryValue(Double value) {
    return new MonetaryValue(value, currency).toString();
  }

  private Future<Context> lookupFeeFineActionsForAccount(Context ctx,
    String accountId) {

    AccountContextData accountContextData = ctx.getAccountContextById(accountId);
    if (accountContextData == null) {
      return succeededFuture(ctx);
    }

    return feeFineActionRepository.findActionsForAccount(accountId)
      .map(this::sortFeeFineActionsByDate)
      .map(ffa_list -> ctx.accountContexts.put(accountId, accountContextData.withActions(ffa_list)))
      .map(ctx);
  }

  private Future<Context> lookupServicePointsForAllActionsInAccount(Context ctx,
    String accountId) {

    AccountContextData accountCtx = ctx.getAccountContextById(accountId);
    if (accountCtx == null) {
      return succeededFuture(ctx);
    }

    return accountCtx.getActions().stream()
      .map(Feefineaction::getCreatedAt)
      .distinct()
      .reduce(succeededFuture(ctx),
      (f, a) -> f.compose(result -> lookupServicePointForFeeFineAction(ctx, a)),
      (a, b) -> succeededFuture(ctx));
  }

  private Future<Context> lookupServicePointForFeeFineAction(Context ctx,
    String servicePointId) {

    if (servicePointId == null) {
      return succeededFuture(ctx);
    }

    if (!isUuid(servicePointId)) {
      log.info("Service point ID is not a valid UUID - {}", servicePointId);
      return succeededFuture(ctx);
    } else {
      if (ctx.servicePoints.containsKey(servicePointId)) {
        return succeededFuture(ctx);
      } else {
        return inventoryClient.getServicePointById(servicePointId)
          .map(sp -> addServicePointToContext(ctx, sp, sp.getId()))
          .map(ctx)
          .otherwise(ctx);
      }
    }
  }

  private Future<Context> addServicePointToContext(Context ctx, ServicePoint servicePoint,
    String servicePointId) {

    if (servicePoint == null) {
      log.error("Service point not found - service point {}", servicePointId);
    } else {
      ctx.servicePoints.put(servicePoint.getId(), servicePoint);
    }

    return succeededFuture(ctx);
  }

  private Future<Context> lookupUserForAccount(Context ctx, Account account) {
    if (account == null) {
      return succeededFuture(ctx);
    }

    String userId = account.getUserId();
    if (!isUuid(userId)) {
      log.error("User ID {} is not a valid UUID - account {}", userId, account.getId());
      return succeededFuture(ctx);
    }

    if (ctx.users.containsKey(userId)) {
      return succeededFuture(ctx);
    } else {
      return usersClient.fetchUserById(userId)
        .compose(user -> addUserToContext(ctx, user, account.getId(), userId))
        .otherwise(ctx);
    }
  }

  private Future<Context> addUserToContext(Context ctx, User user,
    String accountId, String userId) {

    if (user == null) {
      log.error("User not found - account {}, user {}", accountId, userId);
    } else {
      ctx.users.put(user.getId(), user);
    }

    return succeededFuture(ctx);
  }

  private Future<Context> lookupUserGroupForUser(Context ctx, String accountId) {
    User user = ctx.getUserByAccountId(accountId);

    if (user == null || ctx.getUserGroupByAccountId(accountId) != null) {
      return succeededFuture(ctx);
    }

    return userGroupsClient.fetchUserGroupById(user.getPatronGroup())
      .map(userGroup -> ctx.userGroups.put(userGroup.getId(), userGroup))
      .map(ctx)
      .otherwise(ctx);
  }

  private Future<Context> lookupItemForAccount(Context ctx, String accountId) {
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
      if (ctx.items.containsKey(itemId)) {
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

  private Context addItemToContext(Context ctx, Item item, String accountId, String itemId) {
    if (item == null) {
      log.info("Item not found - account {}, item {}", accountId, itemId);
    } else {
      ctx.items.put(itemId, item);
    }
    return ctx;
  }

  private Future<Context> lookupInstanceForAccount(Context ctx, String accountId) {
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
      .map(instance -> ctx.accountContexts.put(accountId,
        ctx.getAccountContextById(accountId).withInstance(instance)))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find instance for account {}, holdingsRecord is {}", accountId,
          holdingsRecordId);
        return ctx;
      });
  }

  private Future<Context> lookupLocationForAccount(Context ctx, String accountId) {
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
      .map(effectiveLocation -> ctx.accountContexts.put(accountId,
        ctx.getAccountContextById(accountId).withEffectiveLocation(effectiveLocation.getName())))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find location for account {}, effectiveLocationId is {}", accountId,
          effectiveLocationId);
        return ctx;
      });
  }

  private Future<Context> lookupLoanForAccount(Context ctx, String accountId) {
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
      .map(loan -> ctx.accountContexts.put(accountId,
        ctx.getAccountContextById(accountId).withLoan(loan)))
      .compose(actx -> circulationStorageClient.getLoanPolicyById(
        ctx.getAccountContextById(accountId).loan.getLoanPolicyId()))
      .map(loanPolicy -> ctx.accountContexts.put(accountId,
        ctx.getAccountContextById(accountId).withLoanPolicy(loanPolicy)))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find loan for account {}, loan is {}", accountId,
          loanId);
        return ctx;
      });
  }

  private Future<Context> lookupOverdueFinePolicyForAccount(Context ctx, String accountId) {
    AccountContextData accountCtx = ctx.getAccountContextById(accountId);
    if (accountCtx == null || accountCtx.loan == null) {
      return succeededFuture(ctx);
    }

    String overdueFinePolicyId = accountCtx.loan.getOverdueFinePolicyId();
    if (!isUuid(overdueFinePolicyId)) {
      log.info("Overdue fine policy ID {} is not a valid UUID - account {}", overdueFinePolicyId,
        accountId);
      return succeededFuture(ctx);
    }

    return overdueFinePolicyRepository.getOverdueFinePolicyById(overdueFinePolicyId)
      .map(policy -> ctx.accountContexts.put(accountId,
        ctx.getAccountContextById(accountId).withOverdueFinePolicy(policy)))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find overdue fine policy for account {}, overdue fine policy is {}",
          accountId, overdueFinePolicyId);
        return ctx;
      });
  }

  private Future<Context> lookupLostItemFeePolicyForAccount(Context ctx, String accountId) {
    AccountContextData accountCtx = ctx.getAccountContextById(accountId);
    if (accountCtx == null || accountCtx.loan == null) {
      return succeededFuture(ctx);
    }

    String lostItemFeePolicyId = accountCtx.loan.getLostItemPolicyId();
    if (!isUuid(lostItemFeePolicyId)) {
      log.info("Lost item fee policy ID {} is not a valid UUID - account {}", lostItemFeePolicyId,
        accountId);
      return succeededFuture(ctx);
    }

    return lostItemFeePolicyRepository.getLostItemFeePolicyById(lostItemFeePolicyId)
      .map(policy -> ctx.accountContexts.put(accountId,
        ctx.getAccountContextById(accountId).withLostItemFeePolicy(policy)))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find lost item fee policy for account {}, lost item fee policy is {}",
          accountId, lostItemFeePolicyId);
        return ctx;
      });
  }

  @With
  @AllArgsConstructor
  private static class Context {
    final DateTimeZone timeZone;
    final Map<Feefineaction, Account> actionsToAccounts;
    final Map<String, AccountContextData> accountContexts;
    final Map<String, User> users;
    final Map<String, UserGroup> userGroups;
    final Map<String, Item> items;
    final Map<String, ServicePoint> servicePoints;

    public Context() {
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
      else {
        Account account = actionsToAccounts.values().stream()
          .filter(a -> accountId.equals(a.getId()))
          .findFirst()
          .orElse(null);

        if (account != null) {
          AccountContextData accountContext = new AccountContextData().withAccount(account);
          accountContexts.put(accountId, accountContext);
          return accountContext;
        } else {
          return null;
        }
      }
    }

    Account getAccountById(String accountId) {
      AccountContextData accountContextData = getAccountContextById(accountId);
      return accountContextData == null ? null : accountContextData.account;
    }

    Item getItemByAccountId(String accountId) {
      Account account = getAccountById(accountId);

      if (account != null) {
        String itemId = account.getItemId();
        if (itemId != null && items.containsKey(itemId)) {
          return items.get(itemId);
        }
      }

      return null;
    }

    User getUserByAccountId(String accountId) {
      Account account = getAccountById(accountId);

      if (account != null && isUuid(account.getUserId())) {
        return users.get(account.getUserId());
      }

      return null;
    }

    UserGroup getUserGroupByAccountId(String accountId) {
      User user = getUserByAccountId(accountId);

      if (user != null && isUuid(user.getPatronGroup())) {
        return userGroups.get(user.getPatronGroup());
      }

      return null;
    }
  }

  @AllArgsConstructor
  @Getter
  @With
  private static class AccountContextData {
    final Account account;
    final List<Feefineaction> actions;
    final Instance instance;
    final String effectiveLocation;
    final Loan loan;
    final LoanPolicy loanPolicy;
    final OverdueFinePolicy overdueFinePolicy;
    final LostItemFeePolicy lostItemFeePolicy;

    public AccountContextData() {
      account = null;
      actions = new ArrayList<>();
      instance = null;
      effectiveLocation = "";
      loan = null;
      loanPolicy = null;
      overdueFinePolicy = null;
      lostItemFeePolicy = null;
    }
  }
}
