package org.folio.rest.impl;

import javax.ws.rs.core.MediaType;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.utils.OkapiClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
import static java.util.Arrays.asList;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.junit.Assert.assertEquals;

@RunWith(VertxUnitRunner.class)
public class TenantRefAPITest {
  private static final int PORT = NetworkUtils.nextFreePort();
  private static Vertx vertx;

  private final OkapiClient okapiClient = new OkapiClient(PORT);

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    Async async = context.async();
    vertx = Vertx.vertx();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    final TenantClient tenantClient = createTenantClient();

    DeploymentOptions restDeploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put("http.port", PORT));

    vertx.deployVerticle(RestVerticle.class.getName(), restDeploymentOptions,
      res -> {
        try {
          tenantClient.postTenant(getTenantAttributes(), result -> async.complete());
        } catch (Exception e) {
          context.fail(e);
        }
      });
  }

  @AfterClass
  public static void tearDownClass(final TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess(res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
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
      .port(PORT)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, "test_tenant"))
      .header(new Header(OKAPI_HEADER_TOKEN, "test_token"))
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

    Comparator<Feefine> byId = (f1, f2) -> StringUtils.compare(f1.getId(), f2.getId());
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

  private static TenantClient createTenantClient() {
    return new TenantClient("http://localhost:" + PORT,
      "test_tenant", "test_token");
  }

  private static TenantAttributes getTenantAttributes() {
    final Parameter loadReferenceParameter = new Parameter()
      .withKey("loadReference").withValue("true");

    return new TenantAttributes()
      .withModuleFrom("mod-feesfines:1.0")
      .withModuleTo("mod-feesfines:2.0")
      .withParameters(Collections.singletonList(loadReferenceParameter));
  }
}
