package org.folio.test.support.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;
import static org.folio.rest.domain.logs.LogEventPayloadField.ACTION;
import static org.folio.rest.domain.logs.LogEventPayloadField.AMOUNT;
import static org.folio.rest.domain.logs.LogEventPayloadField.BALANCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.COMMENTS;
import static org.folio.rest.domain.logs.LogEventPayloadField.DATE;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_OWNER;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEM_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.LOAN_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.PAYMENT_METHOD;
import static org.folio.rest.domain.logs.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.SOURCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.USER_ID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.Arrays;
import java.util.Date;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.jaxrs.model.CancelBulkActionRequest;
import org.folio.rest.jaxrs.model.DefaultActionRequest;
import org.folio.rest.jaxrs.model.DefaultBulkActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.hamcrest.Matcher;

import io.vertx.core.json.JsonObject;

public class LogEventMatcher {
  private LogEventMatcher() {
  }

  public static Matcher<String> feeFineActionLogEventPayload(Account account,
    DefaultBulkActionRequest request,
    String action, double amount, double balance) {
    return feeFineActionLogEventPayload(account.getUserId(), account.getBarcode(),
      account.getItemId(),
      action, request.getServicePointId(), request.getUserName(), account.getFeeFineId(),
      account.getFeeFineOwner(),
      account.getLoanId(), amount, balance, request.getPaymentMethod(), request.getComments());
  }

  public static Matcher<String> feeFineActionLogEventPayload(Account account,
    DefaultActionRequest request,
    String action, double amount, double balance) {
    return feeFineActionLogEventPayload(account.getUserId(), account.getBarcode(),
      account.getItemId(),
      action, request.getServicePointId(), request.getUserName(), account.getFeeFineId(),
      account.getFeeFineOwner(),
      account.getLoanId(), amount, balance, request.getPaymentMethod(), request.getComments());
  }

  public static Matcher<String> feeFineActionLogEventPayload(String userId, String itemBarcode,
    String itemId,
    String action, String servicePointId, String source, String feeFineId,
    String feeFineOwner, String loanId, double amount, double balance, String paymentMethod,
    String comments) {

    return allOf(Arrays.asList(
      hasJsonPath(USER_ID.value(), is(userId)),
      hasJsonPath(ITEM_BARCODE.value(), is(itemBarcode)),
      hasJsonPath(ITEM_ID.value(), is(itemId)),
      hasJsonPath(ACTION.value(), is(action)),
      hasJsonPath(DATE.value(), notNullValue(Date.class)),
      hasJsonPath(SERVICE_POINT_ID.value(), is(servicePointId)),
      hasJsonPath(SOURCE.value(), is(source)),
      hasJsonPath(FEE_FINE_ID.value(), is(feeFineId)),
      hasJsonPath(FEE_FINE_OWNER.value(), is(feeFineOwner)),
      hasJsonPath(LOAN_ID.value(), is(loanId)),
      hasJsonPath(AMOUNT.value(), is(amount)),
      hasJsonPath(BALANCE.value(), is(balance)),
      hasJsonPath(PAYMENT_METHOD.value(), is(paymentMethod)),
      hasJsonPath(COMMENTS.value(), is(comments))));
  }

  public static Matcher<String> cancelledActionLogEventPayload(Account account,
    CancelActionRequest request) {
    return cancelledActionLogEventPayload(account.getUserId(), account.getItemId(),
      request.getServicePointId(),
      request.getUserName(), account.getFeeFineId(), account.getFeeFineOwner(), account.getLoanId(),
      account.getAmount(), request.getComments());
  }

  public static Matcher<String> cancelledActionLogEventPayload(Account account,
    CancelBulkActionRequest request) {
    return cancelledActionLogEventPayload(account.getUserId(), account.getItemId(),
      request.getServicePointId(),
      request.getUserName(), account.getFeeFineId(), account.getFeeFineOwner(), account.getLoanId(),
      account.getAmount(), request.getComments());
  }

  public static Matcher<String> cancelledActionLogEventPayload(String userId, String itemId,
    String servicePointId, String source,
    String feeFineId, String feeFineOwner, String loanId, MonetaryValue amount, String comments) {

    return allOf(Arrays.asList(
      hasJsonPath(USER_ID.value(), is(userId)),
      hasJsonPath(ITEM_ID.value(), is(itemId)),
      hasJsonPath(ACTION.value(), is(Action.CANCEL.getFullResult())),
      hasJsonPath(DATE.value(), notNullValue(Date.class)),
      hasJsonPath(SERVICE_POINT_ID.value(), is(servicePointId)),
      hasJsonPath(SOURCE.value(), is(source)),
      hasJsonPath(FEE_FINE_ID.value(), is(feeFineId)),
      hasJsonPath(FEE_FINE_OWNER.value(), is(feeFineOwner)),
      hasJsonPath(LOAN_ID.value(), is(loanId)),
      hasJsonPath(AMOUNT.value(), is(amount.getAmount().doubleValue())),
      hasJsonPath(BALANCE.value(), is(0.0)),
      hasJsonPath(COMMENTS.value(), is(comments))));
  }

  public static Matcher<? super Object> partialRefundOfClosedAccountWithPaymentPayloads() {
    return hasJsonPath(ACTION.value(), anyOf(
      is(WAIVE.getFullResult()),
      is(CREDIT.getPartialResult()),
      is(REFUND.getPartialResult()),
      is(PAY.getPartialResult())));
  }

  public static Matcher<? super Object> partialRefundOfClosedAccountWithTransferPayloads() {
    return hasJsonPath(ACTION.value(), anyOf(
      is(TRANSFER.getFullResult()),
      is(CREDIT.getPartialResult()),
      is(REFUND.getPartialResult()),
      is(WAIVE.getPartialResult())));
  }

  public static Matcher<? super Object> partialRefundOfOpenAccountWithPaymentPayloads() {
    return hasJsonPath(ACTION.value(), anyOf(
      is(PAY.getPartialResult()),
      is(WAIVE.getPartialResult()),
      is(CREDIT.getPartialResult()),
      is(REFUND.getPartialResult())));
  }

  public static Matcher<? super Object> partialRefundOfOpenAccountWithTransferPayloads() {
    return hasJsonPath(ACTION.value(), anyOf(
      is(TRANSFER.getPartialResult()),
      is(WAIVE.getPartialResult()),
      is(CREDIT.getPartialResult()),
      is(REFUND.getPartialResult())));
  }

  public static Matcher<? super Object> partialRefundOfClosedAccountWithPaymentAndTransferPayloads() {
    return hasJsonPath(ACTION.value(), anyOf(
      is(PAY.getPartialResult()),
      is(WAIVE.getPartialResult()),
      is(TRANSFER.getFullResult()),
      is(CREDIT.getFullResult()),
      is(CREDIT.getPartialResult()),
      is(REFUND.getFullResult()),
      is(REFUND.getPartialResult())));
  }

  public static Matcher<? super Object> partialRefundOfOpenAccountWithPaymentAndTransferPayloads() {
    return hasJsonPath(ACTION.value(), anyOf(
      is(PAY.getPartialResult()),
      is(WAIVE.getPartialResult()),
      is(TRANSFER.getPartialResult()),
      is(CREDIT.getFullResult()),
      is(CREDIT.getPartialResult()),
      is(REFUND.getFullResult()),
      is(REFUND.getPartialResult())));
  }

  public static Matcher<String> noticeErrorLogRecord(Feefineaction action,
    String errorMessage) {

    return allOf(Arrays.asList(
        hasJsonPath("logEventType", is("NOTICE_ERROR")),
        hasJsonPath("payload", is(noticeErrorEventPayload(action, errorMessage)))
      )
    );
  }

  public static Matcher<String> noticeErrorEventPayload(Feefineaction action,
    String errorMessage) {

    return allOf(Arrays.asList(
      hasJsonPath("userId", is(action.getUserId())),
      hasJsonPath("source", is(action.getSource())),
      hasJsonPath("errorMessage", is(errorMessage)),
      hasJsonPath("date", notNullValue(String.class)),
      hasJsonPath("items", hasItem(allOf(
        hasJsonPath("servicePointId", is(action.getCreatedAt())),
        hasJsonPath("triggeringEvent", is(action.getTypeAction()))
      )))
    ));
  }

}
