package org.folio.rest.impl;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.FeefinedataCollection;
import org.folio.rest.jaxrs.model.LostItemFeePolicies;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicies;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;
import org.folio.util.pubsub.exceptions.ModuleRegistrationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Arrays.asList;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import javax.ws.rs.core.MediaType;

@RunWith(VertxUnitRunner.class)
@PrepareForTest(PubSubClientUtils.class)
public class TenantRefAPITest extends APITests {

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Before
  public void beforeEach(final TestContext context) {
    Async async = context.async();

    mockStatic(PubSubClientUtils.class);
    when(PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
      .thenReturn(CompletableFuture.completedFuture(true));

    try {
      tenantClient.postTenant(getTenantAttributes(), result -> {
        context.assertEquals(HttpStatus.SC_CREATED, result.statusCode());
        // start verifying behavior
        PowerMockito.verifyStatic(PubSubClientUtils.class);
        // verify that module registration method was invoked
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
      .port(OKAPI_PORT)
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, OKAPI_TENANT))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN))
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
  public void shouldFailWhenRegistrationInPubsubFailed(TestContext context) {
    Async async = context.async();

    String errorMessage = "Module registration failed";
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    future.completeExceptionally(new ModuleRegistrationException(errorMessage));

    when(PubSubClientUtils.registerModule(any(OkapiConnectionParams.class)))
      .thenReturn(future);

    try {
      tenantClient.postTenant(getTenantAttributes(), response -> {
        context.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.statusCode());
        response.bodyHandler(body -> {
          context.assertEquals(errorMessage, body.toString());
          async.complete();
        });
      });
    } catch (Exception e) {
      context.fail(e);
    }
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
}
