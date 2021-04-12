package org.folio.rest.service.report;

import static java.math.BigDecimal.ZERO;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.repository.FeeFineActionRepository.ORDER_BY_OWNER_SOURCE_DATE_ASC;
import static org.folio.rest.utils.AccountHelper.getPatronInfoFromComment;
import static org.folio.rest.utils.AccountHelper.getStaffInfoFromComment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReport;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportEntry;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportStats;
import org.folio.rest.jaxrs.model.ReportTotalsEntry;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.repository.FeeFineActionRepository;
import org.joda.time.DateTime;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;

public class CashDrawerReconciliationReportService extends DateBasedReportService<CashDrawerReconciliationReport> {
  private static final Logger log = LogManager.getLogger(CashDrawerReconciliationReportService.class);

  private static final int REPORT_ROWS_LIMIT = 1_000_000;
  private static final String EMPTY_VALUE = "-";

  private final FeeFineActionRepository feeFineActionRepository;
  private final UsersClient usersClient;

  // Report parameters
  private final DateTime startDate;
  private final DateTime endDate;
  private final String createdAt;
  private final List<String> sourceIds;
  private final List<String> sources;

  public CashDrawerReconciliationReportService(Map<String, String> headers, Context context,
    DateTime startDate, DateTime endDate, String createdAt, List<String> sources) {

    super(headers, context);

    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    usersClient = new UsersClient(context.owner(), headers);

    this.startDate = startDate;
    this.endDate = endDate;
    this.createdAt = createdAt;
    this.sourceIds = sources;
    this.sources = new ArrayList<>();
  }

  public Future<CashDrawerReconciliationReport> build() {
    return adjustDates(startDate, endDate)
      .compose(v -> resolveSources())
      .compose(v -> buildWithAdjustedDates());
  }

  private Future<CashDrawerReconciliationReport> buildWithAdjustedDates() {

    log.info("Building cash drawer reconciliation report with parameters: startDate={}, " +
        "endDate={}, createdAt={}, sources={}, tz={}", startDateAdjusted, endDateAdjusted,
      createdAt, sources, timeZone);

    return feeFineActionRepository.findFeeFineActionsAndAccountsByParameters(PAY, startDateAdjusted,
      endDateAdjusted, null, createdAt, sources, ORDER_BY_OWNER_SOURCE_DATE_ASC, REPORT_ROWS_LIMIT)
      .map(this::buildReport);
  }

  private CashDrawerReconciliationReport buildReport(Map<Feefineaction,
    Account> actionsToAccounts) {

    List<CashDrawerReconciliationReportEntry> entryList = actionsToAccounts.keySet().stream()
      .map(action -> buildReportEntry(action, actionsToAccounts.get(action)))
      .collect(Collectors.toList());

    return new CashDrawerReconciliationReport()
      .withReportData(entryList)
      .withReportStats(buildCashDrawerReconciliationReportStats(actionsToAccounts));
  }

  private CashDrawerReconciliationReportEntry buildReportEntry(Feefineaction action,
    Account account) {

    CashDrawerReconciliationReportEntry entry =new CashDrawerReconciliationReportEntry();

    if (action != null) {
      entry = entry
        .withSource(action.getSource())
        .withPaymentMethod(action.getPaymentMethod())
        .withPaidAmount(formatMonetaryValue(action.getAmountAction()))
        .withPaymentDate(formatDate(action.getDateAction()))
        .withPaymentStatus(action.getTypeAction())
        .withTransactionInfo(action.getTransactionInformation())
        .withAdditionalStaffInfo(getStaffInfoFromComment(action.getComments()))
        .withAdditionalPatronInfo(getPatronInfoFromComment(action.getComments()))
        .withFeeFineId(action.getAccountId());
    }

    if (account != null) {
      entry = entry
        .withFeeFineOwner(account.getFeeFineOwner())
        .withFeeFineType(account.getFeeFineType())
        .withPatronId(account.getUserId());
    }

    return entry;
  }

  private CashDrawerReconciliationReportStats buildCashDrawerReconciliationReportStats(
    Map<Feefineaction, Account> actionsToAccounts) {

    CashDrawerReconciliationReportStats stats =
      new CashDrawerReconciliationReportStats();

    List<Feefineaction> actions = new ArrayList<>(actionsToAccounts.keySet());

    Function<Feefineaction, String> feeFineTypeCategoryNameFunction = action -> {
      Account account = actionsToAccounts.get(action);
      if (account == null) {
        return EMPTY_VALUE;
      }
      return account.getFeeFineType();
    };

    Function<Feefineaction, String> feeFineOwnerCategoryNameFunction = action -> {
      Account account = actionsToAccounts.get(action);
      if (account == null) {
        return EMPTY_VALUE;
      }
      return account.getFeeFineOwner();
    };

    calculateTotals(stats.getBySource(), actions, Feefineaction::getSource,
      Feefineaction::getAmountAction, "Source totals");

    calculateTotals(stats.getByPaymentMethod(), actions, Feefineaction::getPaymentMethod,
      Feefineaction::getAmountAction, "Payment method totals");

    calculateTotals(stats.getByFeeFineType(), actions, feeFineTypeCategoryNameFunction,
      Feefineaction::getAmountAction, "Fee/fine type totals");

    calculateTotals(stats.getByFeeFineOwner(), actions, feeFineOwnerCategoryNameFunction,
      Feefineaction::getAmountAction, "Fee/fine owner totals");

    return stats;
  }

  private void calculateTotals(List<ReportTotalsEntry> totalsEntries, List<Feefineaction> actions,
    Function<Feefineaction, String> categoryNameFunction,
    Function<Feefineaction, Double> amountFunction, String totalsCategoryName) {

    List<String> categories = actions.stream()
      .map(categoryNameFunction)
      .filter(Objects::nonNull)
      .distinct()
      .collect(Collectors.toList());

    // Calculate categories
    categories.forEach(category -> totalsEntries.add(new ReportTotalsEntry()
      .withName(category)
      .withTotalAmount(actions.stream()
        .filter(action -> category.equals(categoryNameFunction.apply(action)))
        .map(amountFunction)
        .filter(Objects::nonNull)
        .map(MonetaryValue::new)
        .reduce(MonetaryValue::add)
        .orElse(new MonetaryValue(ZERO))
        .toString())
      .withTotalCount(String.valueOf(actions.stream()
        .filter(action -> category.equals(categoryNameFunction.apply(action)))
        .count()))));

    // Calculate total
    totalsEntries.add(new ReportTotalsEntry()
      .withName(totalsCategoryName)
      .withTotalAmount(actions.stream()
        .filter(action -> categories.contains(categoryNameFunction.apply(action)))
        .map(amountFunction)
        .filter(Objects::nonNull)
        .map(MonetaryValue::new)
        .reduce(MonetaryValue::add)
        .orElse(new MonetaryValue(ZERO))
        .toString())
      .withTotalCount(String.valueOf(actions.stream()
        .filter(action -> categories.contains(categoryNameFunction.apply(action)))
        .count())));
  }

  public CompositeFuture resolveSources() {
    if (sourceIds == null || sourceIds.isEmpty()) {
      return CompositeFuture.all(Collections.singletonList(Future.succeededFuture(null)));
    }

    return CompositeFuture.all(sourceIds.stream().map(userId -> usersClient.fetchUserById(userId)
      .map(this::getPersonalName)
      .map(sources::add)
      .mapEmpty())
      .collect(Collectors.toList()));
  }

  private String formatMonetaryValue(Double value) {
    return new MonetaryValue(value, currency).toString();
  }

  public String getPersonalName(User user) {
    if (isNotBlank(user.getPersonal().getFirstName()) &&
      isNotBlank(user.getPersonal().getLastName())) {

      return String.format("%s, %s", user.getPersonal().getLastName(),
        user.getPersonal().getFirstName());
    }
    else {
      //Fallback to user name if insufficient personal details
      return user.getUsername();
    }
  }
}
