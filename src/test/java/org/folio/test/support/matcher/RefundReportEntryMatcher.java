package org.folio.test.support.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.isEmptyString;

import java.util.List;

import org.folio.rest.jaxrs.model.RefundReportEntry;
import org.hamcrest.Matcher;
import org.hamcrest.text.IsEmptyString;

import io.restassured.response.Response;

public class RefundReportEntryMatcher {

  private RefundReportEntryMatcher() {}

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
      hasJsonPath("actionCompletionDate", isEmptyString()),
      hasJsonPath("staffMemberName", isEmptyString()),
      hasJsonPath("actionTaken", isEmptyString())
    ));
  }
}
