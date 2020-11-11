package org.folio.rest.domain;

import java.util.Collections;
import java.util.List;

import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.jaxrs.model.CancelBulkActionRequest;
import org.folio.rest.jaxrs.model.DefaultActionRequest;
import org.folio.rest.jaxrs.model.DefaultBulkActionRequest;

public class ActionRequest {
  private final List<String> accountIds;
  private final String amount;
  private final String comments;
  private final String transactionInfo;
  private final String servicePointId;
  private final String userName;
  private final String paymentMethod;
  private final boolean notifyPatron;
  private final String reasonForAction;

  public ActionRequest(List<String> accountIds, String amount, String comments,
    String transactionInfo, String servicePointId, String userName, String paymentMethod,
    boolean notifyPatron, String reasonForAction) {

    this.accountIds = accountIds;
    this.amount = amount;
    this.comments = comments;
    this.transactionInfo = transactionInfo;
    this.servicePointId = servicePointId;
    this.userName = userName;
    this.paymentMethod = paymentMethod;
    this.notifyPatron = notifyPatron;
    this.reasonForAction = reasonForAction;
  }

  public List<String> getAccountIds() {
    return accountIds;
  }

  public String getAmount() {
    return amount;
  }

  public String getComments() {
    return comments;
  }

  public String getTransactionInfo() {
    return transactionInfo;
  }

  public String getServicePointId() {
    return servicePointId;
  }

  public String getUserName() {
    return userName;
  }

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public boolean getNotifyPatron() {
    return notifyPatron;
  }

  public String getReasonForAction() {
    return reasonForAction;
  }

  public static ActionRequest from(DefaultActionRequest request, String accountId) {
    return new ActionRequest(
      Collections.singletonList(accountId),
      request.getAmount(),
      request.getComments(),
      request.getTransactionInfo(),
      request.getServicePointId(),
      request.getUserName(),
      request.getPaymentMethod(),
      request.getNotifyPatron(),
      null);
  }

  public static ActionRequest from(DefaultBulkActionRequest request) {
    return new ActionRequest(
      request.getAccountIds(),
      request.getAmount(),
      request.getComments(),
      request.getTransactionInfo(),
      request.getServicePointId(),
      request.getUserName(),
      request.getPaymentMethod(),
      request.getNotifyPatron(),
      null);
  }

  public static ActionRequest from(CancelActionRequest request, String accountId) {
    return new ActionRequest(
      Collections.singletonList(accountId),
      null,
      request.getComments(),
      null,
      request.getServicePointId(),
      request.getUserName(),
      null,
      request.getNotifyPatron(),
      request.getCancellationReason());
  }

  public static ActionRequest from(CancelBulkActionRequest request) {
    return new ActionRequest(
      request.getAccountIds(),
      null,
      request.getComments(),
      null,
      request.getServicePointId(),
      request.getUserName(),
      null,
      request.getNotifyPatron(),
      null);
  }
}
