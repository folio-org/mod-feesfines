package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.vertx.core.Future.succeededFuture;
import static java.util.Arrays.asList;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.FeefinedataCollection;
import org.folio.rest.jaxrs.model.LostItemFeePolicies;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicies;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.test.support.ApiTests;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class TenantRefAPITest extends ApiTests {

  @Test
  public void overdueFinePolicyLoaded(TestContext context) {
    succeededFuture(client.get("/overdue-fines-policies"))
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
      }).onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void lostItemFeePolicyLoaded(TestContext context) {
    succeededFuture(client.get("/lost-item-fees-policies"))
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
      }).onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldFailIfNoOkapiUrlHeaderSpecified(TestContext context) {
    final RequestSpecification spec = RestAssured.given()
      .baseUri(getOkapiUrl())
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TENANT_NAME))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN))
      .body(getTenantAttributes());

    succeededFuture(spec.post("/_/tenant"))
      .map(response -> {
        context.assertEquals(response.getStatusCode(), 500);
        context.assertNotNull(response.getBody().asString());
        context.assertTrue(response.getBody().asString()
          .contains("No X-Okapi-Url header"));

        return context;
      }).onComplete(context.asyncAssertSuccess());
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

    succeededFuture(client.get("/feefines"))
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
      }).onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void shouldFailIfCannotRegisterInPubSub(TestContext context) {
    getOkapi().stubFor(post(urlPathMatching("/pubsub/.+"))
      .willReturn(aResponse().withStatus(500).withBody("Pubsub unavailable")));

    succeededFuture(client.post("/_/tenant", getTenantAttributes()))
      .map(response -> {
        context.assertEquals(response.getStatusCode(), 500);
        context.assertNotNull(response.getBody().asString());
        context.assertTrue(response.getBody().asString()
          .contains("EventDescriptor was not registered"));

        return context;
      }).onComplete(context.asyncAssertSuccess());
  }
}
