package org.folio.rest.impl;

import static java.lang.String.format;
import static org.folio.test.support.EntityBuilder.createLocaleSettingsConfigurations;
import static org.folio.test.support.matcher.constant.DbTable.ACCOUNTS_TABLE;
import static org.folio.test.support.matcher.constant.DbTable.FEEFINES_TABLE;
import static org.folio.test.support.matcher.constant.DbTable.FEE_FINE_ACTIONS_TABLE;
import static org.folio.test.support.matcher.constant.ServicePath.ACCOUNTS_PATH;

import java.util.Date;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.KvConfigurations;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.folio.test.support.matcher.constant.ServicePath;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

public class FeeFineReportsAPITestBase extends ApiTests {
  static final String PAID_PARTIALLY = "Paid partially";
  static final String PAID_FULLY = "Paid fully";
  static final String TRANSFERRED_PARTIALLY = "Transferred partially";
  static final String TRANSFERRED_FULLY = "Transferred fully";
  static final String REFUNDED_PARTIALLY = "Refunded partially";
  static final String REFUNDED_FULLY = "Refunded fully";

  static final String DATE_TIME_JSON_FORMAT = "yyyy-MM-dd HH:mm:ss";
  static final DateTimeFormatter DATE_TIME_JSON_FORMATTER = DateTimeFormat.forPattern(DATE_TIME_JSON_FORMAT);
  static final DateTimeFormatter DATE_TIME_REPORT_FORMATTER = DateTimeFormat.forPattern("M/d/yy, h:mm a");
  static final DateTimeZone TENANT_TZ = DateTimeZone.forID("America/New_York");

  private StubMapping localeSettingsStubMapping;

  void clearDatabase() {
    removeAllFromTable(FEEFINES_TABLE);
    removeAllFromTable(ACCOUNTS_TABLE);
    removeAllFromTable(FEE_FINE_ACTIONS_TABLE);
  }

  void createLocaleSettingsStub() {
    final KvConfigurations localeSettingsConfigurations = createLocaleSettingsConfigurations();
    localeSettingsStubMapping = createStubForPath(ServicePath.CONFIGURATION_ENTRIES,
      localeSettingsConfigurations, ".*");
  }

  void removeLocaleSettingsStub() {
    removeStub(localeSettingsStubMapping);
  }

  Account charge(String userID, Double amount, String feeFineType,
    String itemId, String ownerId) {

    return charge(userID, amount, feeFineType, itemId, ownerId, "owner");
  }

  Account charge(String userID, Double amount, String feeFineType,
    String itemId, String ownerId, String owner) {

    final var account = EntityBuilder.buildAccount(userID, itemId, feeFineType, amount,
      ownerId, owner);
    createEntity(ACCOUNTS_PATH, account);

    return accountsClient.getById(account.getId()).as(Account.class);
  }

  Feefineaction createAction(String userId, int actionCounter, Account account, String dateTime,
    String type, String method, Double amount, Double balance, String staffInfo,
    String patronInfo, String txInfo) {

    return createAction(userId, actionCounter, account, dateTime, type, method, amount, balance,
      staffInfo, patronInfo, txInfo, null, null);
  }

  Feefineaction createAction(String userId, int actionCounter, Account account, String dateTime,
    String type, String method, Double amount, Double balance, String staffInfo,
    String patronInfo, String txInfo, String createdAt, String source) {

    Feefineaction action = EntityBuilder.buildFeeFineAction(userId, account.getId(),
      type, method, amount, balance, parseDateTime(dateTime),
      addSuffix(staffInfo, actionCounter), addSuffix(patronInfo, actionCounter), txInfo, createdAt,
      source);

    createActionEntity(action);

    return action;
  }

  void createActionEntity(Feefineaction action) {
    createEntity(ServicePath.ACTIONS_PATH, action);
  }

  String formatMonetaryValue(Double value) {
    return new MonetaryValue(value).toString();
  }

  String formatRefundReportDate(Date date, DateTimeZone timeZone) {
    return new DateTime(date).withZone(timeZone).toString(DATE_TIME_REPORT_FORMATTER);
  }

  Date parseDateTime(String date) {
    if (date == null) {
      return null;
    }

    return DateTime.parse(date, DateTimeFormat.forPattern(DATE_TIME_JSON_FORMAT))
      .withZoneRetainFields(DateTimeZone.UTC)
      .toDate();
  }

  String addSuffix(String info, int counter) {
    return format("%s %d", info, counter);
  }

  String withTenantTz(String date) {
    return DateTime.parse(date, DateTimeFormat.forPattern(DATE_TIME_JSON_FORMAT))
      .withZoneRetainFields(TENANT_TZ)
      .withZone(DateTimeZone.UTC)
      .toString(DATE_TIME_JSON_FORMATTER);
  }
}
