package org.folio.rest.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.FeefinedataCollection;
import org.folio.rest.jaxrs.model.LostItemFeePolicies;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicies;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.rest.utils.OkapiClient;
import org.folio.util.pubsub.PubSubClientUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.MediaType;

@RunWith(VertxUnitRunner.class)
@PrepareForTest(PubSubClientUtils.class)
public class TenantRefAPITest {
  private static final String MODULE_NAME_TEMPLATE = "%s-%s";
  private static final Vertx vertx = Vertx.vertx();
  private static final OkapiClient okapiClient = new OkapiClient();

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    Async async = context.async();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    DeploymentOptions deploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", okapiClient.getPort()));

    vertx.deployVerticle(RestVerticle.class.getName(), deploymentOptions, r -> async.complete());
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Before
  public void beforeEach(final TestContext context) {
    Async async = context.async();

    PowerMockito.mockStatic(PubSubClientUtils.class);
    when(PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
      .thenReturn(completedFuture(true));

    try {
      new TenantClient(okapiClient.getUrl(), okapiClient.getTenant(), okapiClient.getToken())
        .postTenant(getTenantAttributes(), result -> {
          // start verifying behavior
          PowerMockito.verifyStatic(PubSubClientUtils.class);
          // call the method which is being verified
          PubSubClientUtils.registerModule(any(OkapiConnectionParams.class));
          async.complete();
        });
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void overdueFinePolicyLoaded(TestContext context) {
    succeededFuture(okapiClient.get("/overdue-fines-policies"))
      .map(response -> response.as(OverdueFinePolicies.class))
      .map(policy -> {
        context.assertEquals(policy.getTotalRecords(), 1);

        final OverdueFinePolicy overduePolicy = policy
          .getOverdueFinePolicies().get(0);

        // This id is used in mod-circulation-storage
        // if you're going to change it,
        // circulation rules must be updated as well
        context.assertEquals(overduePolicy.getId(),
          "cd3f6cac-fa17-4079-9fae-2fb28e521412");
        return context;
      }).setHandler(context.asyncAssertSuccess());
  }

  @Test
  public void lostItemFeePolicyLoaded(TestContext context) {
    succeededFuture(okapiClient.get("/lost-item-fees-policies"))
      .map(response -> response.as(LostItemFeePolicies.class))
      .map(policy -> {
        context.assertEquals(policy.getTotalRecords(), 1);

        final LostItemFeePolicy lostItemFeePolicy = policy
          .getLostItemFeePolicies().get(0);

        // This id is used in mod-circulation-storage
        // if you're going to change it,
        // circulation rules must be updated as well
        context.assertEquals(lostItemFeePolicy.getId(),
          "ed892c0e-52e0-4cd9-8133-c0ef07b4a709");
        return context;
      }).setHandler(context.asyncAssertSuccess());
  }

  @Test
  public void shouldFailIfNoOkapiUrlHeaderSpecified(TestContext context) {
    final RequestSpecification spec = RestAssured.given()
      .port(okapiClient.getPort())
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, okapiClient.getTenant()))
      .header(new Header(OKAPI_HEADER_TOKEN, okapiClient.getToken()))
      .body(getTenantAttributes());

    succeededFuture(spec.post("/_/tenant"))
      .map(response -> {
        context.assertEquals(response.getStatusCode(), 500);
        context.assertNotNull(response.getBody().asString());
        context.assertTrue(response.getBody().asString()
          .contains("No X-Okapi-Url header"));

        return context;
      }).setHandler(context.asyncAssertSuccess());
  }

  @Test
  public void feesFinesAreLoaded(TestContext context) {
    // these are default fees/fines, see resources/templates/db_scripts/populate-feefines.sql
    final List<Feefine> expectedFeeFines = asList(
      new Feefine()
        .withId("9523cb96-e752-40c2-89da-60f3961a488d")
        .withFeeFineType("Overdue fine")
        .withAutomatic(true),
      new Feefine()
        .withId("cf238f9f-7018-47b7-b815-bb2db798e19f")
        .withFeeFineType("Lost item fee")
        .withAutomatic(true),
      new Feefine()
        .withId("c7dede15-aa48-45ed-860b-f996540180e0")
        .withFeeFineType("Lost item processing fee")
        .withAutomatic(true),
      new Feefine()
        .withId("d20df2fb-45fd-4184-b238-0d25747ffdd9")
        .withFeeFineType("Replacement processing fee")
        .withAutomatic(true)
    );

    Comparator<Feefine> byId = Comparator.comparing(Feefine::getId);
    expectedFeeFines.sort(byId);

    succeededFuture(okapiClient.get("/feefines"))
      .map(response -> response.as(FeefinedataCollection.class))
      .map(collection -> {
        final List<Feefine> createdFeeFines = collection.getFeefines();
        context.assertEquals(expectedFeeFines.size(), createdFeeFines.size());
        createdFeeFines.sort(byId);

        for (int i = 0; i < expectedFeeFines.size(); i++) {
          final Feefine expected = expectedFeeFines.get(i);
          final Feefine actual = createdFeeFines.get(i);

          assertEquals(expected.getId(), actual.getId());
          assertEquals(expected.getFeeFineType(), actual.getFeeFineType());
          assertEquals(expected.getAutomatic(), actual.getAutomatic());
        }
        return context;
      }).setHandler(context.asyncAssertSuccess());
  }

  private static TenantAttributes getTenantAttributes() {
    final Parameter loadReferenceParameter = new Parameter()
      .withKey("loadReference").withValue("true");

    String moduleName = PomReader.INSTANCE.getModuleName();

    return new TenantAttributes()
      .withModuleFrom(format(MODULE_NAME_TEMPLATE, moduleName, "14.2.4"))
      .withModuleTo(format(MODULE_NAME_TEMPLATE, moduleName, PomReader.INSTANCE.getVersion()))
      .withParameters(Collections.singletonList(loadReferenceParameter));
  }
}
