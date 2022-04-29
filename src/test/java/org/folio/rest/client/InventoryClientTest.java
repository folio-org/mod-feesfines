package org.folio.rest.client;

import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.HoldingsRecords;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import io.vertx.core.Vertx;
import static io.vertx.core.json.JsonObject.mapFrom;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.test.support.ApiTests.OKAPI_TOKEN;
import static org.folio.test.support.ApiTests.TENANT_NAME;

@RunWith(VertxUnitRunner.class)
public class InventoryClientTest {
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final int OKAPI_PORT = NetworkUtils.nextFreePort();
  public static final String OKAPI_URL = "http://localhost:" + OKAPI_PORT;
  public static final String HOLDINGS_URL = "/holdings-storage/holdings";
  @Rule
  public WireMockRule mock = new WireMockRule(OKAPI_PORT);

  private final InventoryClient inventoryClient;
  {
    CaseInsensitiveMap<String, String> okapiHeaders = new CaseInsensitiveMap<>();
    okapiHeaders.put(URL, OKAPI_URL);
    okapiHeaders.put(TENANT, TENANT_NAME);
    okapiHeaders.put(TOKEN, OKAPI_TOKEN);
    inventoryClient = new InventoryClient(Vertx.vertx(), okapiHeaders);
  }

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

  private <T> void createStub(String url, int status, T stubObject) {
    createStub(url, aResponse()
      .withStatus(status)
      .withBody(mapFrom(stubObject).encodePrettily()));
  }

  private void createStub(String url, int status, String responseBody) {
    createStub(url, aResponse()
      .withStatus(status)
      .withBody(responseBody));
  }

  private void createStub(String url, ResponseDefinitionBuilder responseBuilder) {
    mock.stubFor(WireMock.get(urlPathEqualTo(url))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(OKAPI_URL))
      .willReturn(responseBuilder));
  }
}
