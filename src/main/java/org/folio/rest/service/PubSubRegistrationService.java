package org.folio.rest.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class PubSubRegistrationService {
  private final Logger logger = LogManager.getLogger(PubSubRegistrationService.class);
  private final OkapiConnectionParams connectionParams;

  public PubSubRegistrationService(Vertx vertx, Map<String, String> headers) {
    this.connectionParams = new OkapiConnectionParams(headers, vertx);
  }

  public void registerModule(Promise<Object> promise) {
    PubSubClientUtils.registerModule(connectionParams)
      .whenComplete((result, throwable) -> {
        if (isTrue(result) && throwable == null) {
          logger.info("Module was successfully registered as publisher/subscriber in mod-pubsub");
          promise.complete(result);
        } else {
          logger.fatal("Error during module registration in mod-pubsub", throwable);
          promise.fail(throwable);
        }
      });
  }
}
