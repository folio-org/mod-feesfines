package org.folio.rest.service.report.context;

import java.util.Map;

import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Location;

import io.vertx.core.Future;

public interface HasItemInfo extends HasAccountInfo {
  Map<String, Item> getItems();

  Item getItemByAccountId(String accountId);

  Future<Void> updateAccountContextWithInstance(String accountId, Instance instance);

  Future<Void> updateAccountContextWithEffectiveLocation(String accountId, Location location);
}
