package org.folio.rest.utils;

public final class TestResourceClients {

  private TestResourceClients() {}

  public static TestResourceClient accountsClient() {
    return new TestResourceClient("/accounts");
  }
}
