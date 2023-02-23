package org.folio.rest.service;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class PubSubRegistrationService {
  private final Logger logger = LogManager.getLogger(PubSubRegistrationService.class);
  private final OkapiConnectionParams connectionParams;

  public PubSubRegistrationService(Vertx vertx, Map<String, String> headers) {
    this.connectionParams = new OkapiConnectionParams(headers, vertx);
  }

  public Future<Void> registerModule() {
    return Future.fromCompletionStage(PubSubClientUtils.registerModule(connectionParams))
      .onSuccess(r -> logger.info(
        "Module was successfully registered as publisher/subscriber in mod-pubsub"))
      .onFailure(t -> logger.fatal("Error during module registration in mod-pubsub", t))
      .mapEmpty();
  }
}
