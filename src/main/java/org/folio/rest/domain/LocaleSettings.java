package org.folio.rest.domain;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class LocaleSettings {
  private static final DateTimeZone DEFAULT_DATE_TIME_ZONE = DateTimeZone.UTC;

  private final String locale;
  private final String timezone;
  @NonNull
  private final String currency;
  private final String numberingSystem;

  public LocaleSettings(String locale, String timezone, String currency) {
    this(locale, timezone, currency, null);
  }

  public DateTimeZone getDateTimeZone() {
    return StringUtils.isBlank(timezone)
      ? DEFAULT_DATE_TIME_ZONE
      : DateTimeZone.forID(timezone);
  }
}
