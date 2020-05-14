package org.folio.rest.domain;

import java.math.BigDecimal;

public final class FeeFineAmount {
  private final BigDecimal amount;

  public FeeFineAmount(Double amount) {
    this.amount = amount != null
      ? BigDecimal.valueOf(amount)
      : BigDecimal.ZERO;
  }

  public final boolean hasZeroAmount() {
    return getScaledAmount().compareTo(BigDecimal.ZERO) == 0;
  }

  private BigDecimal getScaledAmount() {
    return amount.setScale(2, BigDecimal.ROUND_HALF_UP);
  }
}
