package org.folio.rest.domain;

import static org.folio.rest.jaxrs.model.PaymentStatus.Name.fromValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.test.support.EntityBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AccountPaymentStatusTest {

  @ParameterizedTest
  @ValueSource(strings = {"Outstanding", "Paid partially", "Paid fully", "Waived partially",
    "Waived fully", "Transferred partially", "Transferred fully", "Refunded partially",
    "Refunded fully", "Credited fully", "Credited partially", "Cancelled item returned",
    "Cancelled item renewed", "Cancelled item declared lost", "Cancelled as error",
    "Suspended claim returned"
  })

  void paymentStatusIsValid(String status) {
    PaymentStatus paymentStatus = new PaymentStatus().withName(fromValue(status));
    assertNotNull(paymentStatus);
    assertEquals(status, paymentStatus.getName().value());
  }

  @Test
  void paymentStatusIsNotValid() {
    assertThrows(IllegalArgumentException.class, () -> fromValue("Invalid status"));
  }
}
