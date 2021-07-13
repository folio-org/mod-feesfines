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
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReport;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReportEntry;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReportStats;
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
      hasJsonPath("reportData", cashDrawerReconciliationReport.getReportData().isEmpty()
        ? is(List.of())
        : contains(cashDrawerReconciliationReport.getReportData().stream()
        .map(ReportMatcher::cashDrawerReconciliationReportEntryMatcher)
        .collect(Collectors.toList()))),
      hasJsonPath("reportStats", cashDrawerReconciliationReportStatsMatcher(
        cashDrawerReconciliationReport.getReportStats())));
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

  public static Matcher<Response> cashDrawerReconciliationReportStatsMatcher(
    CashDrawerReconciliationReportStats stats) {

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

  public static Matcher<ValidatableResponse> financialTransactionsDetailReportMatcher(
    FinancialTransactionsDetailReport financialTransactionsDetailReport) {

    return allOf(
      hasJsonPath("reportData", financialTransactionsDetailReport.getReportData().isEmpty()
        ? is(List.of())
        : contains(financialTransactionsDetailReport.getReportData().stream()
        .map(ReportMatcher::financialTransactionsDetailReportEntryMatcher)
        .collect(Collectors.toList()))),
      hasJsonPath("reportStats", financialTransactionsDetailReportStatsMatcher(
        financialTransactionsDetailReport.getReportStats()))
    );
  }

  public static Matcher<Response> financialTransactionsDetailReportEntryMatcher(
    FinancialTransactionsDetailReportEntry entry) {

    return allOf(List.of(
      hasJsonPath("feeFineOwner", is(entry.getFeeFineOwner())),
      hasJsonPath("feeFineType", is(entry.getFeeFineType())),
      hasJsonPath("billedAmount", is(entry.getBilledAmount())),
      hasJsonPath("dateBilled", is(entry.getDateBilled())),
      hasJsonPath("feeFineCreatedAt", is(entry.getFeeFineCreatedAt())),
      hasJsonPath("feeFineSource", is(entry.getFeeFineSource())),
      hasJsonPath("feeFineId", is(entry.getFeeFineId())),
      hasJsonPath("action", is(entry.getAction())),
      hasJsonPath("actionAmount", is(entry.getActionAmount())),
      hasJsonPath("actionDate", is(entry.getActionDate())),
      hasJsonPath("actionCreatedAt", is(entry.getActionCreatedAt())),
      hasJsonPath("actionSource", is(entry.getActionSource())),
      hasJsonPath("actionStatus", is(entry.getActionStatus())),
      hasJsonPath("actionAdditionalStaffInfo", is(entry.getActionAdditionalStaffInfo())),
      hasJsonPath("actionAdditionalPatronInfo", is(entry.getActionAdditionalPatronInfo())),
      hasJsonPath("paymentMethod", is(entry.getPaymentMethod())),
      hasJsonPath("transactionInfo", is(entry.getTransactionInfo())),
      hasJsonPath("waiveReason", is(entry.getWaiveReason())),
      hasJsonPath("refundReason", is(entry.getRefundReason())),
      hasJsonPath("transferAccount", is(entry.getTransferAccount())),
      hasJsonPath("patronId", is(entry.getPatronId())),
      hasJsonPath("patronName", is(entry.getPatronName())),
      hasJsonPath("patronBarcode", is(entry.getPatronBarcode())),
      hasJsonPath("patronGroup", is(entry.getPatronGroup())),
      hasJsonPath("patronEmail", is(entry.getPatronEmail())),
      hasJsonPath("instance", is(entry.getInstance())),
      hasJsonPath("contributors", is(entry.getContributors())),
      hasJsonPath("itemBarcode", is(entry.getItemBarcode())),
      hasJsonPath("callNumber", is(entry.getCallNumber())),
      hasJsonPath("effectiveLocation", is(entry.getEffectiveLocation())),
      hasJsonPath("loanDate", is(entry.getLoanDate())),
      hasJsonPath("dueDate", is(entry.getDueDate())),
      hasJsonPath("returnDate", is(entry.getReturnDate())),
      hasJsonPath("loanPolicyId", is(entry.getLoanPolicyId())),
      hasJsonPath("loanPolicyName", is(entry.getLoanPolicyName())),
      hasJsonPath("overdueFinePolicyId", is(entry.getOverdueFinePolicyId())),
      hasJsonPath("overdueFinePolicyName", is(entry.getOverdueFinePolicyName())),
      hasJsonPath("lostItemPolicyId", is(entry.getLostItemPolicyId())),
      hasJsonPath("lostItemPolicyName", is(entry.getLostItemPolicyName())),
      hasJsonPath("loanId", is(entry.getLoanId())),
      hasJsonPath("holdingsRecordId", is(entry.getHoldingsRecordId())),
      hasJsonPath("instanceId", is(entry.getInstanceId())),
      hasJsonPath("itemId", is(entry.getItemId()))
    ));
  }

  public static Matcher<Response> financialTransactionsDetailReportStatsMatcher(
    FinancialTransactionsDetailReportStats stats) {

    return allOf(
      hasJsonPath("byFeeFineOwner",
        contains(reportTotalsEntryMatcherList(stats.getByFeeFineOwner()))),
      hasJsonPath("byFeeFineType",
        contains(reportTotalsEntryMatcherList(stats.getByFeeFineType()))),
      hasJsonPath("byAction",
        contains(reportTotalsEntryMatcherList(stats.getByAction()))),
      hasJsonPath("byPaymentMethod",
        contains(reportTotalsEntryMatcherList(stats.getByPaymentMethod()))),
      hasJsonPath("byWaiveReason",
        contains(reportTotalsEntryMatcherList(stats.getByWaiveReason()))),
      hasJsonPath("byRefundReason",
        contains(reportTotalsEntryMatcherList(stats.getByRefundReason()))),
      hasJsonPath("byTransferAccount",
        contains(reportTotalsEntryMatcherList(stats.getByTransferAccount())))
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
