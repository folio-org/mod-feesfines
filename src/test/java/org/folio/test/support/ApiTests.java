package org.folio.test.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.vertx.core.json.JsonObject.mapFrom;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.utils.ResourceClients.buildAccountClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFinesClient;
import static org.folio.rest.utils.ResourceClients.buildManualBlockClient;
import static org.folio.rest.utils.ResourceClients.buildManualBlockTemplateClient;
import static org.junit.Assert.assertThat;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import io.vertx.core.VertxOptions;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.RestVerticle;
import org.folio.rest.impl.TenantAPI;
import org.folio.rest.impl.TenantRefAPI;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.utils.OkapiClient;
import org.folio.rest.utils.ResourceClient;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ApiTests {
  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String TENANT_NAME = "test_tenant";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final String USER_ID = "69d9169d-06da-4622-9c18-2868bd46b60f";
  public static final String OKAPI_TOKEN = generateOkapiToken();
  public static final String MODULE_NAME = "mod-feesfines";

  @ClassRule
  public static final OkapiDeployment okapiDeployment = new OkapiDeployment();

  protected static Vertx vertx;

  protected final ResourceClient accountsClient = buildAccountClient();
  protected final ResourceClient feeFineActionsClient = buildFeeFineActionsClient();
  protected final ResourceClient manualBlocksClient = buildManualBlockClient();
  protected final ResourceClient feeFinesClient = buildFeeFinesClient();
  protected final ResourceClient manualBlockTemplatesClient = buildManualBlockTemplateClient();
  protected final OkapiClient client = new OkapiClient(getOkapiUrl());
  private final static Logger logger = LogManager.getLogger("ApiTests");

  @BeforeClass
  public static void deployVerticle() throws Exception {
    vertx = Vertx.vertx();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    final CompletableFuture<Void> future = new CompletableFuture<>();

    vertx.deployVerticle(RestVerticle.class.getName(), createDeploymentOptions(),
      res -> createTenant(getTenantAttributes(), future));

    get(future);
  }

  @AfterClass
  public static void undeployEnvironment() {
    final CompletableFuture<Void> future = new CompletableFuture<>();

    vertx.close(notUsed -> {
      PostgresClient.stopEmbeddedPostgres();
      future.complete(null);
    });

    get(future);
  }

  @Before
  public void setUpMapping() {
    okapiDeployment.setUpMapping();
  }

  public static void createTenant(TenantAttributes attributes, CompletableFuture<Void> future) {
    TenantRefAPI tenantAPI = new TenantRefAPI();
    Map<String, String> headers = new HashMap<>();

    headers.put("Content-type", "application/json");
    headers.put("Accept", "application/json,text/plain");
    headers.put("x-okapi-tenant", TENANT_NAME);
    headers.put("X-Okapi-Url", getOkapiUrl());

    tenantAPI.postTenant(attributes, headers, responseAsyncResult -> {
      assertThat(responseAsyncResult.succeeded(), CoreMatchers.is(true));
      assertThat(responseAsyncResult.result().getStatus(), CoreMatchers.is(HttpStatus.SC_NO_CONTENT));
      future.complete(null);
    }, vertx.getOrCreateContext());
  }

  protected static TenantAttributes getTenantAttributes() {
    final Parameter loadReferenceParameter = new Parameter()
      .withKey("loadReference").withValue("true");

    return new TenantAttributes()
      .withModuleFrom(MODULE_NAME + "-14.2.4")
      .withModuleTo(MODULE_NAME + "-" + PomReader.INSTANCE.getVersion())
      .withParameters(Collections.singletonList(loadReferenceParameter));
  }

  private static DeploymentOptions createDeploymentOptions() {
    return new DeploymentOptions().setConfig(new JsonObject()
      .put("http.port", okapiDeployment.getVerticlePort()));
  }

  public static String getOkapiUrl() {
    return okapiDeployment.getOkapiUrl();
  }

  protected OkapiDeployment getOkapi() {
    return okapiDeployment;
  }

  protected void removeAllFromTable(String tableName) {
    final CompletableFuture<Void> future = new CompletableFuture<>();

    PostgresClient.getInstance(vertx, TENANT_NAME)
      .delete(tableName, new Criterion(), result -> future.complete(null));

    get(future);
  }

  private static String generateOkapiToken() {
    final String payload = new JsonObject()
      .put("user_id", USER_ID)
      .put("tenant", TENANT_NAME)
      .put("sub", "admin")
      .toString();
    return format("1.%s.3", Base64.getEncoder()
      .encodeToString(payload.getBytes()));
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }

  protected static <T> T get(CompletableFuture<T> future) {
    try {
      return future.get(10, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  protected <T> void createEntity(String path, T entity) {
    ObjectMapper mapper = new ObjectMapper().setDateFormat(new SimpleDateFormat(DATE_TIME_FORMAT));

    try {
      RestAssured.given()
        .spec(getRequestSpecification())
        .body(mapper.writeValueAsString(entity))
        .when()
        .post(path)
        .then()
        .statusCode(HttpStatus.SC_CREATED)
        .contentType(ContentType.JSON);
    }
    catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  protected <T> void deleteEntity(String path, String id) {
    RestAssured.given()
      .spec(getRequestSpecification())
      .when()
      .delete(format("%s/%s", path, id))
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

  protected RequestSpecification getRequestSpecification() {
    return RestAssured.given()
      .baseUri(getOkapiUrl())
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TENANT_NAME))
      .header(new Header(OKAPI_URL_HEADER, getOkapiUrl()))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN));
  }

  public <T> StubMapping createStub(String url, T returnObject) {
    return createStub(url, aResponse().withBody(mapFrom(returnObject).encodePrettily()));
  }

  public <T> StubMapping createStub(String url, T returnObject, String id) {
    return createStub(url + "/" + id, returnObject);
  }

  public <T> StubMapping createStubForPath(String url, T returnObject, String pathRegex) {
    return createStubForPathMatching(url + pathRegex,
      aResponse().withBody(mapFrom(returnObject).encodePrettily()));
  }

  private StubMapping createStub(String url, ResponseDefinitionBuilder responseBuilder) {
    return createStub(urlPathEqualTo(url), responseBuilder);
  }

  private StubMapping createStubForPathMatching(String regex,
    ResponseDefinitionBuilder responseBuilder) {

    return createStub(urlPathMatching(regex), responseBuilder);
  }

  private StubMapping createStub(UrlPathPattern urlPathPattern,
    ResponseDefinitionBuilder responseBuilder) {

    return getOkapi().stubFor(WireMock.get(urlPathPattern)
      .withHeader(ACCEPT, matching(APPLICATION_JSON))
      .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
      .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
      .withHeader(OKAPI_URL_HEADER, matching(getOkapiUrl()))
      .willReturn(responseBuilder));
  }

  public void removeStub(StubMapping stubMapping) {
    getOkapi().removeStub(stubMapping);
  }
}
