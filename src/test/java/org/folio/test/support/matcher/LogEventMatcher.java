package org.folio.test.support.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.logs.LogEventPayloadField.ACTION;
import static org.folio.rest.domain.logs.LogEventPayloadField.AMOUNT;
import static org.folio.rest.domain.logs.LogEventPayloadField.BALANCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.COMMENTS;
import static org.folio.rest.domain.logs.LogEventPayloadField.DATE;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_OWNER;
import static org.folio.rest.domain.logs.LogEventPayloadField.LOAN_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.PAYMENT_METHOD;
import static org.folio.rest.domain.logs.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.SOURCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.USER_ID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;

import org.folio.rest.domain.Action;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.jaxrs.model.CancelBulkActionRequest;
import org.folio.rest.jaxrs.model.DefaultActionRequest;
import org.folio.rest.jaxrs.model.DefaultBulkActionRequest;
import org.folio.rest.jaxrs.model.User;
import org.hamcrest.Matcher;

import java.util.Arrays;
import java.util.Date;

public class LogEventMatcher {
  private LogEventMatcher(){}

  public static Matcher<String> feeFineActionLogEventPayload(Account account, DefaultBulkActionRequest request,
    User user, String action, double amount, double balance) {
    return feeFineActionLogEventPayload(user.getId(), user.getBarcode(), action, request.getServicePointId(),
      request.getUserName(), account.getFeeFineId(), account.getFeeFineOwner(), account.getLoanId(),
      amount, balance, request.getPaymentMethod(), request.getComments());
  }

  public static Matcher<String> feeFineActionLogEventPayload(Account account, DefaultActionRequest request,
    User user, String action, double amount, double balance) {
    return feeFineActionLogEventPayload(user.getId(), user.getBarcode(), action, request.getServicePointId(),
      request.getUserName(), account.getFeeFineId(), account.getFeeFineOwner(), account.getLoanId(),
      amount, balance, request.getPaymentMethod(), request.getComments());
  }

  public static Matcher<String> feeFineActionLogEventPayload(String userId, String userBarcode,
    String action, String servicePointId, String source, String feeFineId, String feeFineOwner, String loanId,
    double amount, double balance, String paymentMethod, String comments) {

    return allOf(Arrays.asList(
      hasJsonPath(USER_ID.value(), is(userId)),
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

  public static Matcher<String> cancelledActionLogEventPayload(Account account, User user, CancelActionRequest request) {
    return cancelledActionLogEventPayload(user.getId(), user.getBarcode(), request.getServicePointId(),
      request.getUserName(), account.getFeeFineId(), account.getFeeFineOwner(), account.getLoanId(),
      account.getAmount(), request.getComments());
  }

  public static Matcher<String> cancelledActionLogEventPayload(Account account, User user, CancelBulkActionRequest request) {
    return cancelledActionLogEventPayload(user.getId(), user.getBarcode(), request.getServicePointId(),
      request.getUserName(), account.getFeeFineId(), account.getFeeFineOwner(), account.getLoanId(),
      account.getAmount(), request.getComments());
  }

  public static Matcher<String> cancelledActionLogEventPayload(String userId, String userBarcode,
    String servicePointId, String source, String feeFineId, String feeFineOwner, String loanId,
    double amount, String comments) {

    return allOf(Arrays.asList(
      hasJsonPath(USER_ID.value(), is(userId)),
      hasJsonPath(ACTION.value(), is(Action.CANCEL.getFullResult())),
      hasJsonPath(DATE.value(), notNullValue(Date.class)),
      hasJsonPath(SERVICE_POINT_ID.value(), is(servicePointId)),
      hasJsonPath(SOURCE.value(), is(source)),
      hasJsonPath(FEE_FINE_ID.value(), is(feeFineId)),
      hasJsonPath(FEE_FINE_OWNER.value(), is(feeFineOwner)),
      hasJsonPath(LOAN_ID.value(), is(loanId)),
      hasJsonPath(AMOUNT.value(), is(amount)),
      hasJsonPath(BALANCE.value(), is(0.0)),
      hasJsonPath(COMMENTS.value(), is(comments))));
  }

  public static Matcher<? super Object> notCreditOrRefundActionLogEventPayload() {
    return hasJsonPath(ACTION.value(), not(anyOf(is(CREDIT.getFullResult()), is(CREDIT.getPartialResult()),
        is(REFUND.getPartialResult()), is(REFUND.getPartialResult()))));
  }
}
