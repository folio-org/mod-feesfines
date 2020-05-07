package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.vertx.core.json.JsonObject.mapFrom;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.rest.utils.JsonHelper.write;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.domain.EventType;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.Criteria.Criterion;
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
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.restassured.http.ContentType;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
@PrepareForTest(PubSubClientUtils.class)
public class AccountsAPITest {
  private static final Logger logger = LoggerFactory.getLogger(FeeFinesAPITest.class);
  private static final String HTTP_PORT_PROPERTY = "http.port";
  private static final String REST_PATH = "/accounts";
  private static final String INDIVIDUAL_RECORD_PATH_TEMPLATE = REST_PATH + "/%s";
  private static final String ACCOUNTS_TABLE = "accounts";

  private static final Vertx vertx = Vertx.vertx();
  private static final OkapiClient okapiClient = new OkapiClient();

  @Rule
  public WireMockRule userMockServer = new WireMockRule(
    WireMockConfiguration.wireMockConfig()
      .dynamicPort()
      .notifier(new ConsoleNotifier(true)));

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @BeforeClass
  public static void setUpClass(final TestContext context) throws Exception {
    Async async = context.async();

    PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    TenantClient tenantClient =
      new TenantClient(okapiClient.getUrl(), okapiClient.getTenant(), okapiClient.getToken());
    DeploymentOptions restDeploymentOptions = new DeploymentOptions()
      .setConfig(new JsonObject().put(HTTP_PORT_PROPERTY, okapiClient.getPort()));
    TenantAttributes attributes = new TenantAttributes()
      .withModuleTo(String.format("mod-feesfines-%s", PomReader.INSTANCE.getVersion()));

    vertx.deployVerticle(RestVerticle.class.getName(), restDeploymentOptions, res -> {
      try {
        tenantClient.postTenant(attributes, res2 -> async.complete()
        );
      } catch (Exception e) {
        logger.error(e.getMessage());
      }
    });
  }

  @Before
  public void setUp(TestContext context) {
    Async async = context.async();

    userMockServer.stubFor(WireMock.get(WireMock.urlPathMatching("/inventory/items.*"))
      .willReturn(aResponse().withBodyFile("items.json")));

    userMockServer.stubFor(WireMock.get(WireMock.urlPathMatching("/holdings-storage/holdings.*"))
      .willReturn(aResponse().withBodyFile("holdings.json")));

    PostgresClient.getInstance(vertx, okapiClient.getTenant())
      .delete(ACCOUNTS_TABLE, new Criterion(), result -> {
        if (result.failed()) {
          logger.error(result.cause());
          context.fail(result.cause());
        } else {
          async.complete();
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
  public void testAccountsAPI(TestContext context) {
    mockStatic(PubSubClientUtils.class);
    when(PubSubClientUtils.sendEventMessage(any(Event.class), any(OkapiConnectionParams.class)))
      .thenReturn(completedFuture(true));

    Account accountToPost = createAccount();
    String accountId = accountToPost.getId();

    String individualAccountUrl = String.format(INDIVIDUAL_RECORD_PATH_TEMPLATE, accountId);

    okapiClient.post(REST_PATH, mapFrom(accountToPost).encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
    assertThatEventWasPublished(eventCaptor, accountToPost, 1, context);

    okapiClient.get(REST_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(ContentType.JSON);

    okapiClient.get(individualAccountUrl)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(ContentType.JSON);

    Account accountToPut = accountToPost.withRemaining(4.55);

    okapiClient.put(individualAccountUrl, mapFrom(accountToPut).encodePrettily())
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    assertThatEventWasPublished(eventCaptor, accountToPut, 2, context);

    okapiClient.delete(individualAccountUrl)
    .then()
    .statusCode(HttpStatus.SC_NO_CONTENT);

    Account accountToDelete = new Account()
      .withId(accountId)
      .withRemaining(0.00);

    assertThatEventWasPublished(eventCaptor, accountToDelete, 3, context);
  }

  private void assertThatEventWasPublished(ArgumentCaptor<Event> eventCaptor, Account account,
    int expectedNumberOfInvocations, TestContext context) {

    verifyStatic(PubSubClientUtils.class, times(expectedNumberOfInvocations));
    PubSubClientUtils.sendEventMessage(eventCaptor.capture(), any(OkapiConnectionParams.class));

    Event event = eventCaptor.getValue();
    context.assertEquals(EventType.FF_BALANCE_CHANGE.name(), event.getEventType());

    EventMetadata eventMetadata = event.getEventMetadata();
    context.assertEquals(PubSubClientUtils.constructModuleName(), eventMetadata.getPublishedBy());
    context.assertEquals(okapiClient.getTenant(), eventMetadata.getTenantId());
    context.assertEquals(1, eventMetadata.getEventTTL());

    JsonObject expectedPayload = new JsonObject();
    write(expectedPayload, "accountId", account.getId());
    write(expectedPayload, "userId", account.getUserId());
    write(expectedPayload, "feeFineId", account.getFeeFineId());
    write(expectedPayload, "balance", account.getRemaining());

    JsonObject eventPayload = new JsonObject(event.getEventPayload());

    context.assertEquals(expectedPayload, eventPayload);
  }

  private Account createAccount() {
    return new Account()
      .withId(randomId())
      .withOwnerId(randomId())
      .withUserId(randomId())
      .withItemId(randomId())
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(9.00)
      .withRemaining(4.55)
      .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
      .withStatus(new Status().withName("Open"));
  }

  private static String randomId() {
    return UUID.randomUUID().toString();
  }

}
