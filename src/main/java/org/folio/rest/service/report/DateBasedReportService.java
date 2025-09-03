package org.folio.rest.service.report;

import static io.vertx.core.Future.succeededFuture;
import static org.joda.time.DateTimeZone.UTC;

import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.folio.rest.client.SettingsClient;
import org.folio.rest.domain.LocaleSettings;
import org.folio.rest.service.report.parameters.DateBasedReportParameters;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import io.vertx.core.Context;
import io.vertx.core.Future;

public abstract class DateBasedReportService<T, P> {
  private static final LocaleSettings FALLBACK_LOCALE_SETTINGS =
    new LocaleSettings(Locale.US.toLanguageTag(), UTC.getID(),
      Currency.getInstance(Locale.US).getCurrencyCode());

  private final SettingsClient settingsClient;

  DateTimeZone timeZone;
  DateTimeFormatter dateTimeFormatter;
  Currency currency;

  public DateBasedReportService(Map<String, String> headers, Context context) {
    settingsClient = new SettingsClient(context.owner(), headers);
  }

  public abstract Future<T> build(P params);

  void setUpLocale(LocaleSettings localeSettings) {
    timeZone = localeSettings.getDateTimeZone();
    dateTimeFormatter = DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("SS",
      Locale.forLanguageTag(localeSettings.getLocale())));
    currency = Currency.getInstance(localeSettings.getCurrency());
  }

  public Future<Void> adjustDates(DateBasedReportParameters params) {
    return settingsClient.getLocaleSettings()
      .recover(throwable -> succeededFuture(FALLBACK_LOCALE_SETTINGS))
      .onSuccess(localeSettings -> adjustDates(params, localeSettings))
      .mapEmpty();
  }

  private void adjustDates(DateBasedReportParameters params, LocaleSettings localeSettings) {
    setUpLocale(localeSettings);

    if (params.getRawStartDate() != null) {
      params.setStartDate(params.getRawStartDate()
        .withTimeAtStartOfDay()
        .withZoneRetainFields(timeZone)
        .withZone(UTC)
        .toString(ISODateTimeFormat.dateTime()));
    }

    if (params.getRawEndDate() != null) {
      params.setEndDate(params.getRawEndDate()
        .withTimeAtStartOfDay()
        .plusDays(1)
        .withZoneRetainFields(timeZone)
        .withZone(UTC)
        .toString(ISODateTimeFormat.dateTime()));
    }
  }

  String formatDate(Date date) {
    if (date == null) {
      return "";
    }

    return new DateTime(date).withZone(timeZone).toString(dateTimeFormatter);
  }

  String reformatLoanDate(String date) {
    return DateTime.parse(date)
      .withZoneRetainFields(UTC)
      .withZone(timeZone)
      .toString(dateTimeFormatter);
  }
}
