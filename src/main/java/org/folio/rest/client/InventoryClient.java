package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.jaxrs.model.Campus;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.HoldingsRecords;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Institution;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Items;
import org.folio.rest.jaxrs.model.Library;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.ServicePoint;

public class InventoryClient extends OkapiClient {

  private static final String ITEMS_LIMIT = "1000";
  private static final String HOLDINGS_LIMIT = "1000";

  public InventoryClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<Items> getItemsById(List<String> itemIds) {
    if (itemIds.isEmpty()) {
      return succeededFuture(new Items()
        .withItems(new ArrayList<>())
        .withTotalRecords(0));
    }

    Promise<HttpResponse<Buffer>> promise = Promise.promise();

    HttpRequest<Buffer> request = okapiGetAbs("/item-storage/items");
    if (!itemIds.isEmpty()) {
      request.addQueryParam("query", String.format("(id==(%s))",
          itemIds.stream()
            .map(id -> String.format("\"%s\"", id))
            .collect(Collectors.joining(" or "))))
        .addQueryParam("limit", ITEMS_LIMIT);
    }
    request.send(promise);

    return promise.future().compose(response -> {
      if (response.statusCode() != 200) {
        return failedFuture("Failed to get items by IDs. Response status code: "
          + response.statusCode());
      } else {
        try {
          Items items = objectMapper.readValue(response.bodyAsString(), Items.class);
          return succeededFuture(items);
        } catch (IOException ioException) {
          return failedFuture("Failed to parse response. Response body: "
            + response.bodyAsString());
        }
      }
    });
  }

  public Future<HoldingsRecords> getHoldingsById(List<String> holdingIds) {
    if (holdingIds.isEmpty()) {
      return succeededFuture(new HoldingsRecords()
        .withHoldingsRecords(new ArrayList<>())
        .withTotalRecords(0));
    }

    Promise<HttpResponse<Buffer>> promise = Promise.promise();

    HttpRequest<Buffer> request = okapiGetAbs("/holdings-storage/holdings");
    if (!holdingIds.isEmpty()) {
      request.addQueryParam("query", String.format("(id==(%s))",
          holdingIds.stream()
            .map(id -> String.format("\"%s\"", id))
            .collect(Collectors.joining(" or "))))
        .addQueryParam("limit", HOLDINGS_LIMIT);
    }
    request.putHeader(ACCEPT, APPLICATION_JSON);
    request.send(promise);

    return promise.future().compose(response -> {
      if (response.statusCode() != 200) {
        return failedFuture("Failed to get holdings by IDs. Response status code: "
          + response.statusCode());
      } else {
        try {
          HoldingsRecords holdingsRecords = objectMapper.readValue(response.bodyAsString(),
            HoldingsRecords.class);
          return succeededFuture(holdingsRecords);
        } catch (IOException ioException) {
          return failedFuture("Failed to parse request. Response body: "
            + response.bodyAsString());
        }
      }
    });
  }

  public Future<Item> getItemById(String id) {
    return getById("/item-storage/items", id, Item.class);
  }

  public Future<HoldingsRecord> getHoldingById(String id) {
    return getById("/holdings-storage/holdings", id, HoldingsRecord.class);
  }

  public Future<Instance> getInstanceById(String id) {
    return getById("/instance-storage/instances", id, Instance.class);
  }

  public Future<Location> getLocationById(String id) {
    return getById("/locations", id, Location.class);
  }

  public Future<Institution> getInstitutionById(String id) {
    return getById("/location-units/institutions", id, Institution.class);
  }

  public Future<Campus> getCampusById(String id) {
    return getById("/location-units/campuses", id, Campus.class);
  }

  public Future<Library> getLibraryById(String id) {
    return getById("/location-units/libraries", id, Library.class);
  }

  public Future<ServicePoint> getServicePointById(String id) {
    return getById("/service-points", id, ServicePoint.class);
  }
}
