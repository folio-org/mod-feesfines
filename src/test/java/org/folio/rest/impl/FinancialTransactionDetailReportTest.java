package org.folio.rest.impl;

import static java.lang.String.format;
import static org.folio.HttpStatus.HTTP_OK;
import static org.folio.HttpStatus.HTTP_UNPROCESSABLE_ENTITY;
import static org.folio.rest.utils.ResourceClients.buildFinancialTransactionsDetailReportClient;
import static org.folio.test.support.EntityBuilder.buildFinancialTransactionsDetailReportEntry;
import static org.folio.test.support.EntityBuilder.buildHoldingsRecord;
import static org.folio.test.support.EntityBuilder.buildInstance;
import static org.folio.test.support.EntityBuilder.buildItem;
import static org.folio.test.support.EntityBuilder.buildLoan;
import static org.folio.test.support.EntityBuilder.buildLoanPolicy;
import static org.folio.test.support.EntityBuilder.buildLocation;
import static org.folio.test.support.EntityBuilder.buildLostItemFeePolicy;
import static org.folio.test.support.EntityBuilder.buildOverdueFinePolicy;
import static org.folio.test.support.EntityBuilder.buildReportTotalsEntry;
import static org.folio.test.support.EntityBuilder.buildServicePoint;
import static org.folio.test.support.matcher.ReportMatcher.financialTransactionsDetailReportMatcher;
import static org.folio.test.support.matcher.constant.ServicePath.ACCOUNTS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.HOLDINGS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.INSTANCES_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.USERS_GROUPS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.USERS_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReport;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReportStats;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.jaxrs.model.ReportTotalsEntry;
import org.folio.rest.jaxrs.model.ServicePoint;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;
import org.folio.rest.utils.ReportResourceClient;
import org.folio.test.support.EntityBuilder;
import org.folio.test.support.matcher.constant.ServicePath;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.restassured.response.Response;

public class FinancialTransactionDetailReportTest extends FeeFineReportsAPITestBase {
  private static final String START_DATE = "2020-01-01";
  private static final String END_DATE = "2020-01-15";
  private static final String OWNER_1 = "Owner 1";
  private static final String OWNER_ID_1 = randomId();
  private static final String OWNER_2 = "Owner 2";
  private static final String OWNER_ID_2 = randomId();
  private static final String USER_1 = "Patron 1";
  private static final String USER_BARCODE_1 = "user-barcode-1";
  private static final String USER_ID_1 = randomId();
  private static final String USER_2 = "Patron 2";
  private static final String USER_BARCODE_2 = "user-barcode-2";
  private static final String USER_ID_2 = randomId();
  private static final String USER_GROUP_1 = "Group 1";
  private static final String USER_GROUP_2 = "Group 2";
  private static final String CREATED_AT_1 = "Service point 1";
  private static final String CREATED_AT_ID_1 = randomId();
  private static final String CREATED_AT_2 = "Service point 2";
  private static final String CREATED_AT_ID_2 = randomId();
  private static final String SOURCE_1 = "Source 1";
  private static final String SOURCE_ID_1 = randomId();
  private static final String SOURCE_2 = "Source 2";
  private static final String SOURCE_ID_2 = randomId();
  private static final String FEE_FINE_TYPE_1 = "Fee/fine type 1";
  private static final String FEE_FINE_TYPE_2 = "Fee/fine type 2";
  private static final String PAYMENT_METHOD_1 = "Payment method 1";
  private static final String PAYMENT_METHOD_2 = "Payment method 2";
  private static final String PAYMENT_TX_INFO = "Payment transaction information";
  private static final String PAYMENT_STAFF_INFO = "Payment - info for staff";
  private static final String PAYMENT_PATRON_INFO = "Payment - info for patron";
  private static final String LOAN_DATE_1_RAW = "2020-01-01 01:00:00";
  private static final String LOAN_DATE_1 = withTenantTz(LOAN_DATE_1_RAW,
    LOAN_DATE_TIME_REPORT_FORMATTER);
  private static final Date DUE_DATE_1 = parseDateTimeUTC("2020-01-01 12:00:00");
  private static final String RETURN_DATE_1_RAW = "2020-01-01 05:00:00";
  private static final String RETURN_DATE_1 = withTenantTz(RETURN_DATE_1_RAW,
    LOAN_RETURN_DATE_TIME_REPORT_FORMATTER);

  private static final String FEE_FINE_OWNER_TOTALS = "Fee/fine owner totals";
  private static final String FEE_FINE_TYPE_TOTALS = "Fee/fine type totals";
  private static final String ACTION_TOTALS = "Action totals";
  private static final String PAYMENT_METHOD_TOTALS = "Payment method totals";
  private static final String WAIVE_REASON_TOTALS = "Waive reason totals";
  private static final String REFUND_REASON_TOTALS = "Refund reason totals";
  private static final String TRANSFER_ACCOUNT_TOTALS = "Transfer account totals";

  private static final String ZERO_MONETARY = "0.00";
  private static final String ZERO_COUNT = "0";

  private final ReportResourceClient reportClient = buildFinancialTransactionsDetailReportClient();

  private UserGroup userGroup;
  private Location location;
  private Instance instance;
  private HoldingsRecord holdingsRecord;
  private User user1;
  private User user2;
  private Item item1;
  private Item item2;
  private LoanPolicy loanPolicy;
  private LostItemFeePolicy lostItemFeePolicy;
  private OverdueFinePolicy overdueFinePolicy;
  private ServicePoint servicePoint1;
  private ServicePoint servicePoint2;
  private Loan loan1;

  private StubMapping user1StubMapping;
  private StubMapping userGroupStubMapping;
  private StubMapping itemStubMapping;
  private StubMapping loanStubMapping;
  private StubMapping holdingsStubMapping;
  private StubMapping instanceStubMapping;
  private StubMapping locationStubMapping;

  @Before
  public void setUp() {
    clearDatabase();
    createLocaleSettingsStub();

    loanPolicy = buildLoanPolicy("Loan policy");
    createStub(ServicePath.LOAN_POLICIES_PATH, loanPolicy, loanPolicy.getId());
    overdueFinePolicy = buildOverdueFinePolicy("ofp-1" + randomId());
    createOverdueFinePolicy(overdueFinePolicy);
    lostItemFeePolicy = buildLostItemFeePolicy("lfp-1" + randomId());
    createLostItemFeePolicy(lostItemFeePolicy);

    servicePoint1 = buildServicePoint(CREATED_AT_ID_1, CREATED_AT_1);
    createStub(ServicePath.SERVICE_POINTS_PATH, servicePoint1, servicePoint1.getId());
    servicePoint2 = buildServicePoint(CREATED_AT_ID_2, CREATED_AT_2);
    createStub(ServicePath.SERVICE_POINTS_PATH, servicePoint2, servicePoint2.getId());

    location = buildLocation("Location");
    locationStubMapping = createStub(ServicePath.LOCATIONS_PATH, location, location.getId());

    instance = buildInstance();
    holdingsRecord = buildHoldingsRecord(instance);
    item1 = buildItem(holdingsRecord, location);
    item2 = buildItem(holdingsRecord, location).withBarcode("item2-barcode");
    loan1 = buildLoan(LOAN_DATE_1, DUE_DATE_1, RETURN_DATE_1, item1.getId(), loanPolicy.getId(),
      overdueFinePolicy.getId(), lostItemFeePolicy.getId());
    loanStubMapping = createStub(ServicePath.LOANS_PATH, loan1, loan1.getId());

    itemStubMapping = createStub(ServicePath.ITEMS_PATH, item1, item1.getId());
    createStub(ServicePath.ITEMS_PATH, item2, item2.getId());
    holdingsStubMapping = createStub(HOLDINGS_PATH, holdingsRecord, holdingsRecord.getId());
    instanceStubMapping = createStub(INSTANCES_PATH, instance, instance.getId());

    userGroup = EntityBuilder.buildUserGroup().withGroup(USER_GROUP_1);
    userGroupStubMapping = createStub(USERS_GROUPS_PATH, userGroup, userGroup.getId());

    user1 = EntityBuilder.buildUser()
      .withId(USER_ID_1)
      .withPatronGroup(userGroup.getId())
      .withBarcode(USER_BARCODE_1);
    user1StubMapping = createStub(USERS_PATH, user1, USER_ID_1);

    createStub(USERS_PATH, EntityBuilder.buildUser().withId(SOURCE_ID_1), SOURCE_ID_1);
    createStub(USERS_PATH, EntityBuilder.buildUser().withId(SOURCE_ID_2), SOURCE_ID_2);
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
    Account account = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_1, null, OWNER_ID_1);

    createAction(USER_ID_1, 1, account, "2019-12-31 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD_1,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO, CREATED_AT_ID_1, SOURCE_1);

    createAction(USER_ID_1, 1, account, "2020-02-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD_1,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO, CREATED_AT_ID_1, SOURCE_1);

    requestAndCheck(emptyReport());
  }

  @Test
  public void shouldReturn422WhenRequestIsNotValid() {
    reportClient.getFinancialTransactionsDetailReport(null, "2020-01-01", List.of(CREATED_AT_ID_1),
      OWNER_ID_1, HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getFinancialTransactionsDetailReport(null, null, List.of(CREATED_AT_ID_1),
      OWNER_ID_1, HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getFinancialTransactionsDetailReport("2020-01-01", "2020-01-02",
      List.of(CREATED_AT_ID_1), null, HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getFinancialTransactionsDetailReport("2020-01-01", "2020-01-02", null, null,
      HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getFinancialTransactionsDetailReport("not-a-date", "2020-01-01",
      List.of(CREATED_AT_ID_1), OWNER_ID_1, HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getFinancialTransactionsDetailReport("2020-01-01", "not-a-date",
      List.of(CREATED_AT_ID_1), OWNER_ID_1, HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getFinancialTransactionsDetailReport("not-a-date", "not-a-date",
      List.of(CREATED_AT_ID_1), OWNER_ID_1, HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getFinancialTransactionsDetailReport("1 Jan 2021", null, List.of(CREATED_AT_ID_1),
      OWNER_ID_1, HTTP_UNPROCESSABLE_ENTITY);
    reportClient.getFinancialTransactionsDetailReport("2020-01-01", "2020-01-02",
      List.of(CREATED_AT_ID_1), "not-a-uuid", HTTP_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturn200WhenRequestIsValid() {
    reportClient.getFinancialTransactionsDetailReport("2020-01-01", null,
      null, OWNER_ID_1, HTTP_OK);
    reportClient.getFinancialTransactionsDetailReport("2020-01-01", "2020-01-02",
      List.of(CREATED_AT_ID_1), OWNER_ID_1, HTTP_OK);
    reportClient.getFinancialTransactionsDetailReport("2020-01-01", "2020-01-02",
      List.of(CREATED_AT_ID_1, CREATED_AT_ID_2), OWNER_ID_1, HTTP_OK);
  }

  @Test
  public void returnsEmptyResultWhenAccountIsDeleted() {
    Pair<Account, Feefineaction> sourceObjects = createMinimumViableReportData();

    assert sourceObjects.getLeft() != null;
    deleteEntity(ACCOUNTS_PATH, sourceObjects.getLeft().getId());

    assert sourceObjects.getRight() != null;
    requestAndCheck(emptyReport(), START_DATE, END_DATE, List.of(CREATED_AT_ID_1), OWNER_ID_1);
  }

  @Test
  public void emptyResultWhenFilteredByWrongOwner() {
    createMinimumViableReportData();

    requestAndCheck(emptyReport(), START_DATE, END_DATE, List.of(CREATED_AT_ID_1), OWNER_ID_2);
  }

  @Test
  public void emptyResultWhenFilteredByWrongServicePoint() {
    createMinimumViableReportData();

    requestAndCheck(emptyReport(), START_DATE, END_DATE, List.of(CREATED_AT_ID_2), OWNER_ID_1);
  }

  @Test
  public void nonEmptyResultWhenFilteredByOwnerAndServicePoint() {
    createMinimumViableReportData();

    requestAndCheckNonEmpty(START_DATE, END_DATE, List.of(CREATED_AT_ID_1), OWNER_ID_1);
  }

  @Test
  public void validReportWhenActionsExist() {
    String chargeActionDate = withTenantTz("2020-01-01 00:00:01");
    Account account1 = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_1, item1.getId(), loan1,
      OWNER_ID_1, OWNER_1, chargeActionDate, CREATED_AT_ID_1, SOURCE_1);

    Feefineaction paymentAction1 = createAction(USER_ID_1, 1, account1, withTenantTz("2020-01-01 00:10:00"),
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT_ID_1, SOURCE_1);

    requestAndCheck(new FinancialTransactionsDetailReport()
      .withReportData(List.of(
        buildFinancialTransactionsDetailReportEntry(
          OWNER_1,
          FEE_FINE_TYPE_1,
          "10.00",
          formatReportDate(parseDateTimeUTC(chargeActionDate)),
          CREATED_AT_1,
          SOURCE_1,
          account1.getId(),
          "Payment",
          "3.00",
          formatReportDate(paymentAction1.getDateAction()),
          CREATED_AT_1,
          SOURCE_1,
          PAID_PARTIALLY,
          addSuffix(PAYMENT_STAFF_INFO, 1),
          addSuffix(PAYMENT_PATRON_INFO, 1),
          PAYMENT_METHOD_1,
          PAYMENT_TX_INFO,
          "",
          "",
          "",
          USER_ID_1,
          getFullName(user1),
          USER_BARCODE_1,
          USER_GROUP_1,
          user1.getPersonal().getEmail(),
          instance.getTitle(),
          "Primary contributor, Non-primary contributor",
          item1.getBarcode(),
          item1.getEffectiveCallNumberComponents().getCallNumber(),
          location.getName(),
          formatReportDate(parseDateTimeTenantTz(LOAN_DATE_1_RAW)),
          formatReportDate(loan1.getDueDate()),
          formatReportDate(parseDateTimeTenantTz(RETURN_DATE_1_RAW)),
          loanPolicy.getId(),
          loanPolicy.getName(),
          overdueFinePolicy.getId(),
          overdueFinePolicy.getName(),
          lostItemFeePolicy.getId(),
          lostItemFeePolicy.getName(),
          loan1.getId(),
          holdingsRecord.getId(),
          instance.getId(),
          item1.getId())
      ))
      .withReportStats(new FinancialTransactionsDetailReportStats()
        .withByFeeFineOwner(List.of(
          buildReportTotalsEntry(OWNER_1, "3.00", "1"),
          buildReportTotalsEntry(FEE_FINE_OWNER_TOTALS, "3.00", "1")))
        .withByFeeFineType(List.of(
          buildReportTotalsEntry(FEE_FINE_TYPE_1, "3.00", "1"),
          buildReportTotalsEntry(FEE_FINE_TYPE_TOTALS, "3.00", "1")))
        .withByAction(List.of(
          buildReportTotalsEntry("Payment", "3.00", "1"),
          buildReportTotalsEntry(ACTION_TOTALS, "3.00", "1")))
        .withByPaymentMethod(List.of(
          buildReportTotalsEntry(PAYMENT_METHOD_1, "3.00", "1"),
          buildReportTotalsEntry(PAYMENT_METHOD_TOTALS, "3.00", "1")))
        .withByWaiveReason(List.of(
          buildReportTotalsEntry(WAIVE_REASON_TOTALS, "0.00", "0")))
        .withByRefundReason(List.of(
          buildReportTotalsEntry(REFUND_REASON_TOTALS, "0.00", "0")))
        .withByTransferAccount(List.of(
          buildReportTotalsEntry(TRANSFER_ACCOUNT_TOTALS, "0.00", "0")))
      ));
  }

  @Test
  public void validResponseWhenUserIsMissing() {
    Pair<Account, Feefineaction> sourceObjects =  createMinimumViableReportData();

    removeStub(user1StubMapping);
    removeStub(userGroupStubMapping);
    removeStub(itemStubMapping);
    removeStub(loanStubMapping);
    removeStub(holdingsStubMapping);
    removeStub(instanceStubMapping);
    removeStub(locationStubMapping);

    assert sourceObjects.getLeft() != null;
    requestReport(START_DATE, END_DATE, List.of(CREATED_AT_ID_1), OWNER_ID_1).then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(1))
      .body("reportData[0].feeFineId", is(sourceObjects.getLeft().getId()));
  }

  private void requestAndCheck(FinancialTransactionsDetailReport report) {
    requestAndCheck(report, START_DATE, END_DATE, List.of(CREATED_AT_ID_1), OWNER_ID_1);
  }

  private void requestAndCheck(FinancialTransactionsDetailReport report,
    String startDate, String endDate, List<String> createdAt, String owner) {

    requestReport(startDate, endDate, createdAt, owner)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body(financialTransactionsDetailReportMatcher(report));
  }

  private void requestAndCheckNonEmpty(String startDate, String endDate,
    List<String> createdAt, String owner) {

    requestReport(startDate, endDate, createdAt, owner)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(not(0)));;
  }

  private Response requestReport(String startDate, String endDate, List<String> createdAt,
    String owner) {

    return reportClient.getFinancialTransactionsDetailReport(startDate, endDate, createdAt, owner);
  }

  private FinancialTransactionsDetailReport emptyReport() {
    return new FinancialTransactionsDetailReport()
      .withReportData(List.of())
      .withReportStats(new FinancialTransactionsDetailReportStats()
        .withByFeeFineOwner(emptyTotalsEntry(FEE_FINE_OWNER_TOTALS))
        .withByFeeFineType(emptyTotalsEntry(FEE_FINE_TYPE_TOTALS))
        .withByAction(emptyTotalsEntry(ACTION_TOTALS))
        .withByPaymentMethod(emptyTotalsEntry(PAYMENT_METHOD_TOTALS))
        .withByWaiveReason(emptyTotalsEntry(WAIVE_REASON_TOTALS))
        .withByRefundReason(emptyTotalsEntry(REFUND_REASON_TOTALS))
        .withByTransferAccount(emptyTotalsEntry(TRANSFER_ACCOUNT_TOTALS)));
  }

  private List<ReportTotalsEntry> emptyTotalsEntry(String totalsName) {
    return List.of(new ReportTotalsEntry()
      .withName(totalsName)
      .withTotalAmount(ZERO_MONETARY)
      .withTotalCount(ZERO_COUNT));
  }

  private Pair<Account, Feefineaction> createMinimumViableReportData() {
    String chargeActionDate = withTenantTz("2020-01-01 00:00:01");
    Account account = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_1, item1.getId(), loan1,
      OWNER_ID_1, OWNER_1, chargeActionDate, CREATED_AT_ID_1, SOURCE_1);

    Feefineaction action = createAction(USER_ID_1, 1, account, withTenantTz("2020-01-01 00:10:00"),
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT_ID_1, SOURCE_1);

    return Pair.of(account, action);
  }

  private String getFullName(User user) {
    return format("%s, %s %s", user.getPersonal().getLastName(),
      user.getPersonal().getFirstName(), user.getPersonal().getMiddleName());
  }
}
