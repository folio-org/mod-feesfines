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

import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.MediaType;

import org.folio.rest.jaxrs.model.LostItemFeePolicies;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicies;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.service.KafkaService;
import org.folio.test.support.ApiTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class TenantRefAPITest extends ApiTests {

  @BeforeEach
  public void disableKafkaTopicCreation() {
    TenantRefAPI.setKafkaServiceFactory(TenantRefAPITest::noOpKafkaService);
  }

  @AfterEach
  public void resetKafkaTopicCreation() {
    TenantRefAPI.resetKafkaServiceFactory();
  }

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
  public void shouldCreateKafkaTopicsDuringTenantInitialization(VertxTestContext context) {
    AtomicReference<String> tenantIdReference = new AtomicReference<>();
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.succeededFuture(), tenantIdReference));

    succeededFuture(client.post("/_/tenant", getTenantAttributes()))
      .map(response -> {
        assertThat(response.getStatusCode(), is(204));
        assertThat(tenantIdReference.get(), is(TENANT_NAME));
        return context;
      }).onComplete(ar -> {
        TenantRefAPI.resetKafkaServiceFactory();
        if (ar.failed()) {
          context.failNow(ar.cause());
        } else {
          context.completeNow();
        }
      });
  }

  @Test
  public void shouldFailIfCannotCreateKafkaTopics(VertxTestContext context) {
    String expectedError = "Kafka unavailable";
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.failedFuture(expectedError), new AtomicReference<>()));

    succeededFuture(client.post("/_/tenant", getTenantAttributes()))
      .map(response -> {
        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getBody().asString(), notNullValue());
        assertThat(response.getBody().asString().contains(expectedError), is(true));
        return context;
      }).onComplete(ar -> {
        TenantRefAPI.resetKafkaServiceFactory();
        if (ar.failed()) {
          context.failNow(ar.cause());
        } else {
          context.completeNow();
        }
      });
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

  private static KafkaService noOpKafkaService(Vertx vertx) {
    return kafkaService(vertx, Future.succeededFuture(), new AtomicReference<>());
  }

  private static KafkaService kafkaService(Vertx vertx, Future<Void> result,
    AtomicReference<String> tenantIdReference) {

    return new KafkaService(vertx) {
      @Override
      public Future<Void> createTopics(String tenantId) {
        tenantIdReference.set(tenantId);
        return result;
      }
    };
  }
}
