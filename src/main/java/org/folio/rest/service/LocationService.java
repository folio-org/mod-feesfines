package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.util.UuidUtil.isUuid;

import java.util.Map;

import org.folio.rest.client.InventoryClient;
import org.folio.rest.jaxrs.model.Location;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class LocationService {
  private final InventoryClient inventoryClient;

  public LocationService(Vertx vertx, Map<String, String> okapiHeaders) {
    inventoryClient = new InventoryClient(vertx, okapiHeaders);
  }

  public Future<Location> getEffectiveLocation(String effectiveLocationId) {
    return inventoryClient.getLocationById(effectiveLocationId)
      .compose(this::fetchInstitution)
      .compose(this::fetchLibrary)
      .compose(this::fetchCampus);
  }

  private Future<Location> fetchInstitution(Location location) {
    final String institutionId = location.getInstitutionId();

    if (!isUuid(institutionId)) {
      return succeededFuture(location);
    }

    return inventoryClient.getInstitutionById(institutionId)
      .map(location::withInstitution);
  }

  private Future<Location> fetchLibrary(Location location) {
    final String libraryId = location.getLibraryId();

    if (!isUuid(libraryId)) {
      return succeededFuture(location);
    }

    return inventoryClient.getLibraryById(libraryId)
      .map(location::withLibrary);
  }

  private Future<Location> fetchCampus(Location location) {
    final String campusId = location.getCampusId();

    if (!isUuid(campusId)) {
      return succeededFuture(location);
    }

    return inventoryClient.getCampusById(campusId)
      .map(location::withCampus);
  }
}
