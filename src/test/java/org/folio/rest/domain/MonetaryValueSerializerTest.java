package org.folio.rest.domain;

import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.OUTSTANDING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.test.support.ApiTests;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class MonetaryValueSerializerTest extends ApiTests {

  private static final String ACCOUNT_ID = randomId();

  @After
  public void afterEach() {
    deleteEntity("/accounts", ACCOUNT_ID);
  }

  @Test
  @Parameters({"0", "0.0", "0.00", "0.000"})
  public void monetaryValueShouldBeZero(String amount) {
    Account accountToPost = buildAccount(amount);

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
  }

  @Test
  @Parameters({ "1", "0.006", "0.0051", "0.0050000000000001" })
  public void monetaryValueShouldNotBeZero(String amount) {
    Account accountToPost = buildAccount(amount);

    // create an account
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("amount", not(0.0f));
  }

  @Test
  @Parameters({ "1", "0.1", "0.01", "0.006", "0.0051", "0.0050000000000001" })
  public void monetaryValueIsPositive(String amount) {
    Account accountToPost = buildAccount(amount);

    // create an account
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("amount", greaterThan(0.0f));
  }

  @Test
  @Parameters({ "-1", "-0.00000001" })
  public void monetaryValueIsNotPositive(String amount) {
    Account accountToPost = buildAccount(amount);

    // create an account
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("amount", lessThanOrEqualTo(0.0f));
  }

  @Test
  @Parameters({ "-1", "-0.1", "-0.01", "-0.006", "-0.0051", "-0.0050000000000001" })
  public void monetaryValueIsNegative(String amount) {
    Account accountToPost = buildAccount(amount);

    // create an account
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("amount", lessThan(0.0f));
  }

  @Test
  @Parameters({ "1", "0", "0.00", "0.000", "0.005", "0.000000000001" })
  public void monetaryValueIsNotNegative(String amount) {
    Account accountToPost = buildAccount(amount);

    // create an account
    accountsClient.create(accountToPost)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON);

    accountsClient.getById(ACCOUNT_ID)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body("amount", greaterThanOrEqualTo(0.0f));
  }

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
      .withPaymentStatus(new PaymentStatus().withName(OUTSTANDING))
      .withStatus(new Status().withName("Open"));
  }
}
