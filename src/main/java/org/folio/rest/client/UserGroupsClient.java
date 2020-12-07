package org.folio.rest.client;

import java.util.Map;

import org.folio.rest.jaxrs.model.UserGroup;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class UserGroupsClient extends OkapiClient {
  public UserGroupsClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<UserGroup> fetchUserGroupById(String userGroupId) {
    return getById("/groups", userGroupId, UserGroup.class);
  }
}
