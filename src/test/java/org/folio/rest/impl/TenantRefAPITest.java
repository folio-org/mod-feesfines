package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Collections;

import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
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

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

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
