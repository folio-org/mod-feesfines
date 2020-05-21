package org.folio.rest.domain;

import static java.math.BigDecimal.ROUND_HALF_UP;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;

import java.math.BigDecimal;

public final class FeeFineAmount {
  private final BigDecimal amount;

  public FeeFineAmount(Double amount) {
    this.amount = amount != null ? valueOf(amount) : ZERO;
  }

  public final boolean hasZeroAmount() {
    return getScaledAmount().compareTo(ZERO) == 0;
  }

  private BigDecimal getScaledAmount() {
    return amount.setScale(2, ROUND_HALF_UP);
  }
}
