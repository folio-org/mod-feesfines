package org.folio.rest.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.vertx.core.json.JsonObject.mapFrom;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.okapi.common.XOkapiHeaders.TENANT;
import static org.folio.okapi.common.XOkapiHeaders.TOKEN;
import static org.folio.okapi.common.XOkapiHeaders.URL;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.test.support.ApiTests.OKAPI_TOKEN;
import static org.folio.test.support.ApiTests.TENANT_NAME;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.Before;
import org.junit.Rule;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.vertx.core.Vertx;

public abstract class BaseClientTest {
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final int OKAPI_PORT = NetworkUtils.nextFreePort();
  public static final String OKAPI_URL = "http://localhost:" + OKAPI_PORT;

  protected final Vertx vertx = Vertx.vertx();

  protected final CaseInsensitiveMap<String, String> okapiHeaders;

  @Rule
  public WireMockRule mock = new WireMockRule(OKAPI_PORT);

  {
    okapiHeaders = new CaseInsensitiveMap<>();
    okapiHeaders.put(URL, OKAPI_URL);
    okapiHeaders.put(TENANT, TENANT_NAME);
    okapiHeaders.put(TOKEN, OKAPI_TOKEN);
  }

  @Before
  public void beforeEach() {
    mock.resetAll();
  }

  protected  <T> void createStub(String url, String id, int status, T stubObject) {
    createStub(url + "/" + id, status, stubObject);
  }

  protected <T> void createStub(String url, String id, int status, String responseBody) {
    createStub(url + "/" + id, status, responseBody);
  }

  protected <T> void createStub(String url, int status, T stubObject) {
    createStub(url, aResponse()
      .withStatus(status)
      .withBody(mapFrom(stubObject).encodePrettily()));
  }

  protected void createStub(String url, int status, String responseBody) {
    createStub(url, aResponse()
      .withStatus(status)
      .withBody(responseBody));
  }

  protected void createStub(String url, ResponseDefinitionBuilder responseBuilder) {
    mock.stubFor(WireMock.get(urlPathEqualTo(url))
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(OKAPI_URL))
      .willReturn(responseBuilder));
  }
}
