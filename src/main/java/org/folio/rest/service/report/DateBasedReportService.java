package org.folio.rest.service.report;

import static io.vertx.core.Future.succeededFuture;
import static org.joda.time.DateTimeZone.UTC;

import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.folio.rest.client.ConfigurationClient;
import org.folio.rest.domain.LocaleSettings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import io.vertx.core.Context;
import io.vertx.core.Future;

public abstract class DateBasedReportService<T> {
  private static final LocaleSettings FALLBACK_LOCALE_SETTINGS =
    new LocaleSettings(Locale.US.toLanguageTag(), UTC.getID(),
      Currency.getInstance(Locale.US).getCurrencyCode());

  private final ConfigurationClient configurationClient;

  DateTimeZone timeZone;
  DateTimeFormatter dateTimeFormatter;
  Currency currency;

  String startDateAdjusted;
  String endDateAdjusted;

  public DateBasedReportService(Map<String, String> headers, Context context) {
    configurationClient = new ConfigurationClient(context.owner(), headers);
  }

  void setUpLocale(LocaleSettings localeSettings) {
    timeZone = localeSettings.getDateTimeZone();
    dateTimeFormatter = DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("SS",
      Locale.forLanguageTag(localeSettings.getLocale())));
    currency = Currency.getInstance(localeSettings.getCurrency());
  }

  public abstract Future<T> build();

  public Future<Void> adjustDates(DateTime startDate, DateTime endDate) {

    return configurationClient.getLocaleSettings()
      .recover(throwable -> succeededFuture(FALLBACK_LOCALE_SETTINGS))
      .onSuccess(localeSettings -> adjustDates(startDate, endDate, localeSettings))
      .mapEmpty();
  }

  private void adjustDates(DateTime startDate, DateTime endDate, LocaleSettings localeSettings) {
    setUpLocale(localeSettings);

    if (startDate != null) {
      startDateAdjusted = startDate
        .withTimeAtStartOfDay()
        .withZoneRetainFields(timeZone)
        .withZone(UTC)
        .toString(ISODateTimeFormat.dateTime());
    }

    if (endDate != null) {
      endDateAdjusted = endDate
        .withTimeAtStartOfDay()
        .plusDays(1)
        .withZoneRetainFields(timeZone)
        .withZone(UTC)
        .toString(ISODateTimeFormat.dateTime());
    }
  }

  String formatDate(Date date) {
    return new DateTime(date).withZone(timeZone).toString(dateTimeFormatter);
  }
}
