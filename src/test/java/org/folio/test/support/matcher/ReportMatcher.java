package org.folio.test.support.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.isEmptyString;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.CashDrawerReconciliationReport;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportEntry;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportSources;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportStats;
import org.folio.rest.jaxrs.model.RefundReportEntry;
import org.folio.rest.jaxrs.model.ReportTotalsEntry;
import org.hamcrest.Matcher;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;

public class ReportMatcher {

  private ReportMatcher() {}

  public static Matcher<Response> refundReportEntryMatcher(RefundReportEntry refundReportEntry) {
    return allOf(List.of(
      hasJsonPath("patronName", is(refundReportEntry.getPatronName())),
      hasJsonPath("patronBarcode", is(refundReportEntry.getPatronBarcode())),
      hasJsonPath("patronId", is(refundReportEntry.getPatronId())),
      hasJsonPath("patronGroup", is(refundReportEntry.getPatronGroup())),
      hasJsonPath("feeFineType", is(refundReportEntry.getFeeFineType())),
      hasJsonPath("billedAmount", is(refundReportEntry.getBilledAmount())),
      hasJsonPath("dateBilled", is(refundReportEntry.getDateBilled())),
      hasJsonPath("paidAmount", is(refundReportEntry.getPaidAmount())),
      hasJsonPath("paymentMethod", is(refundReportEntry.getPaymentMethod())),
      hasJsonPath("transferredAmount", is(refundReportEntry.getTransferredAmount())),
      hasJsonPath("transferAccount", is(refundReportEntry.getTransferAccount())),
      hasJsonPath("feeFineId", is(refundReportEntry.getFeeFineId())),
      hasJsonPath("refundDate", is(refundReportEntry.getRefundDate())),
      hasJsonPath("refundAmount", is(refundReportEntry.getRefundAmount())),
      hasJsonPath("refundAction", is(refundReportEntry.getRefundAction())),
      hasJsonPath("refundReason", is(refundReportEntry.getRefundReason())),
      hasJsonPath("staffInfo", is(refundReportEntry.getStaffInfo())),
      hasJsonPath("patronInfo", is(refundReportEntry.getPatronInfo())),
      hasJsonPath("itemBarcode", is(refundReportEntry.getItemBarcode())),
      hasJsonPath("instance", is(refundReportEntry.getInstance())),
      hasJsonPath("feeFineOwner", is(refundReportEntry.getFeeFineOwner())),
      hasJsonPath("actionCompletionDate", isEmptyString()),
      hasJsonPath("staffMemberName", isEmptyString()),
      hasJsonPath("actionTaken", isEmptyString())
    ));
  }

  public static Matcher<ValidatableResponse> cashDrawerReconciliationReportMatcher(
    CashDrawerReconciliationReport cashDrawerReconciliationReport) {

    return allOf(
      hasJsonPath("reportData",
        cashDrawerReconciliationReport.getReportData().isEmpty() ?
          is(List.of()) :
          contains(
            cashDrawerReconciliationReport.getReportData().stream()
              .map(ReportMatcher::cashDrawerReconciliationReportEntryMatcher)
              .collect(Collectors.toList()))),
      hasJsonPath("reportStats", reportStatsMatcher(cashDrawerReconciliationReport.getReportStats())));
  }

  public static Matcher<Response> cashDrawerReconciliationReportEntryMatcher(
    CashDrawerReconciliationReportEntry entry) {

    return allOf(List.of(
      hasJsonPath("source", is(entry.getSource())),
      hasJsonPath("paymentMethod", is(entry.getPaymentMethod())),
      hasJsonPath("paidAmount", is(entry.getPaidAmount())),
      hasJsonPath("feeFineOwner", is(entry.getFeeFineOwner())),
      hasJsonPath("feeFineType", is(entry.getFeeFineType())),
      hasJsonPath("paymentDate", is(entry.getPaymentDate())),
      hasJsonPath("paymentStatus", is(entry.getPaymentStatus())),
      hasJsonPath("transactionInfo", is(entry.getTransactionInfo())),
      hasJsonPath("additionalStaffInfo", is(entry.getAdditionalStaffInfo())),
      hasJsonPath("additionalPatronInfo", is(entry.getAdditionalPatronInfo())),
      hasJsonPath("patronId", is(entry.getPatronId())),
      hasJsonPath("feeFineId", is(entry.getFeeFineId()))
    ));
  }

  public static Matcher<Response> reportStatsMatcher(CashDrawerReconciliationReportStats stats) {
    return allOf(
      hasJsonPath("bySource",
        contains(reportTotalsEntryMatcherList(stats.getBySource()))),
      hasJsonPath("byPaymentMethod",
        contains(reportTotalsEntryMatcherList(stats.getByPaymentMethod()))),
      hasJsonPath("byFeeFineType",
        contains(reportTotalsEntryMatcherList(stats.getByFeeFineType()))),
      hasJsonPath("byFeeFineOwner",
        contains(reportTotalsEntryMatcherList(stats.getByFeeFineOwner())))
    );
  }

  private static List<Matcher<? super Response>> reportTotalsEntryMatcherList(
    List<ReportTotalsEntry> entries) {

    return entries.stream()
      .map(ReportMatcher::reportTotalsEntryMatcher)
      .collect(Collectors.toList());
  }

  private static Matcher<Response> reportTotalsEntryMatcher(ReportTotalsEntry entry) {
    return allOf(
      hasJsonPath("name", is(entry.getName())),
      hasJsonPath("totalAmount", is(entry.getTotalAmount())),
      hasJsonPath("totalCount", is(entry.getTotalCount()))
    );
  }

  public static Matcher<ValidatableResponse> cashDrawerReconciliationReportSourcesMatcher(
    CashDrawerReconciliationReportSources cashDrawerReconciliationReportSources) {

    return allOf(
      hasJsonPath("sources", contains(cashDrawerReconciliationReportSources.getSources().toArray()))
    );
  }
}
