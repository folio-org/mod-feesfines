package org.folio.test.support;

import static org.folio.test.support.ApiTests.randomId;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;

public class EntityBuilder {

  private EntityBuilder() {}

  public static Account createAccount() {
    return new Account()
      .withId(randomId())
      .withOwnerId(randomId())
      .withUserId(randomId())
      .withItemId(randomId())
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(9.00)
      .withRemaining(4.55)
      .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
      .withStatus(new Status().withName("Open"));
  }

  public static Account createAccount(double amount, double remaining) {
    return createAccount()
      .withAmount(amount)
      .withRemaining(remaining);
  }

}
