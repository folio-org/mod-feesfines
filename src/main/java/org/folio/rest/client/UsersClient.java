package org.folio.rest.client;

import java.util.Map;

import org.folio.rest.jaxrs.model.User;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class UsersClient extends OkapiClient {
  public UsersClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(WebClient.create(vertx), okapiHeaders);
  }

  public Future<User> fetchUserById(String userId) {
    return getById("/users", userId, User.class);
  }
}
