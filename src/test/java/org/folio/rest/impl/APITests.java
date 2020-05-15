package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.utils.TestResourceClients.accountsClient;

import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.OkapiClient;
import org.folio.rest.utils.TestResourceClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class APITests {
  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String TENANT_NAME = "test_tenant";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final String USER_ID = "69d9169d-06da-4622-9c18-2868bd46b60f";
  public static final String OKAPI_TOKEN = generateOkapiToken();
  private static final int OKAPI_PORT = NetworkUtils.nextFreePort();

  private static Vertx vertx;

  protected final TestResourceClient accountsClient = accountsClient();

  @ClassRule
  public static WireMockRule wireMock = new WireMockRule(
    new WireMockConfiguration().dynamicPort());

  protected final OkapiClient client = new OkapiClient(getOkapiUrl());

  @BeforeClass
  public static void deployVerticle() throws Exception {
    vertx = Vertx.vertx();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    final CompletableFuture<Void> future = new CompletableFuture<>();

    vertx.deployVerticle(RestVerticle.class.getName(), createDeploymentOptions(),
      res -> future.complete(null));

    future.get(10, TimeUnit.SECONDS);

    mockEndpoints();
    createTenant();
  }

  @AfterClass
  public static void undeployEnvironment() throws Exception {
    final CompletableFuture<Void> future = new CompletableFuture<>();

    vertx.close(notUsed -> {
      PostgresClient.stopEmbeddedPostgres();
      future.complete(null);
    });

    future.get(10, TimeUnit.SECONDS);
  }

  @Before
  public void restoreMocks() {
    mockEndpoints();
  }

  private static void mockEndpoints() {
    wireMock.resetAll();

    wireMock.stubFor(post(urlMatching("/pubsub/publish"))
      .atPriority(100)
      .willReturn(noContent()));
    wireMock.stubFor(post(urlMatching("/pubsub/event-types/declare/(publisher|subscriber)"))
      .atPriority(100)
      .willReturn(created()));
    wireMock.stubFor(post(urlEqualTo("/pubsub/event-types"))
      .atPriority(100)
      .willReturn(created()));

    // forward everything to okapi
    wireMock.stubFor(any(anyUrl())
      .atPriority(Integer.MAX_VALUE)
      .willReturn(aResponse().proxiedFrom("http://localhost:" + OKAPI_PORT)));
  }

  private static void createTenant() {
    RestAssured.given()
      .baseUri("http://localhost:" + OKAPI_PORT)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TENANT_NAME))
      .header(new Header(OKAPI_URL_HEADER, getOkapiUrl()))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN))
      .when()
      .body(getTenantAttributes())
      .post("/_/tenant")
      .then()
      .statusCode(201);
  }

  protected static TenantAttributes getTenantAttributes() {
    final Parameter loadReferenceParameter = new Parameter()
      .withKey("loadReference").withValue("true");

    return new TenantAttributes()
      .withModuleFrom("mod-feesfines-14.2.4")
      .withModuleTo("mod-feesfines-" + PomReader.INSTANCE.getVersion())
      .withParameters(Collections.singletonList(loadReferenceParameter));
  }

  private static DeploymentOptions createDeploymentOptions() {
    return new DeploymentOptions().setConfig(new JsonObject()
      .put("http.port", OKAPI_PORT));
  }

  public static String getOkapiUrl() {
    return "http://localhost:" + wireMock.port();
  }

  protected void removeAllFromTable(String tableName) {

    final CompletableFuture<Void> future = new CompletableFuture<>();

    PostgresClient.getInstance(vertx, TENANT_NAME)
      .delete(tableName, new Criterion(), result -> future.complete(null));

    try {
      future.get(5, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String generateOkapiToken() {
    final String payload = new JsonObject()
      .put("user_id", USER_ID)
      .put("tenant", TENANT_NAME)
      .put("sub", "admin")
      .toString();
    return String.format("1.%s.3", Base64.getEncoder()
      .encodeToString(payload.getBytes()));
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }
}
