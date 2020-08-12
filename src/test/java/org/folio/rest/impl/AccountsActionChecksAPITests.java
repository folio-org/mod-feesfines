package org.folio.rest.impl;

import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.utils.ResourceClients.accountsPayCheckClient;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.AccountsCheckRequest;
import org.folio.rest.utils.ResourceClient;
import org.junit.Test;

public class AccountsActionChecksAPITests extends AccountsAPITest {

  @Test
  public void payCheckAmountShouldBeAllowed() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsPayCheckClient(accountToPost.getId());
    AccountsCheckRequest accountCheckRequest = new AccountsCheckRequest();
    accountCheckRequest.withAmount(3.0);

    accountsPayCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("allowed", is(true))
      .body("amount", is(new Float(accountCheckRequest.getAmount())))
      .body("remainingAmount", is(new Float(accountToPost.getRemaining())));
  }

  @Test
  public void payCheckAmountShouldNotBeAllowedWithExceededAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsPayCheckClient(accountToPost.getId());
    AccountsCheckRequest accountCheckRequest = new AccountsCheckRequest();
    accountCheckRequest.withAmount(10.0);
    String expectedErrorMessage = "Payment amount exceeds the selected amount";

    accountsPayCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(containsString(expectedErrorMessage))
      .body("allowed", is(false))
      .body("amount", is(new Float(accountCheckRequest.getAmount())));
  }

  @Test
  public void payCheckAmountShouldNotBeAllowedWithNegativeAmount() {
    Account accountToPost = postAccount();
    ResourceClient accountsPayCheckClient = accountsPayCheckClient(accountToPost.getId());
    AccountsCheckRequest accountCheckRequest = new AccountsCheckRequest();
    accountCheckRequest.withAmount(-5.0);
    String expectedErrorMessage = "Invalid amount entered";

    accountsPayCheckClient.attemptCreate(accountCheckRequest)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(containsString(expectedErrorMessage))
      .body("allowed", is(false))
      .body("amount", is(new Float(accountCheckRequest.getAmount())));
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
