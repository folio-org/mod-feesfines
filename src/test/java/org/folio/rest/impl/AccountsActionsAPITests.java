package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.utils.ResourceClients.*;
import static org.folio.rest.utils.ResourceClients.accountsActionClient;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.EventType;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.ActionResponse;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.folio.util.pubsub.PubSubClientUtils;
import org.junit.Before;
import org.junit.Test;

import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;

public class AccountsActionsAPITests extends ApiTests {
  private static final String ACCOUNT_ID = randomId();
  private final ResourceClient payClient = accountsActionClient(ACCOUNT_ID, "pay");
  private final ResourceClient actionsClient = actionsClient();

  @Before
  public void beforeEach() {
    removeAllFromTable("feefineactions");
    removeAllFromTable("accounts");
  }

  @Test
  public void payShouldReturn404WhenAccountDoesNotExist() {
    payClient.post(createRequestJson(10.0))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .contentType(ContentType.TEXT)
      .body(equalTo("Account not found: " + ACCOUNT_ID));
  }

  @Test
  public void payShouldReturn422WhenRequestedAmountIsNegative() {
    testRequestWithNonPositiveAmount(-1);
  }

  @Test
  public void payShouldReturn422WhenRequestedAmountIsZero() {
    testRequestWithNonPositiveAmount(0);
  }

  private void testRequestWithNonPositiveAmount(double amount) {
    postAccount(createAccount(1.0));

    ActionResponse expectedResponse = new ActionResponse()
      .withAmount(amount)
      .withAccountId(ACCOUNT_ID)
      .withErrorMessage("Amount must be positive");

    payClient.post(createRequestJson(amount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(JSON)
      .body(equalTo(toJson(expectedResponse)));
  }

  @Test
  public void partialPaymentCreatesActionAndUpdatesAccount() {
    paymentCreatesActionAndUpdatesAccount(Action.PAY, false);
  }

  @Test
  public void fullPaymentCreatesActionAndClosesAccount() {
    paymentCreatesActionAndUpdatesAccount(Action.PAY, true);
  }

  public void paymentCreatesActionAndUpdatesAccount(Action action, boolean terminalAction) {
    // Hamcrest matching fails with doubles
    float accountBalance = 3.45f;
    float requestedAmount = terminalAction ? accountBalance : accountBalance - 1;
    float balanceAfterAction = accountBalance - requestedAmount;
    
    String expectedPaymentStatus = terminalAction ? action.getFullResult() : action.getPartialResult();
    String expectedAccountStatus = terminalAction ? "Closed" : "Open";

    Account account = createAccount(accountBalance);
    postAccount(account);

    ActionRequest request = createRequest(requestedAmount);

    String actionId = payClient.post(toJson(request))
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .body("amount", is(new Float(request.getAmount())))
      .body("accountId", is(ACCOUNT_ID))
      .extract()
      .path("feeFineActionId");

    actionsClient.getById(actionId)
      .then()
      .body("typeAction", is(expectedPaymentStatus))
      .body("comments", is(request.getComments()))
      .body("notify", is(request.getNotifyPatron()))
      .body("amountAction", is(requestedAmount))
      .body("balance", is(balanceAfterAction))
      .body("transactionInformation", is(request.getTransactionInfo()))
      .body("createdAt", is(request.getServicePointId()))
      .body("source", is(request.getUserName()))
      .body("paymentMethod", is(request.getPaymentMethod()))
      .body("accountId", is(ACCOUNT_ID))
      .body("userId", is(account.getUserId()))
      .body("id", is(actionId))
      .body("dateAction", notNullValue(String.class));

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .body("remaining", is(balanceAfterAction))
      .body("status.name", is(expectedAccountStatus))
      .body("paymentStatus.name", is(expectedPaymentStatus));

    verifyThatEventWasSent(EventType.FEE_FINE_BALANCE_CHANGED, new JsonObject()
      .put("userId", account.getUserId())
      .put("feeFineId", account.getId())
      .put("feeFineTypeId", account.getFeeFineId())
      .put("balance", account.getRemaining())
      .put("loanId", account.getLoanId()));

    if (terminalAction && account.getLoanId() != null) {
      verifyThatEventWasSent(EventType.LOAN_RELATED_FEE_FINE_CLOSED, new JsonObject()
        .put("loanId", account.getLoanId())
        .put("feeFineId", account.getId()));
    }
  }

  private Account createAccount(double amount) {
    return new Account()
      .withId(ACCOUNT_ID)
      .withOwnerId(randomId())
      .withUserId(randomId())
      .withItemId(randomId())
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(amount)
      .withRemaining(amount)
      .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
      .withStatus(new Status().withName("Open"));
  }

  private void postAccount(Account account) {
    accountsClient.create(account)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
  }

  private static ActionRequest createRequest(double amount) {
    return new ActionRequest()
      .withAmount(amount)
      .withPaymentMethod("Cash")
      .withServicePointId(randomId())
      .withTransactionInfo("Check #12345")
      .withUserName("Folio, Tester")
      .withNotifyPatron(false)
      .withComments("STAFF : staff comment \\n PATRON : patron comment");
  }

  private static String createRequestJson(double amount) {
    return toJson(createRequest(amount));
  }

  private static String toJson(Object object) {
    return JsonObject.mapFrom(object).encodePrettily();
  }

  private void verifyThatEventWasSent(EventType eventType, JsonObject eventPayload) {
    Event event = new Event()
      .withEventType(eventType.name())
      .withEventPayload(eventPayload.encode())
      .withEventMetadata(new EventMetadata()
        .withPublishedBy(PubSubClientUtils.constructModuleName())
        .withTenantId(TENANT_NAME)
        .withEventTTL(1));

    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> getOkapi().verify(postRequestedFor(urlPathEqualTo("/pubsub/publish"))
        .withRequestBody(equalToJson(toJson(event), true, true))
      ));
  }
}
