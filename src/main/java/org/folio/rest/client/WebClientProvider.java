package org.folio.rest.client;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class WebClientProvider {
  private static final Map<Vertx, WebClient> webClients = new HashMap<>();

  private WebClientProvider() {
  }

  public static WebClient getWebClient(Vertx vertx) {
    return webClients.computeIfAbsent(vertx, WebClient::create);
  }

}
