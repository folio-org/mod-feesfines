package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.test.support.matcher.FeeFineMatchers.hasAllAutomaticFeeFineTypesFor18_3;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import io.restassured.response.Response;
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
    OverdueFinePolicies policy = client.get("/overdue-fines-policies")
      .as(OverdueFinePolicies.class);

    assertThat(policy.getTotalRecords(), is(1));

    final OverdueFinePolicy overduePolicy = policy
      .getOverdueFinePolicies().get(0);

    // This id is used in mod-circulation-storage
    // if you're going to change it,
    // circulation rules must be updated as well
    assertThat(overduePolicy.getId(), is("cd3f6cac-fa17-4079-9fae-2fb28e521412"));

    context.completeNow();
  }

  @Test
  public void shouldCreateKafkaTopicsDuringTenantInitialization(VertxTestContext context) {
    AtomicReference<String> tenantIdReference = new AtomicReference<>();
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.succeededFuture(), tenantIdReference));

    var response = client.post("/_/tenant", getTenantAttributes());

    assertThat(response.getStatusCode(), is(204));
    assertThat(tenantIdReference.get(), is(TENANT_NAME));

    context.completeNow();
  }

  @Test
  public void shouldFailIfCannotCreateKafkaTopics(VertxTestContext context) {
    String expectedError = "Kafka unavailable";
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.failedFuture(expectedError), new AtomicReference<>()));

    var response = client.post("/_/tenant", getTenantAttributes());

    assertThat(response.getStatusCode(), is(500));
    assertThat(response.getBody().asString(), notNullValue());
    assertThat(response.getBody().asString().contains(expectedError), is(true));

    context.completeNow();
  }

  @Test
  public void shouldDeleteKafkaTopicsDuringTenantPurge(VertxTestContext context) {
    AtomicReference<String> tenantIdReference = new AtomicReference<>();
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.succeededFuture(), new AtomicReference<>(),
      Future.succeededFuture(), tenantIdReference));

    Throwable failure = null;
    try {
      Response response = disableTenant(true);

      assertThat(response.getStatusCode(), is(204));
      assertThat(tenantIdReference.get(), is(TENANT_NAME));
    } catch (Throwable t) {
      failure = t;
    }

    recreateTenantAndComplete(context, failure);
  }

  @Test
  public void shouldDeleteKafkaTopicsWhenTenantDisableUsesBlankModuleTo(VertxTestContext context) {
    AtomicReference<String> tenantIdReference = new AtomicReference<>();
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.succeededFuture(), new AtomicReference<>(),
      Future.succeededFuture(), tenantIdReference));

    Throwable failure = null;
    try {
      Response response = disableTenant(getTenantDisableAttributes().withModuleTo(" ").withPurge(true));

      assertThat(response.getStatusCode(), is(204));
      assertThat(tenantIdReference.get(), is(TENANT_NAME));
    } catch (Throwable t) {
      failure = t;
    }

    recreateTenantAndComplete(context, failure);
  }

  @Test
  public void shouldNotDeleteKafkaTopicsWhenTenantDisableDoesNotPurge(VertxTestContext context) {
    AtomicReference<String> tenantIdReference = new AtomicReference<>();
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.succeededFuture(), new AtomicReference<>(),
      Future.succeededFuture(), tenantIdReference));

    Throwable failure = null;
    try {
      Response response = disableTenant(false);

      assertThat(response.getStatusCode(), is(204));
      assertNull(tenantIdReference.get());
    } catch (Throwable t) {
      failure = t;
    }

    recreateTenantAndComplete(context, failure);
  }

  @Test
  public void shouldNotDeleteKafkaTopicsWhenTenantDisableOmitsPurge(VertxTestContext context) {
    AtomicReference<String> tenantIdReference = new AtomicReference<>();
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.succeededFuture(), new AtomicReference<>(),
      Future.succeededFuture(), tenantIdReference));

    Throwable failure = null;
    try {
      Response response = disableTenant(getTenantDisableAttributes());

      assertThat(response.getStatusCode(), is(204));
      assertNull(tenantIdReference.get());
    } catch (Throwable t) {
      failure = t;
    }

    recreateTenantAndComplete(context, failure);
  }

  @Test
  public void shouldFailIfCannotDeleteKafkaTopicsDuringTenantPurge(VertxTestContext context) {
    String expectedError = "Kafka delete unavailable";
    TenantRefAPI.setKafkaServiceFactory(vertx -> kafkaService(vertx,
      Future.succeededFuture(), new AtomicReference<>(),
      Future.failedFuture(expectedError), new AtomicReference<>()));

    Throwable failure = null;
    try {
      Response response = disableTenant(true);

      assertThat(response.getStatusCode(), is(500));
      assertThat(response.getBody().asString(), notNullValue());
      assertThat(response.getBody().asString().contains(expectedError), is(true));
    } catch (Throwable t) {
      failure = t;
    }

    recreateTenantAndComplete(context, failure);
  }

  @Test
  public void shouldFailIfKafkaTopicDeletionThrowsDuringTenantPurge(VertxTestContext context) {
    String expectedError = "Kafka delete exception";
    TenantRefAPI.setKafkaServiceFactory(vertx -> new KafkaService(vertx) {
      @Override
      public Future<Void> createTopics(String tenantId) {
        return Future.succeededFuture();
      }

      @Override
      public Future<Void> deleteTopics(String tenantId) {
        throw new IllegalStateException(expectedError);
      }
    });

    Throwable failure = null;
    try {
      Response response = disableTenant(true);

      assertThat(response.getStatusCode(), is(500));
      assertThat(response.getBody().asString(), notNullValue());
      assertThat(response.getBody().asString().contains(expectedError), is(true));
    } catch (Throwable t) {
      failure = t;
    }

    recreateTenantAndComplete(context, failure);
  }

  @Test
  public void lostItemFeePolicyLoaded(VertxTestContext context) {
    LostItemFeePolicies policy = client.get("/lost-item-fees-policies")
      .as(LostItemFeePolicies.class);

    assertThat(policy.getTotalRecords(), is(1));

    final LostItemFeePolicy lostItemFeePolicy = policy
      .getLostItemFeePolicies().get(0);

    assertThat(lostItemFeePolicy.getId(), is("ed892c0e-52e0-4cd9-8133-c0ef07b4a709"));

    context.completeNow();
  }

  @Test
  public void shouldFailIfNoOkapiUrlHeaderSpecified(VertxTestContext context) {
    final RequestSpecification spec = RestAssured.given()
      .baseUri(getOkapiUrl())
      .contentType(MediaType.APPLICATION_JSON)
      .header(new Header(OKAPI_HEADER_TENANT, TENANT_NAME))
      .header(new Header(OKAPI_HEADER_TOKEN, OKAPI_TOKEN))
      .body(getTenantAttributes());

    var response = spec.post("/_/tenant");

    assertThat(response.getStatusCode(), is(500));
    assertThat(response.getBody().asString(), notNullValue());
    assertThat(response.getBody().asString()
      .contains("No X-Okapi-Url header"), is(true));

    context.completeNow();
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

    var response = client.post("/_/tenant", getTenantAttributes());

    assertThat(response.getStatusCode(), is(500));
    assertThat(response.getBody().asString(), notNullValue());
    assertThat(response.getBody().asString()
      .contains("EventDescriptor was not registered"), is(true));

    context.completeNow();
  }

  private static KafkaService noOpKafkaService(Vertx vertx) {
    return kafkaService(vertx, Future.succeededFuture(), new AtomicReference<>(),
      Future.succeededFuture(), new AtomicReference<>());
  }

  private Response disableTenant(boolean purge) {
    return disableTenant(getTenantDisableAttributes().withPurge(purge));
  }

  private Response disableTenant(org.folio.rest.jaxrs.model.TenantAttributes attributes) {
    return client.post("/_/tenant", attributes);
  }

  private static org.folio.rest.jaxrs.model.TenantAttributes getTenantDisableAttributes() {
    org.folio.rest.jaxrs.model.TenantAttributes attributes = getTenantAttributes();
    return attributes
      .withModuleFrom(attributes.getModuleTo())
      .withModuleTo(null);
  }

  private void recreateTenantAndComplete(VertxTestContext context, Throwable testFailure) {
    TenantRefAPI.setKafkaServiceFactory(TenantRefAPITest::noOpKafkaService);
    try {
      Response cleanupResponse = client.post("/_/tenant", getTenantAttributes());
      if (testFailure != null) {
        context.failNow(testFailure);
      } else if (cleanupResponse.getStatusCode() != 204) {
        context.failNow(new AssertionError("Tenant recreation failed with HTTP "
          + cleanupResponse.getStatusCode() + ": "
          + cleanupResponse.getBody().asString()));
      } else {
        context.completeNow();
      }
    } catch (Throwable cleanupFailure) {
      context.failNow(testFailure != null ? testFailure : cleanupFailure);
    } finally {
      TenantRefAPI.resetKafkaServiceFactory();
    }
  }

  private static KafkaService kafkaService(Vertx vertx, Future<Void> createResult,
    AtomicReference<String> createdTenantIdReference) {

    return kafkaService(vertx, createResult, createdTenantIdReference,
      Future.succeededFuture(), new AtomicReference<>());
  }

  private static KafkaService kafkaService(Vertx vertx, Future<Void> createResult,
    AtomicReference<String> createdTenantIdReference, Future<Void> deleteResult,
    AtomicReference<String> deletedTenantIdReference) {

    return new KafkaService(vertx) {
      @Override
      public Future<Void> createTopics(String tenantId) {
        createdTenantIdReference.set(tenantId);
        return createResult;
      }

      @Override
      public Future<Void> deleteTopics(String tenantId) {
        deletedTenantIdReference.set(tenantId);
        return deleteResult;
      }
    };
  }
}
