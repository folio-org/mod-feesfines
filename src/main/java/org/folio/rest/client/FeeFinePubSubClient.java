package org.folio.rest.client;

import static io.vertx.ext.web.client.WebClient.create;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.HttpStatus;
import org.folio.rest.exception.InternalServerError;
import org.folio.rest.jaxrs.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class FeeFinePubSubClient {
  private static final Logger log = LoggerFactory.getLogger(FeeFinePubSubClient.class);
  private final OkapiClient okapiClient;

  public FeeFinePubSubClient(Vertx vertx, Map<String, String> okapiHeaders) {
    this.okapiClient = new OkapiClient(create(vertx), okapiHeaders);
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
    final CompletableFuture<HttpResponse<Buffer>> sendResult = new CompletableFuture<>();

    okapiClient.okapiPostAbs("/pubsub/publish")
      .sendJson(event, response -> {
        if (response.failed()) {
          sendResult.completeExceptionally(response.cause());
        } else {
          sendResult.complete(response.result());
        }
      });

    return sendResult.thenCompose(response -> {
      if (response.statusCode() == HttpStatus.HTTP_NO_CONTENT.toInt()) {
        return CompletableFuture.completedFuture(null);
      }

      if (isEventHasNoSubscribersResponse(response)) {
        log.warn("No subscribers available for event type [{}]", event.getEventType());
        return CompletableFuture.completedFuture(null);
      }

      log.error("Error publishing event [{}]", response.bodyAsString());

      final CompletableFuture<Void> failureFuture = new CompletableFuture<>();
      failureFuture.completeExceptionally(new InternalServerError(response.bodyAsString()));
      return failureFuture;
    });
  }

  private boolean isEventHasNoSubscribersResponse(HttpResponse<Buffer> response) {
    return response.statusCode() == HttpStatus.HTTP_BAD_REQUEST.toInt()
      && response.bodyAsString() != null
      && response.bodyAsString().toLowerCase()
      .contains("there is no subscribers registered for event type");
  }
}
