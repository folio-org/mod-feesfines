package org.folio.rest.utils;

import static java.lang.String.format;

public final class ResourceClients {

  private ResourceClients() {}

  public static ResourceClient buildAccountClient() {
    return new ResourceClient("/accounts");
  }

  public static ResourceClient buildManualBlockClient() {
    return new ResourceClient("/manualblocks");
  }

  public static ResourceClient buildAccountCheckPayClient(String accountId) {
    return buildAccountActionClient(accountId, "check-pay");
  }

  public static ResourceClient buildAccountCheckWaiveClient(String accountId) {
    return buildAccountActionClient(accountId, "check-waive");
  }

  public static ResourceClient buildAccountCheckTransferClient(String accountId) {
    return buildAccountActionClient(accountId, "check-transfer");
  }

  public static ResourceClient buildAccountCheckRefundClient(String accountId) {
    return buildAccountActionClient(accountId, "check-refund");
  }

  public static ResourceClient buildAccountPayClient(String accountId) {
    return buildAccountActionClient(accountId, "pay");
  }

  public static ResourceClient buildAccountWaiveClient(String accountId) {
    return buildAccountActionClient(accountId, "waive");
  }

  public static ResourceClient buildAccountTransferClient(String accountId) {
    return buildAccountActionClient(accountId, "transfer");
  }

  public static ResourceClient buildAccountCancelClient(String accountId) {
    return buildAccountActionClient(accountId, "cancel");
  }

  public static ResourceClient accountsRefundClient(String accountId) {
    return buildAccountActionClient(accountId, "refund");
  }

  private static ResourceClient buildAccountActionClient(String accountId, String action) {
    return new ResourceClient(format("/accounts/%s/%s", accountId, action));
  }

  public static ResourceClient buildAccountBulkCheckPayClient() {
    return buildAccountBulkActionClient("check-pay");
  }

  public static ResourceClient buildAccountBulkCheckWaiveClient() {
    return buildAccountBulkActionClient("check-waive");
  }

  public static ResourceClient buildAccountBulkCheckRefundClient() {
    return buildAccountBulkActionClient("check-refund");
  }

  public static ResourceClient buildAccountBulkCheckTransferClient() {
    return buildAccountBulkActionClient("check-transfer");
  }

  public static ResourceClient buildAccountBulkPayClient() {
    return buildAccountBulkActionClient("pay");
  }

  public static ResourceClient buildAccountBulkWaiveClient() {
    return buildAccountBulkActionClient("waive");
  }

  public static ResourceClient buildAccountBulkTransferClient() {
    return buildAccountBulkActionClient("transfer");
  }

  private static ResourceClient buildAccountBulkActionClient(String action) {
    return new ResourceClient(format("/accounts-bulk/%s", action));
  }

  public static ResourceClient feeFineActionsClient() {
    return new ResourceClient("/feefineactions");
  }

  public static ResourceClient tenantClient() {
    return new ResourceClient("/_/tenant");
  }
}
