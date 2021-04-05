package org.folio.rest.service.report;

import static java.math.BigDecimal.ZERO;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.repository.FeeFineActionRepository.ORDER_BY_OWNER_SOURCE_ASC;
import static org.folio.rest.utils.AccountHelper.getPatronInfoFromComment;
import static org.folio.rest.utils.AccountHelper.getStaffInfoFromComment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReport;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportEntry;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.ReportStats;
import org.folio.rest.jaxrs.model.ReportTotalsEntry;
import org.folio.rest.repository.FeeFineActionRepository;
import org.joda.time.DateTime;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class CashDrawerReconciliationReportService extends DateBasedReportService<CashDrawerReconciliationReport> {
  private static final Logger log = LogManager.getLogger(CashDrawerReconciliationReportService.class);

  private static final int REPORT_ROWS_LIMIT = 1_000_000;

  private final FeeFineActionRepository feeFineActionRepository;

  // Report parameters
  private final DateTime startDate;
  private final DateTime endDate;
  private final String createdAt;
  private final List<String> sources;

  public CashDrawerReconciliationReportService(Map<String, String> headers, Context context,
    DateTime startDate, DateTime endDate, String createdAt, List<String> sources) {

    super(headers, context);

    feeFineActionRepository = new FeeFineActionRepository(headers, context);

    this.startDate = startDate;
    this.endDate = endDate;
    this.createdAt = createdAt;
    this.sources = sources;
  }

  public Future<CashDrawerReconciliationReport> build() {

    return adjustDates(startDate, endDate)
      .compose(v -> buildWithAdjustedDates());
  }

  private Future<CashDrawerReconciliationReport> buildWithAdjustedDates() {

    log.info("Building cash drawer reconciliation report with parameters: startDate={}, endDate={}, " +
      "createdAt={}, sources={}, tz={}", startDateAdjusted, endDateAdjusted, createdAt, sources,
      timeZone);

    return feeFineActionRepository.findFeeFineActionsAndAccountsByParameters(PAY, startDateAdjusted,
      endDateAdjusted, null, createdAt, sources, ORDER_BY_OWNER_SOURCE_ASC, REPORT_ROWS_LIMIT)
      .map(this::buildReport);
  }

  private CashDrawerReconciliationReport buildReport(
    Map<Feefineaction, Account> actionsToAccounts) {

    List<CashDrawerReconciliationReportEntry> entryList = actionsToAccounts.keySet().stream()
      .map(action -> buildReportEntry(action, actionsToAccounts.get(action)))
      .collect(Collectors.toList());

    return new CashDrawerReconciliationReport()
      .withReportData(entryList)
      .withReportStats(buildReportStats(actionsToAccounts));
  }

  private CashDrawerReconciliationReportEntry buildReportEntry(Feefineaction action,
    Account account) {

    return new CashDrawerReconciliationReportEntry()
      .withSource(action.getSource())
      .withPaymentMethod(action.getPaymentMethod())
      .withPaidAmount(formatMonetaryValue(action.getAmountAction()))
      .withFeeFineOwner(account.getFeeFineOwner())
      .withFeeFineType(account.getFeeFineType())
      .withPaymentDate(formatDate(action.getDateAction()))
      .withPaymentStatus(action.getTypeAction())
      .withTransactionInfo(action.getTransactionInformation())
      .withAdditionalStaffInfo(getStaffInfoFromComment(action.getComments()))
      .withAdditionalPatronInfo(getPatronInfoFromComment(action.getComments()))
      .withPatronId(account.getUserId())
      .withFeeFineId(action.getAccountId());
  }

  private ReportStats buildReportStats(Map<Feefineaction, Account> actionsToAccounts) {
    ReportStats reportStats = new ReportStats();

    List<Feefineaction> actions = new ArrayList<>(actionsToAccounts.keySet());

    calculateTotals(reportStats.getBySource(), actions, Feefineaction::getSource,
      Feefineaction::getAmountAction, "Source totals");

    calculateTotals(reportStats.getByPaymentMethod(), actions, Feefineaction::getPaymentMethod,
      Feefineaction::getAmountAction, "Payment method totals");

    calculateTotals(reportStats.getByFeeFineType(), actions,
      action -> actionsToAccounts.get(action).getFeeFineType(),
      Feefineaction::getAmountAction, "Fee/fine type totals");

    calculateTotals(reportStats.getByFeeFineOwner(), actions,
      action -> actionsToAccounts.get(action).getFeeFineOwner(),
      Feefineaction::getAmountAction, "Fee/fine owner totals");

    return reportStats;
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

  private String formatMonetaryValue(Double value) {
    return new MonetaryValue(value, currency).toString();
  }
}
