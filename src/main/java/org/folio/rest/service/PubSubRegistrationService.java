package org.folio.rest.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    CompletableFuture<Void> registrationFuture = PubSubClientUtils.registerModule(connectionParams)
      .whenComplete(this::logRegistrationResult)
      .thenApply(ignored -> null);

    return Future.fromCompletionStage(registrationFuture);
  }

  private void logRegistrationResult(Boolean result, Throwable throwable) {
    if (isTrue(result) && throwable == null) {
      logger.info("Module was successfully registered as publisher/subscriber in mod-pubsub");
    } else {
      logger.fatal("Error during module registration in mod-pubsub", throwable);
    }
  }
}
