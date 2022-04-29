package org.folio.rest.client;

import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.HoldingsRecords;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class InventoryClientTest extends BaseClientTest {
  private final InventoryClient inventoryClient;
  {
    inventoryClient = new InventoryClient(vertx, okapiHeaders);
  }

  public static final String HOLDINGS_URL = "/holdings-storage/holdings";
  @Test
  public void shouldSucceedWhenGettingHoldingsRecords(TestContext context) {
    Async async = context.async();

    String holdingsRecordId = UUID.randomUUID().toString();
    HoldingsRecords holdingsRecords = new HoldingsRecords()
      .withHoldingsRecords(List.of(new HoldingsRecord().withId(holdingsRecordId)))
      .withTotalRecords(1);
    createStub(HOLDINGS_URL, HttpStatus.SC_OK, holdingsRecords);

    inventoryClient.getHoldingsById(List.of(holdingsRecordId))
      .onSuccess(records -> {
        context.assertEquals(holdingsRecords.getHoldingsRecords().get(0).getId(),
          records.getHoldingsRecords().get(0).getId());
        context.assertEquals(holdingsRecords.getTotalRecords(), records.getTotalRecords());
        async.complete();
      })
      .onFailure(throwable -> context.fail("Should have succeeded"));
  }

  @Test
  public void shouldFailWhenReceivingErrorResponse(TestContext context) {
    Async async = context.async();

    String holdingsRecordId = UUID.randomUUID().toString();
    createStub(HOLDINGS_URL, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal server error, contact administrator");

    inventoryClient.getHoldingsById(List.of(holdingsRecordId))
      .onSuccess(throwable -> context.fail("Should have failed"))
      .onFailure(failure -> {
        context.assertEquals("Failed to get holdings by IDs. Response status code: 500",
          failure.getMessage());
        async.complete();
      });
  }

  @Test
  public void shouldFailWhenReceivingIncorrectJSON(TestContext context) {
    Async async = context.async();

    String holdingsRecordId = UUID.randomUUID().toString();
    String incorrectResponse = "{";
    createStub(HOLDINGS_URL, HttpStatus.SC_OK, incorrectResponse);

    inventoryClient.getHoldingsById(List.of(holdingsRecordId))
      .onSuccess(throwable -> context.fail("Should have failed"))
      .onFailure(failure -> {
        context.assertEquals("Failed to parse request. Response body: {", failure.getMessage());
        async.complete();
      });
  }

}
