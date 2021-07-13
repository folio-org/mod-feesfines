package org.folio.rest.service.report.context;

import java.util.Map;

import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;

public interface HasUserInfo {
  User getUserByAccountId(String accountId);
  UserGroup getUserGroupByAccountId(String accountId);
  Map<String, User> getUsers();
  Map<String, UserGroup> getUserGroups();
}
