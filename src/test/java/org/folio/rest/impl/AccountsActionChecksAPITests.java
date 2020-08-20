package org.folio.rest.impl;

import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.utils.ResourceClients.accountsCheckPayClient;
import static org.folio.rest.utils.ResourceClients.accountsCheckTransferClient;
import static org.folio.rest.utils.ResourceClients.accountsCheckWaiveClient;
import static org.folio.test.support.EntityBuilder.createAccount;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CheckActionRequest;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.junit.Before;
import org.junit.Test;

public class AccountsActionChecksAPITests extends ApiTests {
  private static final double ACCOUNT_BALANCE = 4.55;
  private static final String ACCOUNTS_TABLE = "accounts";
  private Account accountToPost;
  private ResourceClient accountsCheckPayClient;
  private ResourceClient accountsCheckWaiveClient;
  private ResourceClient accountsCheckTransferClient;

  @Before
  public void setUp() {
    accountToPost = postAccount();
    accountsCheckPayClient = accountsCheckPayClient(accountToPost.getId());
    accountsCheckWaiveClient = accountsCheckWaiveClient(accountToPost.getId());
    accountsCheckTransferClient = accountsCheckTransferClient(accountToPost.getId());
  }

  @Test
  public void checkPayAmountShouldBeAllowed() {
    actionCheckAmountShouldBeAllowed(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldBeAllowed() {
    actionCheckAmountShouldBeAllowed(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldBeAllowed() {
    actionCheckAmountShouldBeAllowed(accountsCheckTransferClient);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotBeAllowedWithExceededAmount() {
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(accountsCheckTransferClient);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithNegativeAmount() {
    actionCheckAmountShouldNotBeAllowedWithNegativeAmount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithNegativeAmount() {
    actionCheckAmountShouldNotBeAllowedWithNegativeAmount(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotBeAllowedWithNegativeAmount() {
    actionCheckAmountShouldNotBeAllowedWithNegativeAmount(accountsCheckTransferClient);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithZeroAmount() {
    actionCheckAmountShouldNotBeAllowedWithZeroAmount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithZeroAmount() {
    actionCheckAmountShouldNotBeAllowedWithZeroAmount(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotBeAllowedWithZeroAmount() {
    actionCheckAmountShouldNotBeAllowedWithZeroAmount(accountsCheckTransferClient);
  }

  @Test
  public void checkPayAmountShouldNotBeNumber() {
    actionCheckAmountShouldBeNumber(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeNumber() {
    actionCheckAmountShouldBeNumber(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotBeNumber() {
    actionCheckAmountShouldBeNumber(accountsCheckTransferClient);
  }

  @Test
  public void checkPayAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckAmountShouldNotFailForNonExistentAccount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckAmountShouldNotFailForNonExistentAccount(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotFailForNonExistentAccount() {
    removeAllFromTable(ACCOUNTS_TABLE);
    actionCheckAmountShouldNotFailForNonExistentAccount(accountsCheckTransferClient);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedForClosedAccount() {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedForClosedAccount() {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldNotBeAllowedForClosedAccount() {
    actionCheckAmountShouldNotBeAllowedForClosedAccount(accountsCheckTransferClient);
  }

  @Test
  public void checkPayAmountShouldHandleLongDecimalsCorrectly() {
    successfulActionCheckHandlesLongDecimalsCorrectly(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldHandleLongDecimalsCorrectly() {
    successfulActionCheckHandlesLongDecimalsCorrectly(accountsCheckWaiveClient);
  }

  @Test
  public void checkTransferAmountShouldHandleLongDecimalsCorrectly() {
    successfulActionCheckHandlesLongDecimalsCorrectly(accountsCheckTransferClient);
  }

  @Test
  public void failedCheckPayReturnsInitialRequestedAmount() {
    failedActionCheckReturnsInitialRequestedAmount(accountsCheckPayClient);
  }

  @Test
  public void failedCheckWaiveReturnsInitialRequestedAmount() {
    failedActionCheckReturnsInitialRequestedAmount(accountsCheckWaiveClient);
  }

  @Test
  public void failedCheckTransferReturnsInitialRequestedAmount() {
    failedActionCheckReturnsInitialRequestedAmount(accountsCheckTransferClient);
  }

  private void actionCheckAmountShouldBeAllowed(ResourceClient actionCheckClient) {
    CheckActionRequest accountCheckRequest = new CheckActionRequest().withAmount("3.0");

    actionCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("allowed", is(true))
      .body("amount", is("3.00")) // scaled to 2 decimal places
      .body("remainingAmount", is("1.55"));
  }

  private void actionCheckAmountShouldNotBeAllowedWithExceededAmount(
    ResourceClient accountsPayCheckClient) {

    CheckActionRequest accountCheckRequest = new CheckActionRequest();
    accountCheckRequest.withAmount("10.0");
    String expectedErrorMessage = "Requested amount exceeds remaining amount";

    accountsPayCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(containsString(expectedErrorMessage))
      .body("allowed", is(false))
      .body("amount", is(accountCheckRequest.getAmount()));
  }

  private void actionCheckAmountShouldNotBeAllowedWithNegativeAmount(
    ResourceClient accountsActionCheckClient) {

    CheckActionRequest accountCheckRequest = new CheckActionRequest();
    accountCheckRequest.withAmount("-5.0");
    String expectedErrorMessage = "Amount must be positive";

    accountsActionCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(containsString(expectedErrorMessage))
      .body("allowed", is(false))
      .body("amount", is(accountCheckRequest.getAmount()));
  }

  private void actionCheckAmountShouldNotBeAllowedWithZeroAmount(
    ResourceClient accountsActionCheckClient) {

    CheckActionRequest accountCheckRequest = new CheckActionRequest();
    accountCheckRequest.withAmount("0.0");
    String expectedErrorMessage = "Amount must be positive";

    accountsActionCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(containsString(expectedErrorMessage))
      .body("allowed", is(false))
      .body("amount", is(accountCheckRequest.getAmount()));
  }

  private void actionCheckAmountShouldBeNumber(ResourceClient accountsActionCheckClient) {
    CheckActionRequest accountCheckRequest = new CheckActionRequest();
    accountCheckRequest.withAmount("abc");
    String expectedErrorMessage = "Invalid amount entered";

    accountsActionCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(containsString(expectedErrorMessage))
      .body("allowed", is(false))
      .body("amount", is(accountCheckRequest.getAmount()));
  }

  private void actionCheckAmountShouldNotFailForNonExistentAccount(
    ResourceClient actionCheckClient) {

    CheckActionRequest accountCheckRequest = new CheckActionRequest();
    accountCheckRequest.withAmount("3.0");

    actionCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_NOT_FOUND);
  }

  private void actionCheckAmountShouldNotBeAllowedForClosedAccount(ResourceClient client) {
    accountToPost.setRemaining(0.00);
    accountToPost.getStatus().setName(FeeFineStatus.CLOSED.getValue());
    accountsClient.update(accountToPost.getId(), accountToPost);

    String amount = "1.23";

    client.attemptCreate(new CheckActionRequest().withAmount(amount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("allowed", is(false))
      .body("amount", is(amount))
      .body("errorMessage", is("Account is already closed"));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  private void successfulActionCheckHandlesLongDecimalsCorrectly(ResourceClient client) {
    accountToPost.setRemaining(1.235123456789); // will be rounded to 1.24
    accountsClient.update(accountToPost.getId(), accountToPost);

    client.attemptCreate(new CheckActionRequest().withAmount("1.004123456789")) // rounded to 1.00
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("allowed", is(true))
      .body("amount", is("1.00"))
      .body("remainingAmount", is("0.24")); // 1.24 - 1.00

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  private void failedActionCheckReturnsInitialRequestedAmount(ResourceClient client) {
    accountToPost.setRemaining(0.99);
    accountsClient.update(accountToPost.getId(), accountToPost);

    String requestedAmount = "1.004123456789"; // rounded to 1.00 when compared to account balance

    client.attemptCreate(new CheckActionRequest().withAmount(requestedAmount))
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body("allowed", is(false))
      .body("amount", is(requestedAmount))
      .body("errorMessage", is("Requested amount exceeds remaining amount"));

    removeAllFromTable(ACCOUNTS_TABLE);
  }

  private Account postAccount() {
    Account accountToPost = createAccount(ACCOUNT_BALANCE);
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
    return accountToPost;
  }
}
