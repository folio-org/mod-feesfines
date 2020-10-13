package org.folio.test.support;

import static org.folio.test.support.ApiTests.randomId;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Manualblock;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;

import java.util.Date;

public class EntityBuilder {

  private EntityBuilder() {}

  public static Account buildAccount() {
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

  public static Account buildAccount(String accountId) {
    return buildAccount().withId(accountId);
  }

  public static Account buildAccount(double amount, double remaining) {
    return buildAccount()
      .withAmount(amount)
      .withRemaining(remaining);
  }

  public static Manualblock buildManualBlock() {
    return new Manualblock()
      .withId(randomId())
      .withUserId(randomId())
      .withDesc("Description")
      .withPatronMessage("Patron message")
      .withStaffInformation("Staff information")
      .withRequests(true)
      .withRenewals(true)
      .withBorrowing(true)
      .withExpirationDate(new Date());
  }
}
