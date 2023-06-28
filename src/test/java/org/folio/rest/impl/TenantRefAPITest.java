package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.test.support.matcher.FeeFineMatchers.hasAllAutomaticFeeFineTypesFor18_3;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import javax.ws.rs.core.MediaType;

import org.folio.rest.jaxrs.model.LostItemFeePolicies;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicies;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.test.support.ApiTests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class TenantRefAPITest extends ApiTests {

  @Test
  public void overdueFinePolicyLoaded(VertxTestContext context) {
    succeededFuture(client.get("/overdue-fines-policies"))
      .map(response -> response.as(OverdueFinePolicies.class))
      .map(policy -> {
        assertThat(policy.getTotalRecords(), is(1));

        final OverdueFinePolicy overduePolicy = policy
          .getOverdueFinePolicies().get(0);

        // This id is used in mod-circulation-storage
        // if you're going to change it,
        // circulation rules must be updated as well
        assertThat(overduePolicy.getId(), is("cd3f6cac-fa17-4079-9fae-2fb28e521412"));
        return context;
      }).onComplete(context.succeedingThenComplete());
  }

  @Test
  public void lostItemFeePolicyLoaded(VertxTestContext context) {
    succeededFuture(client.get("/lost-item-fees-policies"))
      .map(response -> response.as(LostItemFeePolicies.class))
      .map(policy -> {
        assertThat(policy.getTotalRecords(), is(1));

        final LostItemFeePolicy lostItemFeePolicy = policy
          .getLostItemFeePolicies().get(0);

        // This id is used in mod-circulation-storage
        // if you're going to change it,
        // circulation rules must be updated as well
        assertThat(lostItemFeePolicy.getId(), is("ed892c0e-52e0-4cd9-8133-c0ef07b4a709"));
        return context;
      }).onComplete(context.succeedingThenComplete());
  }

  @Test
  public void shouldFailIfNoOkapiUrlHeaderSpecified(VertxTestContext context) {
    final RequestSpecification spec = RestAssured.given()
      .baseUri(getOkapiUrl())
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TENANT_NAME))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN))
      .body(getTenantAttributes());

    succeededFuture(spec.post("/_/tenant"))
      .map(response -> {
        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getBody().asString(), notNullValue());
        assertThat(response.getBody().asString()
          .contains("No X-Okapi-Url header"), is(true));
        return context;
      }).onComplete(context.succeedingThenComplete());
  }

  @Test
  public void feesFinesAreLoaded() {
    // these are default fees/fines, see resources/templates/db_scripts/populate-feefines.sql
    client.get("/feefines").then()
      .body(hasAllAutomaticFeeFineTypesFor18_3());
  }

  @Test
  public void shouldFailIfCannotRegisterInPubSub(VertxTestContext context) {
    getOkapi().stubFor(post(urlPathMatching("/pubsub/.+"))
      .willReturn(aResponse().withStatus(500).withBody("Pubsub unavailable")));

    succeededFuture(client.post("/_/tenant", getTenantAttributes()))
      .map(response -> {
        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getBody().asString(), notNullValue());
        assertThat(response.getBody().asString()
          .contains("EventDescriptor was not registered"), is(true));

        return context;
      }).onComplete(context.succeedingThenComplete());
  }
}
