package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.test.support.matcher.FeeFineMatchers.hasAllAutomaticFeeFineTypes;

import javax.ws.rs.core.MediaType;

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
  public void feesFinesAreLoaded() {
    // these are default fees/fines, see resources/templates/db_scripts/populate-feefines.sql
    client.get("/feefines").then()
      .body(hasAllAutomaticFeeFineTypes());
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
