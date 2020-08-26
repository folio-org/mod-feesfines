package org.folio.rest.utils;

public final class ResourceClients {

  private ResourceClients() {}

  public static ResourceClient accountsClient() {
    return new ResourceClient("/accounts");
  }

  public static ResourceClient accountsCheckPayClient(String accountId) {
    return getUrlForAction(accountId, "check-pay");
  }

  public static ResourceClient accountsCheckWaiveClient(String accountId) {
    return getUrlForAction(accountId, "check-waive");
  }

  public static ResourceClient accountsCheckTransferClient(String accountId) {
    return getUrlForAction(accountId, "check-transfer");
  }

  public static ResourceClient accountsCheckRefundClient(String accountId) {
    return getUrlForAction(accountId, "check-refund");
  }

  public static ResourceClient accountsPayClient(String accountId) {
    return getUrlForAction(accountId, "pay");
  }

  public static ResourceClient accountsWaiveClient(String accountId) {
    return getUrlForAction(accountId, "waive");
  }

  private static ResourceClient getUrlForAction(String accountId, String action) {
    return new ResourceClient(String.format("/accounts/%s/%s", accountId, action));
  }

  public static ResourceClient feeFineActionsClient() {
    return new ResourceClient("/feefineactions");
  }

  public static ResourceClient tenantClient() {
    return new ResourceClient("/_/tenant");
  }
}
