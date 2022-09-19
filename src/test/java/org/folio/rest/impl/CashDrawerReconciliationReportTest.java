package org.folio.rest.impl;

import static org.folio.HttpStatus.HTTP_OK;
import static org.folio.HttpStatus.HTTP_UNPROCESSABLE_ENTITY;
import static org.folio.rest.utils.ResourceClients.buildCashDrawerReconciliationReportClient;
import static org.folio.rest.utils.ResourceClients.buildCashDrawerReconciliationReportSourcesClient;
import static org.folio.test.support.EntityBuilder.buildCashDrawerReconciliationReportEntry;
import static org.folio.test.support.EntityBuilder.buildReportTotalsEntry;
import static org.folio.test.support.matcher.ReportMatcher.cashDrawerReconciliationReportMatcher;
import static org.folio.test.support.matcher.ReportMatcher.cashDrawerReconciliationReportSourcesMatcher;
import static org.folio.test.support.matcher.constant.ServicePath.ACCOUNTS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.USERS_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.checkerframework.checker.units.qual.A;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReport;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportEntry;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportSources;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportStats;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.ReportTotalsEntry;
import org.folio.rest.utils.ReportResourceClient;
import org.folio.test.support.EntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

public class CashDrawerReconciliationReportTest extends FeeFineReportsAPITestBase {
  private static final String START_DATE = "2020-01-01";
  private static final String END_DATE = "2020-01-15";
  private static final String OWNER_ID_1 = randomId();
  private static final String OWNER_ID_2 = randomId();
  private static final String USER_ID_1 = randomId();
  private static final String USER_ID_2 = randomId();
  private static final String CREATED_AT = randomId();
  private static final String SOURCE_1 = "Source 1";
  private static final String SOURCE_1_ID = randomId();
  private static final String SOURCE_2 = "Source 2";
  private static final String SOURCE_2_ID = randomId();
  private static final String OWNER_1 = "Owner 1";
  private static final String OWNER_2 = "Owner 2";
  private static final String FEE_FINE_TYPE_1 = "Fee/fine type 1";
  private static final String FEE_FINE_TYPE_2 = "Fee/fine type 2";
  private static final String PAYMENT_METHOD_1 = "Payment method 1";
  private static final String PAYMENT_METHOD_2 = "Payment method 2";
  private static final String PAYMENT_TX_INFO = "Payment transaction information";
  private static final String PAYMENT_STAFF_INFO = "Payment - info for staff";
  private static final String PAYMENT_PATRON_INFO = "Payment - info for patron";
  private static final String SOURCE_TOTALS = "Source totals";
  private static final String PAYMENT_METHOD_TOTALS = "Payment method totals";
  private static final String FEE_FINE_TYPE_TOTALS = "Fee/fine type totals";
  private static final String FEE_FINE_OWNER_TOTALS = "Fee/fine owner totals";
  private static final String ZERO_MONETARY = "0.00";
  private static final String ZERO_COUNT = "0";

  private final ReportResourceClient reportClient = buildCashDrawerReconciliationReportClient();
  private final ReportResourceClient reportSourcesClient = buildCashDrawerReconciliationReportSourcesClient();

  @BeforeEach
  public void setUp() {
    clearDatabase();
    createLocaleSettingsStub();

    createStub(USERS_PATH, EntityBuilder.buildUser().withId(SOURCE_1_ID), SOURCE_1_ID);
    createStub(USERS_PATH, EntityBuilder.buildUser().withId(SOURCE_2_ID), SOURCE_2_ID);
  }

  @Test
  public void okResponseWhenLocaleConfigDoesNotExist() {
    removeLocaleSettingsStub();

    requestAndCheck(emptyReport());
  }

  @Test
  public void okResponseWhenLocaleConfigExists() {
    requestAndCheck(emptyReport());
  }

  @Test
  public void emptyReportWhenPaidBeforeStartDateAndAfterEndDate() {

    double amount = 10.0;

    double amount1 = 3.0;
    double balane = 7.0;

    Account account = charge(USER_ID_1, amount, FEE_FINE_TYPE_1, null, OWNER_ID_1);

    createAction(USER_ID_1, 1, account, "2019-12-31 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD_1,
      amount1, balane, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO, CREATED_AT,
      SOURCE_1);

    createAction(USER_ID_1, 1, account, "2020-02-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD_1,
      amount1, balane, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO, CREATED_AT,
      SOURCE_1);

    requestAndCheck(emptyReport());
  }

  @Test
  public void shouldReturn422WhenRequestIsNotValid() {
    reportClient.getCashDrawerReconciliationReport(null, "2020-01-01", CREATED_AT, null,
      HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getCashDrawerReconciliationReport(null, null, CREATED_AT, null,
      HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getCashDrawerReconciliationReport("2020-01-01", "2020-01-02", null, null,
      HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getCashDrawerReconciliationReport("not-a-date", "2020-01-01", CREATED_AT, null,
      HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getCashDrawerReconciliationReport("2020-01-01", "not-a-date", CREATED_AT, null,
      HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getCashDrawerReconciliationReport("not-a-date", "not-a-date", CREATED_AT, null,
      HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getCashDrawerReconciliationReport("2020-01-01", "2020-01-02", "not-a-uuid", null,
      HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getCashDrawerReconciliationReport("1 Jan 2021", null, CREATED_AT, null,
      HTTP_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturn200WhenRequestIsValid() {
    reportClient.getCashDrawerReconciliationReport("2020-01-01", null, CREATED_AT, null,
      HTTP_OK);
    reportClient.getCashDrawerReconciliationReport("2020-01-01", "2020-01-02", CREATED_AT, null,
      HTTP_OK);
    reportClient.getCashDrawerReconciliationReport("2020-01-01", "2020-01-02", CREATED_AT,
      List.of(SOURCE_1_ID), HTTP_OK);
  }

  @Test
  public void returnsResultWhenAccountIsDeleted() {
    Pair<Account, Feefineaction> sourceObjects = createMinimumViableReportData();

    assert sourceObjects.getLeft() != null;
    deleteEntity(ACCOUNTS_PATH, sourceObjects.getLeft().getId());

    assert sourceObjects.getRight() != null;
    reportClient.getCashDrawerReconciliationReport("2020-01-01", "2020-01-02", CREATED_AT, null)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(1))
      .body("reportData[0].feeFineId", is(sourceObjects.getLeft().getId()));
  }

  @Test
  public void emptyResultWhenFilteredBySource() {
    createMinimumViableReportData();

    requestAndCheck(emptyReport(), START_DATE, END_DATE, CREATED_AT, List.of(SOURCE_2_ID));
  }

  @Test
  public void validReportWhenPaymentsExist() {
    double amount = 10.0;

    Account account1 = charge(USER_ID_1, amount, FEE_FINE_TYPE_1, null, OWNER_ID_1, OWNER_1);
    Account account2 = charge(USER_ID_1, amount, FEE_FINE_TYPE_2, null, OWNER_ID_1, OWNER_1);
    Account account3 = charge(USER_ID_2, amount, FEE_FINE_TYPE_2, null, OWNER_ID_2, OWNER_2);

    // This payment should not be included in the report - it's 1 second before the interval start
    createAction(USER_ID_1, 1, account1, withTenantTz("2019-12-31 23:59:59"), PAID_PARTIALLY,
      PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    Feefineaction paymentAction1 = createAction(USER_ID_1, 1, account1,
      withTenantTz("2020-01-01 00:00:01"),
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    Feefineaction paymentAction2 = createAction(USER_ID_1, 2, account1,
      withTenantTz("2020-01-03 12:00:00"),
      PAID_PARTIALLY, PAYMENT_METHOD_2, 2.0, 8.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    Feefineaction paymentAction3 = createAction(USER_ID_2, 3, account2,
      withTenantTz("2020-01-05 12:00:00"),
      PAID_PARTIALLY, PAYMENT_METHOD_1,
      1.0, 9.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_2);

    Feefineaction paymentAction4 = createAction(USER_ID_2, 4, account3,
      withTenantTz("2020-01-15 23:59:59"),
      PAID_FULLY, PAYMENT_METHOD_2, 10.0, 0.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    // This payment should not be included in the report - it's 1 second after the interval end
    createAction(USER_ID_1, 1, account1, withTenantTz("2020-01-16 00:00:01"),
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    requestAndCheck(new CashDrawerReconciliationReport()
      .withReportData(List.of(
        buildCashDrawerReconciliationReportEntry(
          SOURCE_1, PAYMENT_METHOD_1, "3.00", OWNER_1, FEE_FINE_TYPE_1,
          formatReportDate(paymentAction1.getDateAction(), TENANT_TZ), PAID_PARTIALLY,
          PAYMENT_TX_INFO, addSuffix(PAYMENT_STAFF_INFO, 1), addSuffix(PAYMENT_PATRON_INFO, 1),
          USER_ID_1, account1.getId()),
        buildCashDrawerReconciliationReportEntry(
          SOURCE_1, PAYMENT_METHOD_2, "2.00", OWNER_1, FEE_FINE_TYPE_1,
          formatReportDate(paymentAction2.getDateAction(), TENANT_TZ), PAID_PARTIALLY,
          PAYMENT_TX_INFO, addSuffix(PAYMENT_STAFF_INFO, 2), addSuffix(PAYMENT_PATRON_INFO, 2),
          USER_ID_1, account1.getId()),
        buildCashDrawerReconciliationReportEntry(
          SOURCE_2, PAYMENT_METHOD_1, "1.00", OWNER_1, FEE_FINE_TYPE_2,
          formatReportDate(paymentAction3.getDateAction(), TENANT_TZ), PAID_PARTIALLY,
          PAYMENT_TX_INFO, addSuffix(PAYMENT_STAFF_INFO, 3), addSuffix(PAYMENT_PATRON_INFO, 3),
          USER_ID_2, account2.getId()),
        buildCashDrawerReconciliationReportEntry(
          SOURCE_1, PAYMENT_METHOD_2, "10.00", OWNER_2, FEE_FINE_TYPE_2,
          formatReportDate(paymentAction4.getDateAction(), TENANT_TZ), PAID_FULLY,
          PAYMENT_TX_INFO, addSuffix(PAYMENT_STAFF_INFO, 4), addSuffix(PAYMENT_PATRON_INFO, 4),
          USER_ID_2, account3.getId())))
      .withReportStats(new CashDrawerReconciliationReportStats()
        .withBySource(List.of(
          buildReportTotalsEntry(SOURCE_1, "15.00", "3"),
          buildReportTotalsEntry(SOURCE_2, "1.00", "1"),
          buildReportTotalsEntry(SOURCE_TOTALS, "16.00", "4")))
        .withByPaymentMethod(List.of(
          buildReportTotalsEntry(PAYMENT_METHOD_1, "4.00", "2"),
          buildReportTotalsEntry(PAYMENT_METHOD_2, "12.00", "2"),
          buildReportTotalsEntry(PAYMENT_METHOD_TOTALS, "16.00", "4")))
        .withByFeeFineType(List.of(
          buildReportTotalsEntry(FEE_FINE_TYPE_1, "5.00", "2"),
          buildReportTotalsEntry(FEE_FINE_TYPE_2, "11.00", "2"),
          buildReportTotalsEntry(FEE_FINE_TYPE_TOTALS, "16.00", "4")))
        .withByFeeFineOwner(List.of(
          buildReportTotalsEntry(OWNER_1, "6.00", "3"),
          buildReportTotalsEntry(OWNER_2, "10.00", "1"),
          buildReportTotalsEntry(FEE_FINE_OWNER_TOTALS, "16.00", "4")))));
  }

  @Test
  public void validReportSourcesWhenPaymentsExist() {
    Account account1 = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_1, null, OWNER_ID_1, OWNER_1);
    Account account2 = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_2, null, OWNER_ID_1, OWNER_1);
    Account account3 = charge(USER_ID_2, 10.0, FEE_FINE_TYPE_2, null, OWNER_ID_2, OWNER_2);

    createAction(USER_ID_1, 1, account1, "2020-01-01 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    createAction(USER_ID_1, 2, account1, "2020-01-03 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD_2, 2.0, 8.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    createAction(USER_ID_2, 3, account2, "2020-01-05 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD_1, 1.0, 9.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_2);

    createAction(USER_ID_2, 4, account3, "2020-01-15 12:00:00",
      PAID_FULLY, PAYMENT_METHOD_2, 10.0, 0.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    reportSourcesClient.getCashDrawerReconciliationReportSources(CREATED_AT)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body(cashDrawerReconciliationReportSourcesMatcher(new CashDrawerReconciliationReportSources()
        .withSources(List.of(SOURCE_1, SOURCE_2))));
  }

  @Test
  public void sourceNotIncludedWhenServicePointIsDifferent() {
    Account account1 = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_1, null, OWNER_ID_1, OWNER_1);
    Account account2 = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_2, null, OWNER_ID_1, OWNER_1);
    Account account3 = charge(USER_ID_2, 10.0, FEE_FINE_TYPE_2, null, OWNER_ID_2, OWNER_2);

    createAction(USER_ID_1, 1, account1, "2020-01-01 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    createAction(USER_ID_1, 2, account1, "2020-01-03 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD_2, 2.0, 8.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    createAction(USER_ID_2, 3, account2, "2020-01-05 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD_1, 1.0, 9.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, randomId(), SOURCE_2);

    createAction(USER_ID_2, 4, account3, "2020-01-15 12:00:00",
      PAID_FULLY, PAYMENT_METHOD_2, 10.0, 0.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    reportSourcesClient.getCashDrawerReconciliationReportSources(CREATED_AT)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body(cashDrawerReconciliationReportSourcesMatcher(new CashDrawerReconciliationReportSources()
        .withSources(List.of(SOURCE_1))));
  }

  private void requestAndCheck(CashDrawerReconciliationReport report) {
    requestAndCheck(report, START_DATE, END_DATE, CREATED_AT, null);
  }

  private void requestAndCheck(CashDrawerReconciliationReport report,
    String startDate, String endDate, String createdAt, List<String> sources) {

    List<CashDrawerReconciliationReportEntry> entries = report.getReportData();

    requestReport(startDate, endDate, createdAt, sources)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body(cashDrawerReconciliationReportMatcher(report));
  }

  private Response requestReport(String startDate, String endDate, String createdAt,
    List<String> sources) {

    return reportClient.getCashDrawerReconciliationReport(startDate, endDate, createdAt, sources);
  }

  private CashDrawerReconciliationReport emptyReport() {
    return new CashDrawerReconciliationReport()
      .withReportData(List.of())
      .withReportStats(new CashDrawerReconciliationReportStats()
        .withBySource(List.of(new ReportTotalsEntry()
          .withName(SOURCE_TOTALS)
          .withTotalAmount(ZERO_MONETARY)
          .withTotalCount(ZERO_COUNT)))
        .withByPaymentMethod(List.of(new ReportTotalsEntry()
          .withName(PAYMENT_METHOD_TOTALS)
          .withTotalAmount(ZERO_MONETARY)
          .withTotalCount(ZERO_COUNT)))
        .withByFeeFineType(List.of(new ReportTotalsEntry()
          .withName(FEE_FINE_TYPE_TOTALS)
          .withTotalAmount(ZERO_MONETARY)
          .withTotalCount(ZERO_COUNT)))
        .withByFeeFineOwner(List.of(new ReportTotalsEntry()
          .withName(FEE_FINE_OWNER_TOTALS)
          .withTotalAmount(ZERO_MONETARY)
          .withTotalCount(ZERO_COUNT))));
  }

  private Pair<Account, Feefineaction> createMinimumViableReportData() {
    Account account = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_1, null, OWNER_ID_1);

    Feefineaction action = createAction(USER_ID_1, 1, account, "2020-01-01 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO,
      PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT, SOURCE_1);

    return Pair.of(account, action);
  }
}
