package org.folio.rest.client;

import java.util.Collection;
import java.util.Map;

import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class UsersClient extends OkapiClient {
  public UsersClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<User> fetchUserById(String userId) {
    return getById("/users", userId, User.class);
  }

  public Future<Collection<User>> fetchUsers(Collection<String> ids) {
    return getByIds("/users", ids, User.class, "users");
  }

  public Future<UserGroup> fetchUserGroupById(String userGroupId) {
    return getById("/groups", userGroupId, UserGroup.class);
  }

  public Future<Collection<UserGroup>> fetchUserGroupsByIds(Collection<String> userGroupIds) {
    return getByIds("/groups", userGroupIds, UserGroup.class, "usergroups");
  }
}
