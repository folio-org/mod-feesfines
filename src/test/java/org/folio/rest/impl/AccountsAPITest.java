package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.utils.JsonHelper.write;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.EventType;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.util.OkapiConnectionParams;
import org.folio.util.pubsub.PubSubClientUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
@PrepareForTest(PubSubClientUtils.class)
public class AccountsAPITest extends APITests{
  private static final String REST_PATH = "/accounts";
  private static final String ACCOUNTS_TABLE = "accounts";

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Before
  public void setUp(TestContext context) {
    Async async = context.async();

    wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/inventory/items.*"))
      .willReturn(aResponse().withBodyFile("items.json")));
    wireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/holdings-storage/holdings.*"))
      .willReturn(aResponse().withBodyFile("holdings.json")));

    mockStatic(PubSubClientUtils.class);
    when(PubSubClientUtils.sendEventMessage(any(Event.class), any(OkapiConnectionParams.class)))
      .thenReturn(completedFuture(true));

    PostgresClient.getInstance(vertx, OKAPI_TENANT)
      .delete(ACCOUNTS_TABLE, new Criterion(), result -> {
        if (result.failed()) {
          log.error(result.cause());
          context.fail(result.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void testAllMethodsAndEventPublishing(TestContext context) {
    Account accountToPost = createAccount();
    String accountId = accountToPost.getId();

    // create an account
    okapiClient.post(REST_PATH, toJson(accountToPost))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
    assertThatEventWasPublished(eventCaptor, accountToPost, context);

    // get all accounts
    okapiClient.get(REST_PATH)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON);

    String individualAccountUrl = buildAccountUrl(accountId);

    // get individual account by id
    okapiClient.get(individualAccountUrl)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON);

    Account accountToPut = accountToPost.withRemaining(4.55);

    // put account
    okapiClient.put(individualAccountUrl, toJson(accountToPut))
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    assertThatEventWasPublished(eventCaptor, accountToPut, context);

    // delete account
    okapiClient.delete(individualAccountUrl)
    .then()
    .statusCode(HttpStatus.SC_NO_CONTENT);

    Account accountToDelete = new Account()
      .withId(accountId)
      .withRemaining(0.00);

    assertThatEventWasPublished(eventCaptor, accountToDelete, context);
  }

  @Test
  public void canPutAccountWithEmptyAdditionalFields() {
    Account account = createAccount();

    okapiClient.post(REST_PATH, toJson(account))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    Account accountWithEmptyFields = account
      .withHoldingsRecordId(EMPTY)
      .withInstanceId(EMPTY);

    okapiClient.put(buildAccountUrl(account.getId()), toJson(accountWithEmptyFields))
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);
  }

  private void assertThatEventWasPublished(ArgumentCaptor<Event> eventCaptor, Account account,
    TestContext context) {

    verifyStatic(PubSubClientUtils.class, atLeast(1));
    PubSubClientUtils.sendEventMessage(eventCaptor.capture(), any(OkapiConnectionParams.class));

    Event event = eventCaptor.getValue();
    EventMetadata eventMetadata = event.getEventMetadata();
    JsonObject eventPayload = new JsonObject(event.getEventPayload());

    JsonObject expectedPayload = new JsonObject();
    write(expectedPayload, "userId", account.getUserId());
    write(expectedPayload, "feeFineId", account.getId());
    write(expectedPayload, "feeFineTypeId", account.getFeeFineId());
    write(expectedPayload, "balance", account.getRemaining());

    context.assertEquals(EventType.FF_BALANCE_CHANGED.name(), event.getEventType());
    context.assertEquals(PubSubClientUtils.constructModuleName(), eventMetadata.getPublishedBy());
    context.assertEquals(OKAPI_TENANT, eventMetadata.getTenantId());
    context.assertEquals(1, eventMetadata.getEventTTL());
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

  private static String toJson(Object object) {
    return JsonObject.mapFrom(object).encodePrettily();
  }

  private static String buildAccountUrl(String id) {
    return String.format("%s/%s", REST_PATH, id);
  }

}
