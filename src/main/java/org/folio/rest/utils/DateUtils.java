package org.folio.rest.utils;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

public class DateUtils {
  private DateUtils() { }

  public static DateTime parseDateReportParameter(String date) {
    if (date == null) {
      return null;
    }

    return DateTime.parse(date, ISODateTimeFormat.date());
  }
}
