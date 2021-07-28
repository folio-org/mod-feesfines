package org.folio.rest.domain;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.runners.Parameterized.Parameters;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.test.support.ApiTests;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MonetaryValueSerializerTest extends ApiTests {

  private static final String ACCOUNT_ID = randomId();

  @Parameters
  public static String[] amounts() {
    return new String[] { "0", "0.0", "0.00", "0.000", "0.005", "0.000000000000001" };
  }

  private final String amountForZero;

  public MonetaryValueSerializerTest(String amount) {
    this.amountForZero = amount;
  }

 /* @Parameters
  public static String[] amounts() {
    return new String[] { "0", "0.0", "0.00", "0.000", "0.005", "0.000000000000001" };
  }*/

  @After
  public void afterEach() {
    deleteEntity("/accounts", ACCOUNT_ID);
  }

  @Test
  public void monetaryValueShouldBeZero() {
    Account accountToPost = buildAccount(amountForZero);

    // create an account
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("amount", is(0.0));
  }
 /* @Test
  public void monetaryValueShouldBeZero() {
    Account accountToPost = buildAccount(amountForZero);

    // create an account
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("amount", is(0.0f));
  }*/


  private Account buildAccount(String amount) {
    return new Account()
      .withId(ACCOUNT_ID)
      .withOwnerId(randomId())
      .withUserId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(new MonetaryValue(amount))
      .withRemaining(new MonetaryValue("3.33"))
      .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
      .withStatus(new Status().withName("Open"));
  }
}
