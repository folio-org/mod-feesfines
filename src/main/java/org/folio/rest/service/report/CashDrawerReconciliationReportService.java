package org.folio.rest.service.report;

import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.repository.FeeFineActionRepository.ORDER_BY_OWNER_SOURCE_DATE_ASC;
import static org.folio.rest.service.report.utils.ReportStatsHelper.calculateTotals;
import static org.folio.rest.utils.FeeFineActionHelper.getPatronInfoFromComment;
import static org.folio.rest.utils.FeeFineActionHelper.getStaffInfoFromComment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReport;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportEntry;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportSources;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportStats;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.service.report.parameters.CashDrawerReconciliationReportParameters;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class CashDrawerReconciliationReportService extends
  DateBasedReportService<CashDrawerReconciliationReport, CashDrawerReconciliationReportParameters> {

  private static final Logger log = LogManager.getLogger(CashDrawerReconciliationReportService.class);

  private static final int REPORT_ROWS_LIMIT = 1_000_000;
  private static final String EMPTY_VALUE = "-";

  private final FeeFineActionRepository feeFineActionRepository;

  public CashDrawerReconciliationReportService(Map<String, String> headers, Context context) {
    super(headers, context);

    feeFineActionRepository = new FeeFineActionRepository(headers, context);
  }

  public Future<CashDrawerReconciliationReportSources> findSources(String createdAt) {
    return feeFineActionRepository.findSources(PAY, createdAt, REPORT_ROWS_LIMIT)
      .map(sources -> new CashDrawerReconciliationReportSources()
        .withSources(sources));
  }

  @Override
  public Future<CashDrawerReconciliationReport> build(
    CashDrawerReconciliationReportParameters params) {

    return adjustDates(params)
      .compose(v -> buildWithAdjustedDates(params));
  }

  private Future<CashDrawerReconciliationReport> buildWithAdjustedDates(
    CashDrawerReconciliationReportParameters params) {

    log.info("Building cash drawer reconciliation report with parameters: startDate={}, " +
        "endDate={}, createdAt={}, sources={}, tz={}", params.getStartDate(), params.getEndDate(),
      params.getCreatedAt(), params.getSources(), timeZone);

    return feeFineActionRepository.findFeeFineActionsAndAccounts(PAY,
      params.getStartDate(), params.getEndDate(), null, params.getCreatedAt(), params.getSources(),
      ORDER_BY_OWNER_SOURCE_DATE_ASC, REPORT_ROWS_LIMIT)
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

    CashDrawerReconciliationReportEntry entry = new CashDrawerReconciliationReportEntry();

    if (action != null) {
      entry = entry
        .withSource(action.getSource())
        .withPaymentMethod(action.getPaymentMethod())
        .withPaidAmount(formatMonetaryValue(action.getAmountAction()))
        .withPaymentDate(formatDate(action.getDateAction()))
        .withPaymentStatus(action.getTypeAction())
        .withTransactionInfo(action.getTransactionInformation())
        .withAdditionalStaffInfo(getStaffInfoFromComment(action))
        .withAdditionalPatronInfo(getPatronInfoFromComment(action))
        .withFeeFineId(action.getAccountId())
        .withPatronId(action.getUserId());
    }

    if (account != null) {
      entry = entry
        .withFeeFineOwner(account.getFeeFineOwner())
        .withFeeFineType(account.getFeeFineType());
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
      "Source totals");

    calculateTotals(stats.getByPaymentMethod(), actions, Feefineaction::getPaymentMethod,
      "Payment method totals");

    calculateTotals(stats.getByFeeFineType(), actions, feeFineTypeCategoryNameFunction,
      "Fee/fine type totals");

    calculateTotals(stats.getByFeeFineOwner(), actions, feeFineOwnerCategoryNameFunction,
      "Fee/fine owner totals");

    return stats;
  }

  private String formatMonetaryValue(Double value) {
    return new MonetaryValue(value, currency).toString();
  }
}
