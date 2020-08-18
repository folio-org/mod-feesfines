package org.folio.rest.impl;

import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.utils.ResourceClients.accountsCheckPayClient;
import static org.folio.rest.utils.ResourceClients.accountsCheckWaiveClient;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CheckActionRequest;
import org.folio.rest.utils.ResourceClient;
import org.junit.Test;

public class AccountsActionChecksAPITests extends AccountsAPITest {

  @Test
  public void checkPayAmountShouldBeAllowed() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckPayClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldBeAllowed(accountsCheckPayClient, accountToPost);
  }

  @Test
  public void checkWaiveAmountShouldBeAllowed() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckWaiveClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldBeAllowed(accountsCheckWaiveClient, accountToPost);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithExceededAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckPayClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithExceededAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckWaiveClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(accountsCheckWaiveClient);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithNegativeAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckPayClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithNegativeAmount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithNegativeAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckWaiveClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithNegativeAmount(accountsCheckWaiveClient);
  }

  @Test
  public void checkPayAmountShouldNotBeAllowedWithZeroAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckPayClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithZeroAmount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeAllowedWithZeroAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckWaiveClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithZeroAmount(accountsCheckWaiveClient);
  }

  @Test
  public void checkPayAmountShouldNotBeNumber() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckPayClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldBeNumber(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotBeNumber() {
    Account accountToPost = postAccount();
    ResourceClient accountsCheckWaiveClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldBeNumber(accountsCheckWaiveClient);
  }

  @Test
  public void checkPayAmountShouldNotFailForNonExistentAccount() {
    ResourceClient accountsCheckPayClient = accountsCheckPayClient(UUID.randomUUID().toString());
    actionCheckAmountShouldNotFailForNonExistentAccount(accountsCheckPayClient);
  }

  @Test
  public void checkWaiveAmountShouldNotFailForNonExistentAccount() {
    ResourceClient accountsCheckWaiveClient =
      accountsCheckWaiveClient(UUID.randomUUID().toString());
    actionCheckAmountShouldNotFailForNonExistentAccount(accountsCheckWaiveClient);
  }

  private void actionCheckAmountShouldBeAllowed(
    ResourceClient actionCheckClient, Account accountToPost) {

    CheckActionRequest accountCheckRequest = new CheckActionRequest();
    accountCheckRequest.withAmount("3.0");

    actionCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("allowed", is(true))
      .body("amount", is(accountCheckRequest.getAmount()))
      .body("remainingAmount", is((float) (accountToPost.getRemaining() -
        Double.parseDouble(accountCheckRequest.getAmount()))));
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

  private Account postAccount() {
    Account accountToPost = createAccount();
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);
    return accountToPost;
  }
}
