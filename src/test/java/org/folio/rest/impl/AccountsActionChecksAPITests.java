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
  public void payCheckAmountShouldBeAllowed() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldBeAllowed(accountsPayCheckClient, accountToPost);
  }

  @Test
  public void waiveCheckAmountShouldBeAllowed() {
    Account accountToPost = postAccount();
    ResourceClient accountsWaiveCheckClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldBeAllowed(accountsWaiveCheckClient, accountToPost);
  }

  @Test
  public void payCheckAmountShouldNotBeAllowedWithExceededAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(accountsPayCheckClient);
  }

  @Test
  public void waiveCheckAmountShouldNotBeAllowedWithExceededAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithExceededAmount(accountsPayCheckClient);
  }

  @Test
  public void payCheckAmountShouldNotBeAllowedWithNegativeAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithNegativeAmount(accountsPayCheckClient);
  }

  @Test
  public void waiveCheckAmountShouldNotBeAllowedWithNegativeAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithNegativeAmount(accountsPayCheckClient);
  }

  @Test
  public void payCheckAmountShouldNotBeAllowedWithZeroAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithZeroAmount(accountsPayCheckClient);
  }

  @Test
  public void waiveCheckAmountShouldNotBeAllowedWithZeroAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldNotBeAllowedWithZeroAmount(accountsPayCheckClient);
  }

  @Test
  public void payCheckAmountShouldNotBeNumber() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsCheckPayClient(accountToPost.getId());
    actionCheckAmountShouldBeNumber(accountsPayCheckClient);
  }

  @Test
  public void waiveCheckAmountShouldNotBeNumber() {
    Account accountToPost = postAccount();
    ResourceClient accountsWaiveCheckClient = accountsCheckWaiveClient(accountToPost.getId());
    actionCheckAmountShouldBeNumber(accountsWaiveCheckClient);
  }

  @Test
  public void payCheckAmountShouldNotFailForNonExistentAccount() {
    ResourceClient accountsPayCheckClient = accountsCheckPayClient(UUID.randomUUID().toString());
    actionCheckAmountShouldNotFailForNonExistentAccount(accountsPayCheckClient);
  }

  @Test
  public void waiveCheckAmountShouldNotFailForNonExistentAccount() {
    ResourceClient accountsWaiveCheckClient =
      accountsCheckWaiveClient(UUID.randomUUID().toString());
    actionCheckAmountShouldNotFailForNonExistentAccount(accountsWaiveCheckClient);
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
