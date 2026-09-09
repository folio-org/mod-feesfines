package org.folio.test.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.vertx.core.json.JsonObject.mapFrom;
import static java.lang.String.format;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.rest.utils.ResourceClients.buildAccountClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFinesClient;
import static org.folio.rest.utils.ResourceClients.buildManualBlockClient;
import static org.folio.rest.utils.ResourceClients.buildManualBlockTemplateClient;
import static org.folio.util.PomUtils.getModuleVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.domain.AutomaticFeeFineType;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.utils.OkapiClient;
import org.folio.rest.utils.ResourceClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;

@ExtendWith(VertxExtension.class)
public class ApiTests {
  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
  public static final String TENANT_NAME = "test_tenant";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final String USER_ID = "69d9169d-06da-4622-9c18-2868bd46b60f";
  public static final String X_OKAPI_USER_ID = "a3ac258e-08b0-48de-b0e8-fc94eccc7551";
  public static final String OKAPI_TOKEN = generateOkapiToken();
  public static final String MODULE_NAME = "mod-feesfines";
  public static final String FEEFINES_TABLE = "feefines";
  public static final String OWNERS_TABLE = "owners";
  public static final String ACCOUNTS_TABLE = "accounts";
  public static final String FEE_FINE_ACTIONS_TABLE = "feefineactions";
  public static final OkapiDeployment okapiDeployment = new OkapiDeployment();

  protected static Vertx vertx;
  protected static WebClient webClient;

  protected final ResourceClient accountsClient = buildAccountClient();
  protected final ResourceClient feeFineActionsClient = buildFeeFineActionsClient();
  protected final ResourceClient manualBlocksClient = buildManualBlockClient();
  protected final ResourceClient feeFinesClient = buildFeeFinesClient();
  protected final ResourceClient manualBlockTemplatesClient = buildManualBlockTemplateClient();
  protected final OkapiClient client = new OkapiClient(getOkapiUrl());
  protected static PostgresClient pgClient;
  protected static long testStartTime;
  protected static KafkaTestHelper kafkaTestHelper;

  @BeforeAll
  static void deployVerticle(VertxTestContext context) {
    // Start Kafka before deploying the verticle so KafkaEventProducer can connect.
    kafkaTestHelper = KafkaTestHelper.getInstance();

    // Starting Testcontainers from the deployment callback blocks the Vert.x event loop and can
    // exceed VertxExtension's 30-second lifecycle timeout on a busy Docker host.
    ReadyPostgresTester postgresTester = new ReadyPostgresTester();
    postgresTester.start("postgres", "username", "password");
    PostgresClient.setPostgresTester(postgresTester);

    vertx = Vertx.vertx();
    webClient = WebClient.create(vertx);
    okapiDeployment.start();
    okapiDeployment.setUpMapping();

    vertx.deployVerticle(RestVerticle.class.getName(), createDeploymentOptions())
      .compose(ignored -> postTenantAsync(getTenantAttributes()))
      .onComplete(context.succeeding(ignored -> {
        pgClient = PostgresClient.getInstance(vertx, TENANT_NAME);
        context.completeNow();
      }));
  }

  @AfterAll
  static void undeployEnvironment(VertxTestContext context) {
    vertx.close()
      .onFailure(context::failNow)
      .onSuccess(ignored -> {
        PostgresClient.stopPostgresTester();
        okapiDeployment.resetAll();
        context.completeNow();
      });
  }

  @BeforeEach
  public void setUpMapping() {
    testStartTime = System.currentTimeMillis();
    okapiDeployment.setUpMapping();
  }

  protected static void createTenant() {
    HttpResponse<Buffer> response = postTenant(getTenantAttributes());
    assertEquals(HttpStatus.SC_NO_CONTENT, response.statusCode());
  }

  @SneakyThrows
  protected static HttpResponse<Buffer> postTenant(TenantAttributes attributes) {
    return postTenantAsync(attributes)
      .toCompletionStage()
      .toCompletableFuture()
      .get(10, TimeUnit.SECONDS);
  }

  protected static Future<HttpResponse<Buffer>> postTenantAsync(TenantAttributes attributes) {
    return new TenantClient(getOkapiUrl(), TENANT_NAME, generateOkapiToken(), webClient)
      .postTenant(attributes);
  }

  public static void postTenant(String moduleFrom, String moduleTo) {
    TenantAttributes tenantAttributes = getTenantAttributes()
      .withModuleFrom(moduleFrom)
      .withModuleTo(moduleTo);

    postTenant(tenantAttributes);
  }

  protected static TenantAttributes getTenantAttributes() {
    final Parameter loadReferenceParameter = new Parameter()
      .withKey("loadReference").withValue("true");

    return new TenantAttributes()
      .withModuleFrom(MODULE_NAME + "-14.2.4")
      .withModuleTo(MODULE_NAME + "-" + getModuleVersion())
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
    Criterion criterion = new Criterion();
    if (FEEFINES_TABLE.equals(tableName)) {
      for (AutomaticFeeFineType type : AutomaticFeeFineType.values()) {
        criterion.addCriterion(createFeeFineExclusionCriteria(type.getId()), "AND");
      }
    }
    pgClient .delete(tableName, criterion, result -> future.complete(null));

    get(future);
  }

  private static Criteria createFeeFineExclusionCriteria(String id) {
    return new Criteria()
      .addField("'id'")
      .setOperation("!=")
      .setVal(id);
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

  protected static <T> T get(Future<T> future) {
    try {
      return future.toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  protected static <T> T get(CompletableFuture<T> future) {
    try {
      return future.get(120, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  protected <T> void createEntity(String path, T entity) {
    ObjectMapper mapper = new ObjectMapper()
      .setDateFormat(new SimpleDateFormat(DATE_TIME_FORMAT))
      .setTimeZone(TimeZone.getTimeZone("UTC"));

    try {
      RestAssured.given()
        .spec(getRequestSpecification())
        .body(mapper.writeValueAsString(entity))
        .when()
        .post(path)
        .then()
        .statusCode(HttpStatus.SC_CREATED)
        .contentType(ContentType.JSON);
    } catch (JsonProcessingException e) {
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

  protected <T> void replaceEntity(String path, Object entity) {
    JsonObject entityJson = mapFrom(entity);

    RestAssured.given()
      .spec(getRequestSpecification())
      .body(entityJson.encodePrettily())
      .when()
      .put(format("%s/%s", path, entityJson.getString("id")))
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

  protected StubMapping createStub(String url, ResponseDefinitionBuilder responseBuilder) {
    return createStub(urlPathEqualTo(url), responseBuilder);
  }

  private StubMapping createStubForPathMatching(String regex,
    ResponseDefinitionBuilder responseBuilder) {

    return createStub(urlPathMatching(regex), responseBuilder);
  }

  public <T> StubMapping createStubForCollection(String url, Collection<T> returnObjects,
    String collectionName) {

    JsonArray results = returnObjects.stream()
      .map(JsonObject::mapFrom)
      .collect(collectingAndThen(toList(), JsonArray::new));

    JsonObject response = new JsonObject()
      .put(collectionName, results)
      .put("totalRecords", returnObjects.size());

    return createStubForPathMatching(url, aResponse().withBody(response.encodePrettily()));
  }

  private StubMapping createStub(UrlPathPattern urlPathPattern,
    ResponseDefinitionBuilder responseBuilder) {

    return getOkapi().stubFor(fillHeader(WireMock.get(urlPathPattern))
      .willReturn(responseBuilder));
  }

  public StubMapping createStub(RequestMethod method, String url,
    ResponseDefinitionBuilder responseBuilder) {

    return createStub(request(method.getName(), urlPathEqualTo(url)), responseBuilder);
  }

  public StubMapping createStub(MappingBuilder mappingBuilder,
    ResponseDefinitionBuilder responseBuilder) {

    return getOkapi().stubFor(fillHeader(mappingBuilder)
      .willReturn(responseBuilder));
  }

  private MappingBuilder fillHeader(MappingBuilder mappingBuilder) {
      return mappingBuilder
        .withHeader(ACCEPT, matching(APPLICATION_JSON))
        .withHeader(OKAPI_HEADER_TENANT, matching(TENANT_NAME))
        .withHeader(OKAPI_HEADER_TOKEN, matching(OKAPI_TOKEN))
        .withHeader(OKAPI_URL_HEADER, matching(getOkapiUrl()));
  }

  public void removeStub(StubMapping stubMapping) {
    getOkapi().removeStub(stubMapping);
  }
}
