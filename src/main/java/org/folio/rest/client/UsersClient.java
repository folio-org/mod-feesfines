package org.folio.rest.client;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.Map;
import org.folio.rest.jaxrs.model.User;

public class UsersClient extends OkapiClient {
  public UsersClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<User> fetchUserById(String userId) {
    return getById("/users", userId, User.class);
  }
}
