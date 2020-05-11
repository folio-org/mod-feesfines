package org.folio.rest.impl;

import static java.lang.String.format;

import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.OkapiClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;

public class APITests {
  protected static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String MODULE_NAME_TEMPLATE = "%s-%s";
  private static final String HTTP_PORT_PROPERTY = "http.port";

  protected static final int OKAPI_PORT = NetworkUtils.nextFreePort();
  protected static final String OKAPI_URL = "http://localhost:" + OKAPI_PORT;
  protected static final String OKAPI_TENANT = "test_tenant";
  protected static final String OKAPI_TOKEN = buildToken();
  protected static final String OKAPI_HEADER_URL = "x-okapi-url";

  protected static Vertx vertx;
  protected static OkapiClient okapiClient;
  protected static TenantClient tenantClient;

  @Rule
  public WireMockRule wireMock = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @BeforeClass
  public static void beforeAll(final TestContext context) throws Exception {
    Async async = context.async();

    vertx = Vertx.vertx();
    okapiClient = new OkapiClient(OKAPI_URL, OKAPI_TENANT, OKAPI_TOKEN);
    tenantClient = new TenantClient(OKAPI_URL, OKAPI_TENANT, OKAPI_TOKEN);

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();

    DeploymentOptions deploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put(HTTP_PORT_PROPERTY, OKAPI_PORT));

    vertx.deployVerticle(RestVerticle.class.getName(), deploymentOptions, deployment -> {
      try {
        tenantClient.postTenant(getTenantAttributes(), result -> async.complete());
      } catch (Exception e) {
        log.error(e.getMessage());
        context.fail(e);
      }
    });
  }

  @AfterClass
  public static void afterAll(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  protected static TenantAttributes getTenantAttributes() {
    final Parameter loadReferenceParameter = new Parameter()
      .withKey("loadReference").withValue("true");

    String moduleName = PomReader.INSTANCE.getModuleName();
    String currentVersion = PomReader.INSTANCE.getVersion();

    return new TenantAttributes()
      .withModuleFrom(format(MODULE_NAME_TEMPLATE, moduleName, "14.2.4"))
      .withModuleTo(format(MODULE_NAME_TEMPLATE, moduleName, currentVersion))
      .withParameters(Collections.singletonList(loadReferenceParameter));
  }

  private static String buildToken() {
    byte[] jsonBytes = new JsonObject()
      .put("user_id", "test_user")
      .toString()
      .getBytes();

    return "test." + Base64.getEncoder().encodeToString(jsonBytes);
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }
}
