package org.folio.rest.impl.accountactionchecks;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.BulkCheckActionRequest;
import org.folio.rest.jaxrs.model.CheckActionRequest;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.hamcrest.CoreMatchers;

public class AccountsActionChecksAPITestsBase extends ApiTests {

  protected Account firstAccount;
  protected Account secondAccount;

  protected static final String ACCOUNTS_TABLE = "accounts";
  protected static final String ERROR_MESSAGE_ALREADY_CLOSED = "Fee/fine is already closed";
  protected static final MonetaryValue ACCOUNT_INITIAL_AMOUNT = new MonetaryValue(9.00);
  protected static final MonetaryValue ACCOUNT_REMAINING_AMOUNT = new MonetaryValue(4.55);
  protected static final MonetaryValue REQUESTED_AMOUNT = new MonetaryValue(1.23);
  protected static final String ERROR_MESSAGE_MUST_BE_POSITIVE = "Amount must be positive";
  protected static final String ERROR_MESSAGE_INVALID_AMOUNT = "Invalid amount entered";

  private static final String REQUESTED_AMOUNT_STRING = String.valueOf(REQUESTED_AMOUNT);

  protected void actionShouldNotBeAllowed(boolean bulk,
    ResourceClient accountsActionCheckClient, String amount, String errorMessage) {

    accountsActionCheckClient.attemptCreate(createRequest(bulk, amount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(containsString(errorMessage))
      .body("allowed", is(false))
      .body("accountIds",
        bulk ? CoreMatchers.is(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
          : nullValue())
      .body("amount", is(amount));
  }

  protected void actionShouldBeAllowed(boolean bulk, ResourceClient actionCheckClient,
    String remaining) {

    final var request = createRequest(bulk, REQUESTED_AMOUNT_STRING);
    final var response = actionCheckClient.attemptCreate(request);
    response
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("allowed", is(true))
      .body("accountIds", bulk ? is(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
        : nullValue())
      .body("amount", is(REQUESTED_AMOUNT_STRING))
      .body("remainingAmount", is(remaining));
  }

  protected void actionCheckAmountShouldNotBeAllowedWithExceededAmount(boolean bulk,
    ResourceClient actionCheckClient) {
    String expectedErrorMessage = "Requested amount exceeds remaining amount";
    String amount = String.valueOf(
      bulk ?
        ACCOUNT_REMAINING_AMOUNT.multiply(new MonetaryValue(2.0)).add(
          new MonetaryValue(1.0)) : REQUESTED_AMOUNT.add(new MonetaryValue(10.0)));
    Object request = buildRequest(bulk, amount);

    baseActionCheckAmountShouldNotBeAllowedWithExceededAmount(actionCheckClient,
      expectedErrorMessage, request, amount);
  }

  protected void actionCheckRefundAmountShouldNotBeAllowedWithExceededAmount(boolean bulk,
    ResourceClient actionCheckClient) {
    String expectedErrorMessage =
      "Refund amount must be greater than zero and less than or equal to Selected amount";

    String amount = String.valueOf(REQUESTED_AMOUNT.add(new MonetaryValue(10.0)));
    Object request = buildRequest(bulk, amount);

    baseActionCheckAmountShouldNotBeAllowedWithExceededAmount(
      actionCheckClient, expectedErrorMessage, request, amount);
  }

  private Object buildRequest(boolean bulk, String amount) {
    return bulk
      ? new BulkCheckActionRequest()
      .withAmount(amount)
      .withAccountIds(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
      : new CheckActionRequest().withAmount(amount);
  }

  void baseActionCheckAmountShouldNotBeAllowedWithExceededAmount(
    ResourceClient actionCheckClient, String expectedErrorMessage, Object request, String amount) {

    actionCheckClient.attemptCreate(request)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("errorMessage", is(expectedErrorMessage))
      .body("allowed", is(false))
      .body("amount", is(amount));
  }

  protected void actionCheckShouldNotFailForNonExistentAccount(boolean bulk,
    ResourceClient actionCheckClient) {

    actionCheckClient.attemptCreate(createRequest(bulk,
      REQUESTED_AMOUNT_STRING))
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  protected void actionCheckAmountShouldNotBeAllowedForClosedAccount(boolean bulk,
    ResourceClient client) {

    closeAllAccounts();

    client.attemptCreate(createRequest(bulk, REQUESTED_AMOUNT_STRING))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("allowed", is(false))
      .body("accountIds", bulk ? is(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
        : nullValue())
      .body("amount", is(REQUESTED_AMOUNT_STRING))
      .body("errorMessage", is(ERROR_MESSAGE_ALREADY_CLOSED));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  private void closeAllAccounts() {
    Stream.of(firstAccount, secondAccount)
      .forEach(account -> {
        account.setRemaining(new MonetaryValue(0.0));
        account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
        account.setDateClosed(new Date());
        accountsClient.update(account.getId(), account);
      });
  }

  protected void successfulActionCheckHandlesLongDecimalsCorrectly(ResourceClient client) {
    firstAccount.setRemaining(
      new MonetaryValue(new BigDecimal("1.235987654321"))); // will be rounded to 1.24
    accountsClient.update(firstAccount.getId(), firstAccount);

    client.attemptCreate(new CheckActionRequest().withAmount("1.004987654321")) // rounded to 1.00
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("allowed", is(true))
      .body("amount", is("1.00"))
      .body("remainingAmount", is("0.24")); // 1.24 - 1.00

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  protected void failedActionCheckReturnsInitialRequestedAmount(ResourceClient client) {
    firstAccount.setRemaining(new MonetaryValue(0.99));
    accountsClient.update(firstAccount.getId(), firstAccount);

    String requestedAmount = "1.004123456789"; // rounded to 1.00 when compared to account balance

    client.attemptCreate(new CheckActionRequest().withAmount(requestedAmount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("allowed", is(false))
      .body("amount", is(requestedAmount))
      .body("errorMessage", is("Requested amount exceeds remaining amount"));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  protected Account createAccount() {
    Account accountToPost = EntityBuilder
      .buildAccount(ACCOUNT_INITIAL_AMOUNT,
        ACCOUNT_REMAINING_AMOUNT);
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
    return accountToPost;
  }

  protected Object createRequest(boolean bulk, String amount) {
    if (bulk) {
      return new BulkCheckActionRequest()
        .withAccountIds(Arrays.asList(firstAccount.getId(), secondAccount.getId()))
        .withAmount(amount);
    } else {
      return new CheckActionRequest().withAmount(amount);
    }
  }
}
