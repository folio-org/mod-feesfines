package org.folio.rest.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class WebClientProvider {
  private static final Map<Vertx, WebClient> webClients = new ConcurrentHashMap<>();

  private WebClientProvider() {
  }

  public static WebClient getWebClient(Vertx vertx) {
    return webClients.computeIfAbsent(vertx, WebClient::create);
  }

}
