package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.folio.rest.jaxrs.model.User;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class UsersClient extends OkapiClient {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public UsersClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }

  public Future<User> fetchUserById(String userId) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    okapiGetAbs("/users/" + userId)
      .send(promise);

    return promise.future().compose(response -> {
      int responseStatus = response.statusCode();
      if (responseStatus != 200) {
        String errorMessage = String.format("Failed to fetch user with ID %s. Response: %d %s",
          userId, responseStatus, response.bodyAsString());
        log.error(errorMessage);
        return failedFuture(errorMessage);
      } else {
        try {
          User user = objectMapper.readValue(response.bodyAsString(), User.class);
          return succeededFuture(user);
        } catch (IOException e) {
          log.error("Failed to process response body: {}", response.bodyAsString());
          return failedFuture(e);
        }
      }
    });
  }
}
