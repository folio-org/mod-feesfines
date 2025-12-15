package org.folio.rest.client;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.HttpStatus.HTTP_BAD_REQUEST;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.folio.rest.exception.InternalServerErrorException;
import org.folio.rest.jaxrs.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeeFinePubSubClient {
  private static final Logger log = LoggerFactory.getLogger(FeeFinePubSubClient.class);
  private final OkapiClient okapiClient;

  public FeeFinePubSubClient(Vertx vertx, Map<String, String> okapiHeaders) {
    this.okapiClient = new OkapiClient(vertx, okapiHeaders);
  }

  /**
   * This method publishes the event to pub/sub and IGNORES 400 no subscribers for event
   * error from pub/sub. All other exception will be wrapped to InternalServerError exception
   * and forwarded to consumer.
   *
   * @param event - Event to publish.
   * @return Succeeded CompletableFuture in case of success or failed in case any error
   * occurred.
   */
  public CompletableFuture<Void> publishEvent(Event event) {

    return okapiClient.okapiPostAbs("/pubsub/publish")
      .sendJson(event)
      .toCompletionStage()
      .toCompletableFuture()
      .thenCompose(response -> {
        if (response.statusCode() == HTTP_NO_CONTENT.toInt()) {
          return completedFuture(null);
        }

        if (isEventHasNoSubscribersResponse(response)) {
          log.warn("No subscribers available for event type [{}]", event.getEventType());
          return completedFuture(null);
        }

        log.error("Error publishing event [{}]", response.bodyAsString());

        final CompletableFuture<Void> failureFuture = new CompletableFuture<>();
        failureFuture.completeExceptionally(new InternalServerErrorException(response.bodyAsString()));
        return failureFuture;
      });
  }

  private boolean isEventHasNoSubscribersResponse(HttpResponse<Buffer> response) {
    return response.statusCode() == HTTP_BAD_REQUEST.toInt()
      && response.bodyAsString() != null
      && response.bodyAsString().toLowerCase()
      .contains("there is no subscribers registered for event type");
  }
}
