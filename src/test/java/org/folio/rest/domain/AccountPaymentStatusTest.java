package org.folio.rest.domain;

import static org.folio.rest.jaxrs.model.PaymentStatus.Name.fromValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class AccountPaymentStatusTest {

  @Test
  @Parameters({"Outstanding", "Paid partially", "Paid fully", "Waived partially",
               "Waived fully", "Transferred partially", "Transferred fully", "Refunded partially",
               "Refunded fully", "Credited fully", "Credited partially", "Cancelled item returned",
               "Cancelled as error"
  })
  public void paymentStatusIsValid(String status) {
    Account account = buildAccount(status);
    assertNotNull(account);
    assertEquals(status, account.getPaymentStatus().getName().value());
  }

  @Test(expected = IllegalArgumentException.class)
  public void paymentStatusIsNotValid() {
    buildAccount("Not paid");
  }

  private Account buildAccount(String status) {
    return new Account()
      .withId(UUID.randomUUID().toString())
      .withOwnerId(UUID.randomUUID().toString())
      .withUserId(UUID.randomUUID().toString())
      .withFeeFineId(UUID.randomUUID().toString())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(new MonetaryValue("15.0"))
      .withRemaining(new MonetaryValue("0.0"))
      .withPaymentStatus(new PaymentStatus().withName(fromValue(status)))
      .withStatus(new Status().withName("Open"));
  }
}
