package org.folio.rest.domain;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LocaleSettings {
  private static final DateTimeZone DEFAULT_DATE_TIME_ZONE = DateTimeZone.UTC;

  private final String locale;
  private final String timezone;
  private final String currency;

  public DateTimeZone getDateTimeZone() {
    return StringUtils.isBlank(timezone)
      ? DEFAULT_DATE_TIME_ZONE
      : DateTimeZone.forID(timezone);
  }
}
