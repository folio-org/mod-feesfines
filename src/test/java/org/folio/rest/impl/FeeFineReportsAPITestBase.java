package org.folio.rest.impl;

import static java.lang.String.format;
import static org.folio.test.support.EntityBuilder.buildLocaleSettingsConfigurations;
import static org.folio.test.support.matcher.constant.DbTable.ACCOUNTS_TABLE;
import static org.folio.test.support.matcher.constant.DbTable.FEEFINES_TABLE;
import static org.folio.test.support.matcher.constant.DbTable.FEE_FINE_ACTIONS_TABLE;
import static org.folio.test.support.matcher.constant.ServicePath.ACCOUNTS_PATH;

import java.util.Date;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.KvConfigurations;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
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
  static final String WAIVED_PARTIALLY = "Waived partially";
  static final String WAIVED_FULLY = "Waived fully";
  static final String TRANSFERRED_PARTIALLY = "Transferred partially";
  static final String TRANSFERRED_FULLY = "Transferred fully";
  static final String REFUNDED_PARTIALLY = "Refunded partially";
  static final String REFUNDED_FULLY = "Refunded fully";

  static final String DATE_TIME_JSON_FORMAT = "yyyy-MM-dd HH:mm:ss";
  static final String LOAN_DATE_TIME_JSON_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  static final String LOAN_RETURN_DATE_TIME_JSON_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
  static final DateTimeFormatter DATE_TIME_JSON_FORMATTER = DateTimeFormat.forPattern(
    DATE_TIME_JSON_FORMAT);
  static final DateTimeFormatter DATE_TIME_REPORT_FORMATTER = DateTimeFormat.forPattern(
    "M/d/yy, h:mm a");
  static final DateTimeFormatter LOAN_DATE_TIME_REPORT_FORMATTER = DateTimeFormat.forPattern(
    LOAN_DATE_TIME_JSON_FORMAT);
  static final DateTimeFormatter LOAN_RETURN_DATE_TIME_REPORT_FORMATTER = DateTimeFormat.forPattern(
    LOAN_RETURN_DATE_TIME_JSON_FORMAT);
  static final DateTimeZone TENANT_TZ = DateTimeZone.forID("America/New_York");

  private StubMapping localeSettingsStubMapping;

  void clearDatabase() {
    removeAllFromTable(FEEFINES_TABLE);
    removeAllFromTable(ACCOUNTS_TABLE);
    removeAllFromTable(FEE_FINE_ACTIONS_TABLE);
  }

  void createLocaleSettingsStub() {
    final KvConfigurations localeSettingsConfigurations = buildLocaleSettingsConfigurations();
    localeSettingsStubMapping = createStubForPath(ServicePath.CONFIGURATION_ENTRIES,
      localeSettingsConfigurations, ".*");
  }

  void removeLocaleSettingsStub() {
    removeStub(localeSettingsStubMapping);
  }

  Account charge(String userID, double amount, String feeFineType,
    String itemId, String ownerId) {

    return charge(userID, amount, feeFineType, itemId, ownerId, "owner");
  }

  Account charge(String userID, double amount, String feeFineType,
    String itemId, String ownerId, String owner) {

    final var account = EntityBuilder.buildAccount(userID, itemId, feeFineType, amount,
      ownerId, owner);
    createEntity(ACCOUNTS_PATH, account);

    return accountsClient.getById(account.getId()).as(Account.class);
  }

  Account charge(String userID, Double amount, String feeFineType,
    String itemId, Loan loan, String ownerId, String owner, String chargeActionDate,
    String chargeActionCreatedAt, String chargeActionSource) {

    Account account = EntityBuilder.buildAccount(userID, itemId, feeFineType, amount,
      ownerId, owner);

    if (loan != null) {
      account = account.withLoanId(loan.getId())
        .withDueDate(loan.getDueDate())
        .withReturnedDate(
          DateTime.parse(loan.getReturnDate(), LOAN_RETURN_DATE_TIME_REPORT_FORMATTER)
            .withZoneRetainFields(DateTimeZone.UTC)
            .toDate());
    } else {
      account = account.withLoanId(null)
        .withDueDate(null)
        .withReturnedDate(null);
    }

    createEntity(ACCOUNTS_PATH, account);

    createAction(userID, 1, account, chargeActionDate, feeFineType, null, amount, amount,
      "", "", "", chargeActionCreatedAt, chargeActionSource);

    return accountsClient.getById(account.getId()).as(Account.class);
  }

  Feefineaction createAction(String userId, int actionCounter, Account account, String dateTime,
    String type, String method, double amount, double balance, String staffInfo,
    String patronInfo, String txInfo) {

    return createAction(userId, actionCounter, account, dateTime, type, method, amount, balance,
      staffInfo, patronInfo, txInfo, null, null);
  }

  Feefineaction createAction(String userId, int actionCounter, Account account, String dateTime,
    String type, String method, double amount, double balance, String staffInfo,
    String patronInfo, String txInfo, String createdAt, String source) {

    Feefineaction action = EntityBuilder.buildFeeFineAction(userId, account.getId(),
      type, method, new MonetaryValue(amount), new MonetaryValue(balance),
      parseDateTimeUTC(dateTime),
      addSuffix(staffInfo, actionCounter), addSuffix(patronInfo, actionCounter), txInfo, createdAt,
      source);

    createActionEntity(action);

    return action;
  }

  void createActionEntity(Feefineaction action) {
    createEntity(ServicePath.ACTIONS_PATH, action);
  }

  String formatRefundReportDate(Date date, DateTimeZone timeZone) {
    return new DateTime(date).withZone(timeZone).toString(DATE_TIME_REPORT_FORMATTER);
  }

  void createOverdueFinePolicy(OverdueFinePolicy overdueFinePolicy) {
    createEntity(ServicePath.OVERDUE_FINE_POLICIES_PATH, overdueFinePolicy);
  }

  void deleteOverdueFinePolicy(OverdueFinePolicy overdueFinePolicy) {
    deleteEntity(ServicePath.OVERDUE_FINE_POLICIES_PATH, overdueFinePolicy.getId());
  }

  void createLostItemFeePolicy(LostItemFeePolicy lostItemFeePolicy) {
    createEntity(ServicePath.LOST_ITEM_FEE_POLICIES_PATH, lostItemFeePolicy);
  }

  void deleteLostItemFeePolicy(LostItemFeePolicy lostItemFeePolicy) {
    deleteEntity(ServicePath.LOST_ITEM_FEE_POLICIES_PATH, lostItemFeePolicy.getId());
  }

  String formatReportDate(Date date) {
    return new DateTime(date).withZone(TENANT_TZ).toString(DATE_TIME_REPORT_FORMATTER);
  }

  String formatReportDate(Date date, DateTimeZone timeZone) {
    return new DateTime(date).withZone(timeZone).toString(DATE_TIME_REPORT_FORMATTER);
  }

  static Date parseDateTime(String date, DateTimeZone timeZone) {
    if (date == null) {
      return null;
    }

    return DateTime.parse(date, DateTimeFormat.forPattern(DATE_TIME_JSON_FORMAT))
      .withZoneRetainFields(timeZone)
      .toDate();
  }

  static Date parseDateTimeUTC(String date) {
    return parseDateTime(date, DateTimeZone.UTC);
  }

  static Date parseDateTimeTenantTz(String date) {
    return parseDateTime(date, TENANT_TZ);
  }

  String addSuffix(String info, int counter) {
    return format("%s %d", info, counter);
  }

  static String withTenantTz(String date) {
    return withTenantTz(date, DATE_TIME_JSON_FORMATTER);
  }

  static String withTenantTz(String date, DateTimeFormatter formatter) {
    return DateTime.parse(date, DateTimeFormat.forPattern(DATE_TIME_JSON_FORMAT))
      .withZoneRetainFields(TENANT_TZ)
      .withZone(DateTimeZone.UTC)
      .toString(formatter);
  }
}
