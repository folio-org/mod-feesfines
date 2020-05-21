package org.folio.rest.utils;

public final class ResourceClients {

  private ResourceClients() {}

  public static ResourceClient accountsClient() {
    return new ResourceClient("/accounts");
  }

  public static ResourceClient tenantClient() {
    return new ResourceClient("/_/tenant");
  }
}
