package org.folio.rest.utils;

public final class ResourceClients {

  private ResourceClients() {}

  public static ResourceClient accountsClient() {
    return new ResourceClient("/accounts");
  }

  public static ResourceClient accountsCheckPayClient(String accountId) {
    return buildUrlForAction(accountId, "check-pay");
  }

  public static ResourceClient accountsCheckWaiveClient(String accountId) {
    return buildUrlForAction(accountId, "check-waive");
  }

  public static ResourceClient accountsCheckTransferClient(String accountId) {
    return buildUrlForAction(accountId, "check-transfer");
  }

  public static ResourceClient accountsCheckRefundClient(String accountId) {
    return buildUrlForAction(accountId, "check-refund");
  }

  public static ResourceClient accountsPayClient(String accountId) {
    return buildUrlForAction(accountId, "pay");
  }

  public static ResourceClient accountsWaiveClient(String accountId) {
    return buildUrlForAction(accountId, "waive");
  }

  public static ResourceClient accountsTransferClient(String accountId) {
    return buildUrlForAction(accountId, "transfer");
  }

  public static ResourceClient accountsRefundClient(String accountId) {
    return buildUrlForAction(accountId, "refund");
  }

  private static ResourceClient buildUrlForAction(String accountId, String action) {
    return new ResourceClient(String.format("/accounts/%s/%s", accountId, action));
  }

  public static ResourceClient feeFineActionsClient() {
    return new ResourceClient("/feefineactions");
  }

  public static ResourceClient tenantClient() {
    return new ResourceClient("/_/tenant");
  }
}
