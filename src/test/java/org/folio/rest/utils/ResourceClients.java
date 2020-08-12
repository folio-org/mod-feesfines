package org.folio.rest.utils;

public final class ResourceClients {

  private ResourceClients() {}

  public static ResourceClient accountsClient() {
    return new ResourceClient("/accounts");
  }

  public static ResourceClient accountsPayCheckClient(String accountId) {
    return new ResourceClient("/accounts/" + accountId + "/check-pay");
  }

  public static ResourceClient tenantClient() {
    return new ResourceClient("/_/tenant");
  }
}
