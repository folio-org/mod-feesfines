package org.folio.rest.service.report;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.domain.Action.CANCEL;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.repository.FeeFineActionRepository.ORDER_BY_OWNER_SOURCE_DATE_ASC;
import static org.folio.rest.service.report.utils.ReportStatsHelper.calculateTotals;
import static org.folio.rest.utils.FeeFineActionHelper.getPatronInfoFromComment;
import static org.folio.rest.utils.FeeFineActionHelper.getStaffInfoFromComment;
import static org.folio.rest.utils.PatronHelper.buildFormattedName;
import static org.folio.rest.utils.PatronHelper.getEmail;
import static org.folio.util.UuidUtil.isUuid;
import static org.joda.time.DateTimeZone.UTC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Contributor;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReport;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReportEntry;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReportStats;
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
import org.folio.rest.service.report.context.HasItemInfo;
import org.folio.rest.service.report.context.HasLoanInfo;
import org.folio.rest.service.report.context.HasServicePointsInfo;
import org.folio.rest.service.report.context.HasUserInfo;
import org.folio.rest.service.report.parameters.FinancialTransactionsDetailReportParameters;
import org.folio.rest.service.report.utils.LookupHelper;
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
  private final FeeFineActionRepository feeFineActionRepository;
  private final LostItemFeePolicyRepository lostItemFeePolicyRepository;
  private final OverdueFinePolicyRepository overdueFinePolicyRepository;

  private final LookupHelper lookupHelper;

  public FinancialTransactionsDetailReportService(Map<String, String> headers, io.vertx.core.Context context) {
    super(headers, context);

    inventoryClient = new InventoryClient(context.owner(), headers);
    circulationStorageClient = new CirculationStorageClient(context.owner(), headers);
    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    lostItemFeePolicyRepository = new LostItemFeePolicyRepository(context, headers);
    overdueFinePolicyRepository = new OverdueFinePolicyRepository(context, headers);

    lookupHelper = new LookupHelper(headers, context);
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
    return ctx.actionsToAccounts.keySet().stream()
      .reduce(succeededFuture(ctx),
        (f, a) -> f.compose(result -> processSingleFeeFineAction(ctx, a)),
        (a, b) -> succeededFuture(ctx));
  }

  private Future<Context> processSingleFeeFineAction(Context ctx, Feefineaction feeFineAction) {
    Account account = ctx.actionsToAccounts.get(feeFineAction);
    String accountId = account.getId();

    return lookupHelper.lookupFeeFineActionsForAccount(ctx, accountId)
      .compose(r -> lookupHelper.lookupServicePointsForAllActionsInAccount(ctx, accountId))
      .compose(r -> lookupHelper.lookupUserForAccount(ctx, account))
      .compose(r -> lookupHelper.lookupUserGroupForUser(ctx, accountId))
      .compose(r -> lookupHelper.lookupItemForAccount(ctx, accountId))
      .compose(r -> lookupHelper.lookupInstanceForAccount(ctx, accountId))
      .compose(r -> lookupHelper.lookupLocationForAccount(ctx, accountId))
      .compose(r -> lookupHelper.lookupLoanForAccount(ctx, accountId))
      .compose(r -> lookupHelper.lookupOverdueFinePolicyForAccount(ctx, accountId))
      .compose(r -> lookupHelper.lookupLostItemFeePolicyForAccount(ctx, accountId));
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
        .withWaiveReason(getPaymentMethod(WAIVE, feeFineAction))
        .withRefundReason(getPaymentMethod(REFUND, feeFineAction))
        .withTransferAccount(getPaymentMethod(TRANSFER, feeFineAction));

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
              .withPatronName(buildFormattedName(user))
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
          if (loan != null) {
            entry = entry
              .withLoanId(loan.getId())
              .withLoanDate(reformatLoanDate(loan.getLoanDate()))
              .withLoanPolicyId(loan.getLoanPolicyId());

            LoanPolicy loanPolicy = accountCtx.loanPolicy;
            if (loanPolicy != null) {
              entry = entry
                .withLoanPolicyName(loanPolicy.getName());
            }

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

    FinancialTransactionsDetailReportStats stats = new FinancialTransactionsDetailReportStats();

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
    calculateTotals(stats.getByPaymentMethod(), actions, action -> getPaymentMethod(PAY, action),
      "Payment method totals");

    // Waive reason totals
    calculateTotals(stats.getByWaiveReason(), actions, action -> getPaymentMethod(WAIVE, action),
      "Waive reason totals");

    // Refund reason totals
    calculateTotals(stats.getByRefundReason(), actions, action -> getPaymentMethod(REFUND, action),
      "Refund reason totals");

    // Transfer account totals
    calculateTotals(stats.getByTransferAccount(), actions,
      action -> getPaymentMethod(TRANSFER, action), "Transfer account totals");

    return stats;
  }

  private String formatMonetaryValue(Double value) {
    return new MonetaryValue(value, currency).toString();
  }

  @With
  @AllArgsConstructor
  @Getter
  private static class Context implements HasUserInfo, HasItemInfo, HasServicePointsInfo,
    HasLoanInfo {

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

      Account account = actionsToAccounts.values().stream()
        .filter(a -> accountId.equals(a.getId()))
        .findFirst()
        .orElse(null);

      if (account != null) {
        AccountContextData accountContext = new AccountContextData().withAccount(account);
        accountContexts.put(accountId, accountContext);
        return accountContext;
      }

      return null;
    }

    @Override
    public Account getAccountById(String accountId) {
      AccountContextData accountContextData = getAccountContextById(accountId);
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

      AccountContextData accountContextData = getAccountContextById(accountId);
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
        return null;
      }

      return accountCtx.getActions();
    }

    @Override
    public Loan getLoanByAccountId(String accountId) {
      AccountContextData accountContextData = getAccountContextById(accountId);
      if (accountContextData == null) {
        return null;
      }

      return accountContextData.loan;
    }

    @Override
    public void updateAccountContextWithLoan(String accountId, Loan loan) {
      if (!isAccountContextCreated(accountId)) {
        return;
      }

      accountContexts.put(accountId, getAccountContextById(accountId).withLoan(loan));
    }

    @Override
    public void updateAccountContextWithLoanPolicy(String accountId, LoanPolicy loanPolicy) {
      if (!isAccountContextCreated(accountId)) {
        return;
      }

      accountContexts.put(accountId, getAccountContextById(accountId).withLoanPolicy(loanPolicy));
    }

    @Override
    public void updateAccountContextWithOverdueFinePolicy(String accountId,
      OverdueFinePolicy overdueFinePolicy) {

      if (!isAccountContextCreated(accountId)) {
        return;
      }

      accountContexts.put(accountId, getAccountContextById(accountId)
        .withOverdueFinePolicy(overdueFinePolicy));
    }

    @Override
    public void updateAccountContextWithLostItemFeePolicy(String accountId,
      LostItemFeePolicy lostItemFeePolicy) {

      if (!isAccountContextCreated(accountId)) {
        return;
      }

      accountContexts.put(accountId, getAccountContextById(accountId)
        .withLostItemFeePolicy(lostItemFeePolicy));
    }
  }

  private String getPaymentMethod(Action action, Feefineaction feeFineAction) {
    return List.of(action.getPartialResult(), action.getFullResult())
      .contains(feeFineAction.getTypeAction()) ? feeFineAction.getPaymentMethod() : "";
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
