package org.folio.rest.impl;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.folio.rest.utils.ResourceClients.buildAccountBulkCancelClient;
import static org.folio.rest.utils.ResourceClients.buildAccountCancelClient;
import static org.folio.rest.utils.ResourceClients.feeFineActionsClient;
import static org.folio.test.support.EntityBuilder.buildAccount;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.BulkActionRequest;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.BulkActionSuccessResponse;
import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.jaxrs.model.CancelBulkActionRequest;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AccountsCancelActionAPITests extends ApiTests {
  private static final String FEE_FINE_ACTIONS = "feefineactions";
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String ACCOUNT_ID = randomId();

  private final ResourceClient actionsClient = feeFineActionsClient();
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

    accountCancelClient.attemptCreate(createCancelActionRequest())
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .body("accountId", is(ACCOUNT_ID));

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("status.name", is("Closed"))
      .body("paymentStatus.name", is("Cancelled as error"))
      .body("remaining", is(0.0f));

    actionsClient.getAll()
      .then()
      .log().body()
      .body(FEE_FINE_ACTIONS, hasSize(1))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("amountAction", is((float) accountToPost.getAmount().doubleValue())),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is("Cancelled as error"))
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
      .body("paymentStatus.name", is("Cancelled as error"))
      .body("remaining", is(0.0f)));

    actionsClient.getAll()
      .then()
      .log().body()
      .body(FEE_FINE_ACTIONS, hasSize(2))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("accountId", is(accountIds.get(0))),
        hasJsonPath("amountAction", is((float) accountsToPost.get(0).getAmount().doubleValue())),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is("Cancelled as error"))
      )))
      .body(FEE_FINE_ACTIONS, hasItem(allOf(
        hasJsonPath("accountId", is(accountIds.get(1))),
        hasJsonPath("amountAction", is((float) accountsToPost.get(1).getAmount().doubleValue())),
        hasJsonPath("balance", is(0.0f)),
        hasJsonPath("typeAction", is("Cancelled as error"))
        )
      ));
  }

  private CancelActionRequest createCancelActionRequest() {
    return new CancelActionRequest()
      .withComments("Comment")
      .withNotifyPatron(false)
      .withServicePointId("7c5abc9f-f3d7-4856-b8d7-6712462ca007")
      .withUserName("Test User");
  }

  private BulkActionRequest createBulkCancelActionRequest(List<String> accountIds) {
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
