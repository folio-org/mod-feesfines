package org.folio.rest.domain.logs;

public enum LogEventPayloadField {
  USER_ID("userId"),
  USER_BARCODE("userBarcode"),
  ITEM_ID("itemId"),
  ITEM_BARCODE("itemBarcode"),
  INSTANCE_ID("instanceId"),
  HOLDINGS_RECORD_ID("holdingsRecordId"),
  TEMPLATE_ID("templateId"),
  TRIGGERING_EVENT("triggeringEvent"),
  ACTION("action"),
  DATE("date"),
  SERVICE_POINT_ID("servicePointId"),
  SOURCE("source"),
  FEE_FINE_ID("feeFineId"),
  FEE_FINE_OWNER("feeFineOwner"),
  LOAN_ID("loanId"),
  AUTOMATED("automated"),
  TYPE("type"),
  AMOUNT("amount"),
  BALANCE("balance"),
  PAYMENT_METHOD("paymentMethod"),
  COMMENT("comment"),
  ITEMS("items");

  private final String value;

  LogEventPayloadField(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }
}
