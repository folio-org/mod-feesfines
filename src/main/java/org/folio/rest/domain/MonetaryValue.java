package org.folio.rest.domain;

import static java.math.BigDecimal.ZERO;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//@JsonSerialize(using = MonetaryValue.MonetaryValueSerializer.class)
public class MonetaryValue {
  private static final Currency USD = Currency.getInstance("USD");
  private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;
  public static final MonetaryValue MONETARY_VALUE_ZERO = new MonetaryValue(BigDecimal.ZERO);

  private final BigDecimal amount;

  @JsonIgnore
  private final Currency currency;

  @JsonCreator
  public MonetaryValue(BigDecimal amount) {
    this(amount, USD);
  }

  public MonetaryValue(String amount) {
    this(from(amount), USD);
  }

  public MonetaryValue(Double amount) {
    this(from(amount), USD);
  }

  public MonetaryValue(Double amount, Currency currency) {
    this(from(amount), currency);
  }

  public MonetaryValue(BigDecimal amount, Currency currency) {
    this(amount, currency, DEFAULT_ROUNDING);
  }

  public MonetaryValue(BigDecimal amount, Currency currency, RoundingMode rounding) {
    requireNonNull(amount);
    requireNonNull(currency);
    requireNonNull(rounding);
    this.currency = currency;
    this.amount = amount.setScale(currency.getDefaultFractionDigits(), rounding);
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Currency getCurrency() {
    return currency;
  }

  public boolean isZero() {
    return ZERO.compareTo(amount) == 0;
  }

  public boolean isPositive() {
    return ZERO.compareTo(amount) < 0;
  }

  public boolean isNegative() {
    return ZERO.compareTo(amount) > 0;
  }

  public boolean isGreaterThan(MonetaryValue other) {
    return amount.compareTo(other.getAmount()) > 0;
  }

  public boolean isGreaterThanOrEquals(MonetaryValue other) {
    return amount.compareTo(other.getAmount()) >= 0;
  }

  public MonetaryValue subtract(MonetaryValue other) {
    return new MonetaryValue(amount.subtract(other.getAmount()));
  }

  public MonetaryValue divide(MonetaryValue denominator) {
    return new MonetaryValue(amount.divide(denominator.getAmount(), DEFAULT_ROUNDING));
  }

  public MonetaryValue add(MonetaryValue other) {
    return new MonetaryValue(amount.add(other.getAmount()));
  }

  public MonetaryValue min(MonetaryValue other) {
    return amount.compareTo(other.getAmount()) <= 0 ? this : other;
  }

  private static BigDecimal from(String value) {
    return value == null ? null : new BigDecimal(value);
  }

  private static BigDecimal from(Double value) {
    return value == null ? null : BigDecimal.valueOf(value);
  }

  public double toDouble() {
    return amount.doubleValue();
  }

  @Override
  public String toString() {
    return amount.toString();
  }

  static class MonetaryValueSerializer extends JsonSerializer<MonetaryValue> {

    @Override
    public void serialize(MonetaryValue value, JsonGenerator gen, SerializerProvider provider)
      throws
      IOException {
      gen.writeNumber(value.getAmount().doubleValue());
    }
  }
}
