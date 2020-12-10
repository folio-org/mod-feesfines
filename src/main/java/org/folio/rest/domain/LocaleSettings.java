package org.folio.rest.domain;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LocaleSettings {
  private static final DateTimeZone DEFAULT_DATE_TIME_ZONE = DateTimeZone.UTC;

  final private String locale;
  final private String timezone;
  final private String currency;

  public DateTimeZone getDateTimeZone() {
    return StringUtils.isBlank(timezone)
      ? DEFAULT_DATE_TIME_ZONE
      : DateTimeZone.forID(timezone);
  }
}
