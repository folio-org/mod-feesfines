package org.folio.rest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.HoldingsRecords;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class InventoryClientTest extends BaseClientTest {
  private final InventoryClient inventoryClient;
  {
    inventoryClient = new InventoryClient(vertx, okapiHeaders);
  }

  public static final String HOLDINGS_URL = "/holdings-storage/holdings";

  @Test
  void shouldSucceedWhenGettingHoldingsRecords(VertxTestContext context) {

    String holdingsRecordId = UUID.randomUUID().toString();
    HoldingsRecords holdingsRecords = new HoldingsRecords()
      .withHoldingsRecords(List.of(new HoldingsRecord().withId(holdingsRecordId)))
      .withTotalRecords(1);
    createStub(HOLDINGS_URL, HttpStatus.SC_OK, holdingsRecords);

    inventoryClient.getHoldingsById(List.of(holdingsRecordId))
      .onComplete(context.succeeding(records -> {
        assertEquals(holdingsRecords.getHoldingsRecords().get(0).getId(),
          records.getHoldingsRecords().get(0).getId());
        assertEquals(holdingsRecords.getTotalRecords(), records.getTotalRecords());
        context.completeNow();
      }));
  }

  @Test
  void shouldFailWhenReceivingErrorResponse(VertxTestContext context) {
    String holdingsRecordId = UUID.randomUUID().toString();
    createStub(HOLDINGS_URL, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal server error, contact administrator");

    inventoryClient.getHoldingsById(List.of(holdingsRecordId))
      .onComplete(context.failing(failure -> {
        assertEquals("Failed to get holdings by IDs. Response status code: 500", failure.getMessage());
        context.completeNow();
      }));
  }

  @Test
  void shouldFailWhenReceivingIncorrectJSON(VertxTestContext context) {
    String holdingsRecordId = UUID.randomUUID().toString();
    String incorrectResponse = "{";
    createStub(HOLDINGS_URL, HttpStatus.SC_OK, incorrectResponse);

    inventoryClient.getHoldingsById(List.of(holdingsRecordId))
      .onComplete(context.failing(failure -> {
        assertEquals("Failed to parse request. Response body: {", failure.getMessage());
        context.completeNow();
      }));
  }

}
