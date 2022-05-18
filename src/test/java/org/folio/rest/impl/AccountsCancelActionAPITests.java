package org.folio.rest.impl;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.folio.rest.domain.MonetaryValue.ZERO;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.CANCELLED_AS_ERROR;
import static org.folio.rest.utils.LogEventUtils.fetchLogEventPayloads;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCancelClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCancelClient;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.test.support.EntityBuilder.buildAccount;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.BulkActionSuccessResponse;
import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.jaxrs.model.CancelBulkActionRequest;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.folio.test.support.matcher.LogEventMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AccountsCancelActionAPITests extends ApiTests {
  private static final String FEE_FINE_ACTIONS = "feefineactions";
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String ACCOUNT_ID = randomId();

  private final ResourceClient actionsClient = buildFeeFineActionsClient();
  private final ResourceClient accountCancelClient = buildAccountCancelClient(ACCOUNT_ID);
  private final ResourceClient accountBulkCancelClient = buildAccountBulkCancelClient();

  @Before
  public void setUp() {
    removeAllFromTable(ACCOUNTS_TABLE);
    removeAllFromTable(FEE_FINE_ACTIONS);
  }

  @Test
  public void cancelActionShouldCancelAccount() {
    Account accountToPost = postAccount();

    CancelActionRequest cancelActionRequest = createCancelActionRequest();
    accountCancelClient.attemptCreate(cancelActionRequest)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .body("accountId", is(ACCOUNT_ID))
      .body("remainingAmount", is(ZERO.toString()))
      .body(FEE_FINE_ACTIONS, hasSize(1));

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("status.name", is("Closed"))
      .body("paymentStatus.name", is(CANCELLED_AS_ERROR.value()))
      .body("remaining", is(0.0f));

    actionsClient.getAll()
      .then()
      .log().body()
      .body(FEE_FINE_ACTIONS, hasSize(1))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("amountAction",
          is((float) accountToPost.getAmount().toDouble())),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is(CANCELLED_AS_ERROR.value()))
      )));

    assertThat(fetchLogEventPayloads(getOkapi()).get(0),
      is(LogEventMatcher.cancelledActionLogEventPayload(accountToPost, cancelActionRequest)));
  }

  @Test
  public void shouldUseCancellationReason() {
    final String cancellationReason = "Cancelled item returned";
    final Account accountToPost = postAccount();
    final CancelActionRequest cancelActionRequest = createCancelActionRequest()
      .withCancellationReason(cancellationReason);

    accountCancelClient.attemptCreate(cancelActionRequest)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .body("remainingAmount", is(ZERO.toString()))
      .body("accountId", is(ACCOUNT_ID));

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("status.name", is("Closed"))
      .body("paymentStatus.name", is(cancellationReason))
      .body("remaining", is(0.0f));

    actionsClient.getAll()
      .then()
      .log().body()
      .body(FEE_FINE_ACTIONS, hasSize(1))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("amountAction", is((float) accountToPost.getAmount().toDouble())),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is(cancellationReason))
      )));
  }

  @Test
  public void shouldReturn404WhenAccountDoesNotExist() {
    accountCancelClient.attemptCreate(createCancelActionRequest())
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .body(equalTo(format("Fee/fine ID %s not found", ACCOUNT_ID)));
  }

  @Test
  public void shouldReturn422WhenAccountIsClosed() {
    Account account = buildAccount(ACCOUNT_ID);
    account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
    postAccount(account);

    accountCancelClient.attemptCreate(createCancelActionRequest())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("errorMessage", is("Fee/fine is already closed"));
  }

  @Test
  public void bulkCancelShouldReturn404WhenAccountDoesNotExist() {
    accountBulkCancelClient.attemptCreate(createBulkCancelActionRequest(List.of(ACCOUNT_ID)))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND)
      .body(equalTo(format("Fee/fine ID %s not found", ACCOUNT_ID)));
  }

  @Test
  public void bulkCancelShouldReturn422WhenAccountIsClosed() {
    Account account = buildAccount(ACCOUNT_ID);
    account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
    postAccount(account);

    accountBulkCancelClient.attemptCreate(createBulkCancelActionRequest(List.of(account.getId())))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("errorMessage", is("Fee/fine is already closed"));
  }

  @Test
  public void bulkCancelActionShouldCancelAccount() {
    List<String> accountIds = List.of(randomId(), randomId());
    List<Account> accountsToPost = accountIds.stream()
      .map(EntityBuilder::buildAccount)
      .collect(Collectors.toList());
    accountsToPost.forEach(this::postAccount);

    final var cancelActionRequest = createBulkCancelActionRequest(accountIds);

    final var resp = accountBulkCancelClient.attemptCreate(cancelActionRequest)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .log().body()
      .extract().as(BulkActionSuccessResponse.class);

    Assert.assertThat(resp.getAccountIds(),
      containsInAnyOrder(accountIds.get(0), accountIds.get(1)));

    accountIds.forEach(accountId -> accountsClient.getById(accountId)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("status.name", is("Closed"))
      .body("paymentStatus.name", is(CANCELLED_AS_ERROR.value()))
      .body("remaining", is(0.0f)));

    actionsClient.getAll()
      .then()
      .log().body()
      .body(FEE_FINE_ACTIONS, hasSize(2))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("accountId", is(accountIds.get(0))),
        hasJsonPath("amountAction", is(
          (float) accountsToPost.get(0).getAmount().toDouble())),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is(CANCELLED_AS_ERROR.value()))
      )))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("accountId", is(accountIds.get(1))),
        hasJsonPath("amountAction", is(accountsToPost.get(1).getAmount().getAmount().floatValue())),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is(CANCELLED_AS_ERROR.value()))
        )
      ));

    fetchLogEventPayloads(getOkapi()).forEach(payload -> assertThat(payload,
      is(either(
        LogEventMatcher.cancelledActionLogEventPayload(accountsToPost.get(0), cancelActionRequest))
        .or(LogEventMatcher.cancelledActionLogEventPayload(accountsToPost.get(1),
          cancelActionRequest)))));
  }

  private CancelActionRequest createCancelActionRequest() {
    return new CancelActionRequest()
      .withComments("Comment")
      .withNotifyPatron(false)
      .withServicePointId("7c5abc9f-f3d7-4856-b8d7-6712462ca007")
      .withUserName("Test User");
  }

  private CancelBulkActionRequest createBulkCancelActionRequest(List<String> accountIds) {
    return new CancelBulkActionRequest()
      .withComments("Comment")
      .withAccountIds(accountIds)
      .withNotifyPatron(false)
      .withServicePointId("7c5abc9f-f3d7-4856-b8d7-6712462ca007")
      .withUserName("Test User");
  }

  private Account postAccount() {
    Account accountToPost = buildAccount(ACCOUNT_ID);
    postAccount(accountToPost);
    return accountToPost;
  }

  private void postAccount(Account account) {
    accountsClient.create(account)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
  }
}
