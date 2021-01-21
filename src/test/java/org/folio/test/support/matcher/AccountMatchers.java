package org.folio.test.support.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Date;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.utils.ResourceClient;
import org.hamcrest.Matcher;

import io.restassured.response.Response;

public final class AccountMatchers {

  private AccountMatchers() {}

  public static Matcher<Response> isPaidFully() {
    return new TypeMappingMatcher<>(
      response -> response.getBody().asString(),
      allOf(
        hasJsonPath("status.name", is("Closed")),
        hasJsonPath("paymentStatus.name", is("Paid fully")),
        hasJsonPath("remaining", is(0.0))
      ));
  }

  public static Account verifyAccountAndGet(
    ResourceClient accountsClient,
    String accountId,
    String expectedPaymentStatus,
    float amount,
    String statusName) {

    final Response getAccountByIdResponse = accountsClient.getById(accountId);
    getAccountByIdResponse
      .then()
      .body("remaining", is(amount))
      .body("status.name", is(statusName))
      .body("paymentStatus.name", is(expectedPaymentStatus))
      .body("dateUpdated", notNullValue())
      .body("metadata.updatedDate", notNullValue());

    final Account updatedAccount = getAccountByIdResponse.as(Account.class);
    final Date dateCreated = updatedAccount.getMetadata().getCreatedDate();
    final Date dateUpdated = updatedAccount.getMetadata().getUpdatedDate();

    assertThat(dateUpdated, equalTo(updatedAccount.getDateUpdated()));
    assertThat(dateCreated, not(equalTo(dateUpdated)));

    return updatedAccount;
  }
}
