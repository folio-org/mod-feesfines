package org.folio.rest.domain.logs;

import static org.folio.rest.domain.logs.LogEventPayloadField.ACTION;
import static org.folio.rest.domain.logs.LogEventPayloadField.AMOUNT;
import static org.folio.rest.domain.logs.LogEventPayloadField.BALANCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.COMMENT;
import static org.folio.rest.domain.logs.LogEventPayloadField.DATE;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_OWNER;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.rest.domain.logs.LogEventPayloadField.LOAN_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.PAYMENT_METHOD;
import static org.folio.rest.domain.logs.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.SOURCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.TYPE;
import static org.folio.rest.domain.logs.LogEventPayloadField.USER_BARCODE;
import static org.folio.rest.domain.logs.LogEventPayloadField.USER_ID;
import static org.folio.rest.utils.JsonHelper.write;

import io.vertx.core.json.JsonObject;
import org.joda.time.DateTime;

public class FeeFineLogContext {
  private String userId;
  private String userBarcode;
  private String itemBarcode;
  private String action;
  private DateTime date;
  private String servicePointId;
  private String source;
  private String feeFineId;
  private String feeFineOwner;
  private String loanId;
  private Boolean automated;
  private String type;
  private double amount;
  private double balance;
  private String paymentMethod;
  private String comment;

  public FeeFineLogContext withUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public FeeFineLogContext withUserBarcode(String userBarcode) {
    this.userBarcode = userBarcode;
    return this;
  }

  public FeeFineLogContext withItemBarcode(String itemBarcode) {
    this.itemBarcode = itemBarcode;
    return this;
  }

  public FeeFineLogContext withAction(String action) {
    this.action = action;
    return this;
  }

  public FeeFineLogContext withDate(DateTime date) {
    this.date = date;
    return this;
  }

  public FeeFineLogContext withServicePointId(String servicePointId) {
    this.servicePointId = servicePointId;
    return this;
  }

  public FeeFineLogContext withSource(String source) {
    this.source = source;
    return this;
  }

  public FeeFineLogContext withFeeFineId(String feeFineId) {
    this.feeFineId = feeFineId;
    return this;
  }

  public FeeFineLogContext withFeeFineOwner(String feeFineOwner) {
    this.feeFineOwner = feeFineOwner;
    return this;
  }

  public FeeFineLogContext withLoanId(String loanId) {
    this.loanId = loanId;
    return this;
  }

  public FeeFineLogContext withAutomated(Boolean automated) {
    this.automated = automated;
    return this;
  }

  public FeeFineLogContext withType(String type) {
    this.type = type;
    return this;
  }

  public FeeFineLogContext withAmount(double amount) {
    this.amount = amount;
    return this;
  }

  public FeeFineLogContext withBalance(double balance) {
    this.balance = balance;
    return this;
  }

  public FeeFineLogContext withPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
    return this;
  }

  public FeeFineLogContext withComment(String comment) {
    this.comment = comment;
    return this;
  }

  public JsonObject asJson() {
    JsonObject json = new JsonObject();
    write(json, USER_ID.value(), userId);
    write(json, USER_BARCODE.value(), userBarcode);
    write(json, ITEM_BARCODE.value(), itemBarcode);
    write(json, ACTION.value(), action);
    write(json, DATE.value(), date);
    write(json, SERVICE_POINT_ID.value(), servicePointId);
    write(json, SOURCE.value(), source);
    write(json, FEE_FINE_ID.value(), feeFineId);
    write(json, LOAN_ID.value(), loanId);
    write(json, FEE_FINE_OWNER.value(), feeFineOwner);
    write(json, TYPE.value(), type);
    write(json, AMOUNT.value(), amount);
    write(json, BALANCE.value(), balance);
    write(json, PAYMENT_METHOD.value(), paymentMethod);
    write(json, COMMENT.value(), comment);
    return json;
  }
}
