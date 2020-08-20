package org.folio.rest.utils;

public final class ResourceClients {

  private ResourceClients() {}

  public static ResourceClient accountsClient() {
    return new ResourceClient("/accounts");
  }

  public static ResourceClient accountsCheckPayClient(String accountId) {
    return new ResourceClient("/accounts/" + accountId + "/check-pay");
  }

  public static ResourceClient accountsCheckWaiveClient(String accountId) {
    return new ResourceClient("/accounts/" + accountId + "/check-waive");
  }

  public static ResourceClient accountsCheckTransferClient(String accountId) {
    return new ResourceClient("/accounts/" + accountId + "/check-transfer");
  }

  public static ResourceClient accountsActionClient(String accountId, String action) {
    return new ResourceClient(String.format("/accounts/%s/%s", accountId, action));
  }

  public static ResourceClient actionsClient() {
    return new ResourceClient("/feefineactions");
  }

  public static ResourceClient tenantClient() {
    return new ResourceClient("/_/tenant");
  }
}
