package org.folio.test.support;

import static org.folio.rest.utils.ResourceClients.buildAccountClient;
import static org.folio.rest.utils.ResourceClients.tenantClient;

import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.utils.OkapiClient;
import org.folio.rest.utils.ResourceClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ApiTests {
  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static final String TENANT_NAME = "test_tenant";
  public static final String OKAPI_URL_HEADER = "x-okapi-url";
  public static final String USER_ID = "69d9169d-06da-4622-9c18-2868bd46b60f";
  public static final String OKAPI_TOKEN = generateOkapiToken();
  @ClassRule
  public static final OkapiDeployment okapiDeployment = new OkapiDeployment();

  protected static Vertx vertx;

  protected final ResourceClient accountsClient = buildAccountClient();
  protected final OkapiClient client = new OkapiClient(getOkapiUrl());

  @BeforeClass
  public static void deployVerticle() throws Exception {
    vertx = Vertx.vertx();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    final CompletableFuture<Void> future = new CompletableFuture<>();

    vertx.deployVerticle(RestVerticle.class.getName(), createDeploymentOptions(),
      res -> future.complete(null));

    future.get(10, TimeUnit.SECONDS);
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
  public void setUpMapping() {
    okapiDeployment.setUpMapping();
  }

  private static void createTenant() {
    tenantClient().create(getTenantAttributes());
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
