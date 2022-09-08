package org.folio.rest.impl;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.folio.HttpStatus.HTTP_OK;
import static org.folio.HttpStatus.HTTP_UNPROCESSABLE_ENTITY;
import static org.folio.rest.utils.ResourceClients.buildFinancialTransactionsDetailReportClient;
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
import static org.folio.test.support.matcher.constant.ServicePath.LOANS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.USERS_GROUPS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.USERS_PATH;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReport;
import org.folio.rest.jaxrs.model.FinancialTransactionsDetailReportEntry;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
  private static final String WAIVE_REASON_1 = "Waive reason 1";
  private static final String PAYMENT_TX_INFO = "Payment transaction information";
  private static final String PAYMENT_STAFF_INFO = "Payment - info for staff";
  private static final String PAYMENT_PATRON_INFO = "Payment - info for patron";
  private static final String WAIVE_STAFF_INFO = "Waive - info for staff";
  private static final String WAIVE_PATRON_INFO = "Waive - info for patron";

  // Loan 1
  private static final String LOAN_DATE_1_RAW = "2020-01-01 01:00:00";
  private static final String LOAN_DATE_1 = withTenantTz(LOAN_DATE_1_RAW,
    LOAN_DATE_TIME_REPORT_FORMATTER);
  private static final Date DUE_DATE_1 = parseDateTimeUTC("2020-01-01 12:00:00");
  private static final String RETURN_DATE_1_RAW = "2020-01-01 05:00:00";
  private static final String RETURN_DATE_1 = withTenantTz(RETURN_DATE_1_RAW,
    LOAN_RETURN_DATE_TIME_REPORT_FORMATTER);

  //Loan 2
  private static final String LOAN_DATE_2_RAW = "2020-01-02 01:00:00";
  private static final String LOAN_DATE_2 = withTenantTz(LOAN_DATE_2_RAW,
    LOAN_DATE_TIME_REPORT_FORMATTER);
  private static final Date DUE_DATE_2 = parseDateTimeUTC("2020-01-02 12:00:00");
  private static final String RETURN_DATE_2_RAW = "2020-01-02 05:00:00";
  private static final String RETURN_DATE_2 = withTenantTz(RETURN_DATE_2_RAW,
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

//  public static final String QUERY_BY_IDS_REGEX = "\\?query=id==\\(.+\\)&limit=\\d+";
  public static final String QUERY_BY_IDS_REGEX = "\\\\?.*";

  private final ReportResourceClient reportClient = buildFinancialTransactionsDetailReportClient();

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
  private Loan loan1;
  private Loan loan2;

  private StubMapping userStubMapping;
  private StubMapping userGroupStubMapping;
  private StubMapping itemStubMapping;
  private StubMapping loanStubMapping;
  private StubMapping holdingsStubMapping;
  private StubMapping instanceStubMapping;
  private StubMapping locationStubMapping;
  private StubMapping servicePointStubMapping;
  private StubMapping loanPolicyStubMapping;

  @BeforeEach
  public void setUp() {
    clearDatabase();
    createLocaleSettingsStub();

    loanPolicy = buildLoanPolicy("Loan policy");
    loanPolicyStubMapping = createStubForCollection(ServicePath.LOAN_POLICIES_PATH,
      List.of(loanPolicy), "loanPolicies");

    overdueFinePolicy = buildOverdueFinePolicy("ofp-1" + randomId());
    createOverdueFinePolicy(overdueFinePolicy);
    lostItemFeePolicy = buildLostItemFeePolicy("lfp-1" + randomId());
    createLostItemFeePolicy(lostItemFeePolicy);

    ServicePoint servicePoint1 = buildServicePoint(CREATED_AT_ID_1, CREATED_AT_1);
    ServicePoint servicePoint2 = buildServicePoint(CREATED_AT_ID_2, CREATED_AT_2);
    servicePointStubMapping = createStubForCollection(ServicePath.SERVICE_POINTS_PATH,
      List.of(servicePoint1, servicePoint2), "servicepoints");

    location = buildLocation("Location");
    locationStubMapping = createStubForCollection(ServicePath.LOCATIONS_PATH, List.of(location),
      "locations");

    instance = buildInstance();
    instanceStubMapping = createStubForCollection(INSTANCES_PATH, List.of(instance), "instances");

    holdingsRecord = buildHoldingsRecord(instance);
    holdingsStubMapping = createStubForCollection(HOLDINGS_PATH, List.of(holdingsRecord), "holdingsRecords");

    item1 = buildItem(holdingsRecord, location);
    item2 = buildItem(holdingsRecord, location).withBarcode("item2-barcode");
    itemStubMapping = createStubForCollection(ServicePath.ITEMS_PATH, List.of(item1, item2), "items");

    loan1 = buildLoan(LOAN_DATE_1, DUE_DATE_1, RETURN_DATE_1, item1.getId(), loanPolicy.getId(),
      overdueFinePolicy.getId(), lostItemFeePolicy.getId());
    loan2 = buildLoan(LOAN_DATE_2, DUE_DATE_2, RETURN_DATE_2, item2.getId(), loanPolicy.getId(),
      overdueFinePolicy.getId(), lostItemFeePolicy.getId());
    loanStubMapping = createStubForCollection(ServicePath.LOANS_PATH, List.of(loan1, loan2), "loans");

    UserGroup userGroup1 = EntityBuilder.buildUserGroup().withGroup(USER_GROUP_1);
    UserGroup userGroup2 = EntityBuilder.buildUserGroup().withGroup(USER_GROUP_2);
    userGroupStubMapping = createStubForCollection(USERS_GROUPS_PATH,
      List.of(userGroup1, userGroup2), "usergroups");

    user1 = EntityBuilder.buildUser()
      .withId(USER_ID_1)
      .withPatronGroup(userGroup1.getId())
      .withBarcode(USER_BARCODE_1);

    user2 = EntityBuilder.buildUser()
      .withId(USER_ID_2)
      .withPatronGroup(userGroup2.getId())
      .withBarcode(USER_BARCODE_2);

    userStubMapping = createStubForCollection(USERS_PATH, List.of(user1,user2), "users");
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
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO, CREATED_AT_ID_1,
      SOURCE_1);

    createAction(USER_ID_1, 1, account, "2020-02-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD_1,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO, CREATED_AT_ID_1,
      SOURCE_1);

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
    double chargedAmount1 = 10.0;
    String chargeActionDate1 = withTenantTz("2020-01-01 00:00:01");
    Account account1 = charge(USER_ID_1, chargedAmount1, FEE_FINE_TYPE_1, item1.getId(), loan1,
      OWNER_ID_1, OWNER_1, chargeActionDate1, CREATED_AT_ID_1, SOURCE_1);

    double chargedAmount2 = 100.0;
    String chargeActionDate2 = withTenantTz("2020-01-02 02:00:00");
    Account account2 = charge(USER_ID_2, chargedAmount2, FEE_FINE_TYPE_2, item2.getId(), loan2,
      OWNER_ID_1, OWNER_1, chargeActionDate2, CREATED_AT_ID_2, SOURCE_2);

    Feefineaction paymentAction1 = createAction(USER_ID_1, 1, account1,
      withTenantTz("2020-01-01 00:10:00"),
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT_ID_1, SOURCE_1);

    Feefineaction waiveAction1 = createAction(USER_ID_1, 1, account1,
      withTenantTz("2020-01-01 00:20:00"),
      WAIVED_PARTIALLY, WAIVE_REASON_1, 2.0, 5.0, WAIVE_STAFF_INFO, WAIVE_PATRON_INFO,
      "", CREATED_AT_ID_1, SOURCE_1);

    Feefineaction paymentAction2 = createAction(USER_ID_2, 2, account2,
      withTenantTz("2020-01-02 03:00:00"),
      PAID_FULLY, PAYMENT_METHOD_2, 100.0, 0.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT_ID_2, SOURCE_2);

    Account account3 = charge(USER_ID_1, chargedAmount1, FEE_FINE_TYPE_1, null, null,
      OWNER_ID_1, OWNER_1, chargeActionDate1, CREATED_AT_ID_1, SOURCE_1);

    Feefineaction paymentAction3 = createAction(USER_ID_1, 3, account3,
      withTenantTz("2020-01-03 00:10:00"),
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT_ID_1, SOURCE_1);

    requestAndCheck(new FinancialTransactionsDetailReport()
      .withReportData(List.of(
        new FinancialTransactionsDetailReportEntry()
          .withFeeFineOwner(OWNER_1)
          .withFeeFineType(FEE_FINE_TYPE_1)
          .withBilledAmount(new MonetaryValue(chargedAmount1).toString())
          .withDateBilled(formatReportDate(parseDateTimeUTC(chargeActionDate1)))
          .withFeeFineCreatedAt(CREATED_AT_1)
          .withFeeFineSource(SOURCE_1)
          .withFeeFineId(account1.getId())
          .withAction("Payment")
          .withActionAmount(paymentAction1.getAmountAction().toString())
          .withActionDate(formatReportDate(paymentAction1.getDateAction()))
          .withActionCreatedAt(CREATED_AT_1)
          .withActionSource(SOURCE_1)
          .withActionStatus(PAID_PARTIALLY)
          .withActionAdditionalStaffInfo(addSuffix(PAYMENT_STAFF_INFO, 1))
          .withActionAdditionalPatronInfo(addSuffix(PAYMENT_PATRON_INFO, 1))
          .withPaymentMethod(PAYMENT_METHOD_1)
          .withTransactionInfo(PAYMENT_TX_INFO)
          .withWaiveReason("")
          .withRefundReason("")
          .withTransferAccount("")
          .withPatronId(USER_ID_1)
          .withPatronName(getFullName(user1))
          .withPatronBarcode(USER_BARCODE_1)
          .withPatronGroup(USER_GROUP_1)
          .withPatronEmail(user1.getPersonal().getEmail())
          .withInstance(instance.getTitle())
          .withContributors("Primary contributor, Non-primary contributor")
          .withItemBarcode(item1.getBarcode())
          .withCallNumber(item1.getEffectiveCallNumberComponents().getCallNumber())
          .withEffectiveLocation(location.getName())
          .withLoanDate(formatReportDate(parseDateTimeTenantTz(LOAN_DATE_1_RAW)))
          .withDueDate(formatReportDate(loan1.getDueDate()))
          .withReturnDate(formatReportDate(parseDateTimeTenantTz(RETURN_DATE_1_RAW)))
          .withLoanPolicyId(loanPolicy.getId())
          .withLoanPolicyName(loanPolicy.getName())
          .withOverdueFinePolicyId(overdueFinePolicy.getId())
          .withOverdueFinePolicyName(overdueFinePolicy.getName())
          .withLostItemPolicyId(lostItemFeePolicy.getId())
          .withLostItemPolicyName(lostItemFeePolicy.getName())
          .withLoanId(loan1.getId())
          .withHoldingsRecordId(holdingsRecord.getId())
          .withInstanceId(instance.getId())
          .withItemId(item1.getId()),
        new FinancialTransactionsDetailReportEntry()
          .withFeeFineOwner(OWNER_1)
          .withFeeFineType(FEE_FINE_TYPE_1)
          .withBilledAmount(new MonetaryValue(chargedAmount1).toString())
          .withDateBilled(formatReportDate(parseDateTimeUTC(chargeActionDate1)))
          .withFeeFineCreatedAt(CREATED_AT_1)
          .withFeeFineSource(SOURCE_1)
          .withFeeFineId(account1.getId())
          .withAction("Waive")
          .withActionAmount(waiveAction1.getAmountAction().toString())
          .withActionDate(formatReportDate(waiveAction1.getDateAction()))
          .withActionCreatedAt(CREATED_AT_1)
          .withActionSource(SOURCE_1)
          .withActionStatus(WAIVED_PARTIALLY)
          .withActionAdditionalStaffInfo(addSuffix(WAIVE_STAFF_INFO, 1))
          .withActionAdditionalPatronInfo(addSuffix(WAIVE_PATRON_INFO, 1))
          .withPaymentMethod("")
          .withTransactionInfo("")
          .withWaiveReason(WAIVE_REASON_1)
          .withRefundReason("")
          .withTransferAccount("")
          .withPatronId(USER_ID_1)
          .withPatronName(getFullName(user1))
          .withPatronBarcode(USER_BARCODE_1)
          .withPatronGroup(USER_GROUP_1)
          .withPatronEmail(user1.getPersonal().getEmail())
          .withInstance(instance.getTitle())
          .withContributors("Primary contributor, Non-primary contributor")
          .withItemBarcode(item1.getBarcode())
          .withCallNumber(item1.getEffectiveCallNumberComponents().getCallNumber())
          .withEffectiveLocation(location.getName())
          .withLoanDate(formatReportDate(parseDateTimeTenantTz(LOAN_DATE_1_RAW)))
          .withDueDate(formatReportDate(loan1.getDueDate()))
          .withReturnDate(formatReportDate(parseDateTimeTenantTz(RETURN_DATE_1_RAW)))
          .withLoanPolicyId(loanPolicy.getId())
          .withLoanPolicyName(loanPolicy.getName())
          .withOverdueFinePolicyId(overdueFinePolicy.getId())
          .withOverdueFinePolicyName(overdueFinePolicy.getName())
          .withLostItemPolicyId(lostItemFeePolicy.getId())
          .withLostItemPolicyName(lostItemFeePolicy.getName())
          .withLoanId(loan1.getId())
          .withHoldingsRecordId(holdingsRecord.getId())
          .withInstanceId(instance.getId())
          .withItemId(item1.getId()),
        new FinancialTransactionsDetailReportEntry()
          .withFeeFineOwner(OWNER_1)
          .withFeeFineType(FEE_FINE_TYPE_2)
          .withBilledAmount(new MonetaryValue(chargedAmount2).toString())
          .withDateBilled(formatReportDate(parseDateTimeUTC(chargeActionDate2)))
          .withFeeFineCreatedAt(CREATED_AT_2)
          .withFeeFineSource(SOURCE_2)
          .withFeeFineId(account2.getId())
          .withAction("Payment")
          .withActionAmount(paymentAction2.getAmountAction().toString())
          .withActionDate(formatReportDate(paymentAction2.getDateAction()))
          .withActionCreatedAt(CREATED_AT_2)
          .withActionSource(SOURCE_2)
          .withActionStatus(PAID_FULLY)
          .withActionAdditionalStaffInfo(addSuffix(PAYMENT_STAFF_INFO, 2))
          .withActionAdditionalPatronInfo(addSuffix(PAYMENT_PATRON_INFO, 2))
          .withPaymentMethod(PAYMENT_METHOD_2)
          .withTransactionInfo(PAYMENT_TX_INFO)
          .withWaiveReason("")
          .withRefundReason("")
          .withTransferAccount("")
          .withPatronId(USER_ID_2)
          .withPatronName(getFullName(user2))
          .withPatronBarcode(USER_BARCODE_2)
          .withPatronGroup(USER_GROUP_2)
          .withPatronEmail(user2.getPersonal().getEmail())
          .withInstance(instance.getTitle())
          .withContributors("Primary contributor, Non-primary contributor")
          .withItemBarcode(item2.getBarcode())
          .withCallNumber(item2.getEffectiveCallNumberComponents().getCallNumber())
          .withEffectiveLocation(location.getName())
          .withLoanDate(formatReportDate(parseDateTimeTenantTz(LOAN_DATE_2_RAW)))
          .withDueDate(formatReportDate(loan2.getDueDate()))
          .withReturnDate(formatReportDate(parseDateTimeTenantTz(RETURN_DATE_2_RAW)))
          .withLoanPolicyId(loanPolicy.getId())
          .withLoanPolicyName(loanPolicy.getName())
          .withOverdueFinePolicyId(overdueFinePolicy.getId())
          .withOverdueFinePolicyName(overdueFinePolicy.getName())
          .withLostItemPolicyId(lostItemFeePolicy.getId())
          .withLostItemPolicyName(lostItemFeePolicy.getName())
          .withLoanId(loan2.getId())
          .withHoldingsRecordId(holdingsRecord.getId())
          .withInstanceId(instance.getId())
          .withItemId(item2.getId()),
        new FinancialTransactionsDetailReportEntry()
          .withFeeFineOwner(OWNER_1)
          .withFeeFineType(FEE_FINE_TYPE_1)
          .withBilledAmount(new MonetaryValue(chargedAmount1).toString())
          .withDateBilled(formatReportDate(parseDateTimeUTC(chargeActionDate1)))
          .withFeeFineCreatedAt(CREATED_AT_1)
          .withFeeFineSource(SOURCE_1)
          .withFeeFineId(account3.getId())
          .withAction("Payment")
          .withActionAmount(paymentAction3.getAmountAction().toString())
          .withActionDate(formatReportDate(paymentAction3.getDateAction()))
          .withActionCreatedAt(CREATED_AT_1)
          .withActionSource(SOURCE_1)
          .withActionStatus(PAID_PARTIALLY)
          .withActionAdditionalStaffInfo(addSuffix(PAYMENT_STAFF_INFO, 3))
          .withActionAdditionalPatronInfo(addSuffix(PAYMENT_PATRON_INFO, 3))
          .withPaymentMethod(PAYMENT_METHOD_1)
          .withTransactionInfo(PAYMENT_TX_INFO)
          .withWaiveReason("")
          .withRefundReason("")
          .withTransferAccount("")
          .withPatronId(USER_ID_1)
          .withPatronName(getFullName(user1))
          .withPatronBarcode(USER_BARCODE_1)
          .withPatronGroup(USER_GROUP_1)
          .withPatronEmail(user1.getPersonal().getEmail())
          .withInstance("")
          .withContributors("")
          .withItemBarcode("")
          .withCallNumber("")
          .withEffectiveLocation("")
          .withLoanDate("")
          .withDueDate("")
          .withReturnDate("")
          .withLoanPolicyId("")
          .withLoanPolicyName("")
          .withOverdueFinePolicyId("")
          .withOverdueFinePolicyName("")
          .withLostItemPolicyId("")
          .withLostItemPolicyName("")
          .withLoanId("")
          .withHoldingsRecordId("")
          .withInstanceId("")
          .withItemId("")
      ))
      .withReportStats(new FinancialTransactionsDetailReportStats()
        .withByFeeFineOwner(List.of(
          buildReportTotalsEntry(OWNER_1, "108.00", "4"),
          buildReportTotalsEntry(FEE_FINE_OWNER_TOTALS, "108.00", "4")))
        .withByFeeFineType(List.of(
          buildReportTotalsEntry(FEE_FINE_TYPE_1, "8.00", "3"),
          buildReportTotalsEntry(FEE_FINE_TYPE_2, "100.00", "1"),
          buildReportTotalsEntry(FEE_FINE_TYPE_TOTALS, "108.00", "4")))
        .withByAction(List.of(
          buildReportTotalsEntry("Payment", "106.00", "3"),
          buildReportTotalsEntry("Waive", "2.00", "1"),
          buildReportTotalsEntry(ACTION_TOTALS, "108.00", "4")))
        .withByPaymentMethod(List.of(
          buildReportTotalsEntry(PAYMENT_METHOD_1, "6.00", "2"),
          buildReportTotalsEntry(PAYMENT_METHOD_2, "100.00", "1"),
          buildReportTotalsEntry(PAYMENT_METHOD_TOTALS, "106.00", "3")))
        .withByWaiveReason(List.of(
          buildReportTotalsEntry(WAIVE_REASON_1, "2.00", "1"),
          buildReportTotalsEntry(WAIVE_REASON_TOTALS, "2.00", "1")))
        .withByRefundReason(List.of(
          buildReportTotalsEntry(REFUND_REASON_TOTALS, "0.00", "0")))
        .withByTransferAccount(List.of(
          buildReportTotalsEntry(TRANSFER_ACCOUNT_TOTALS, "0.00", "0")))
      ));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "2020-01-13T01:23:45.000000+0000",
    "2020-01-13T01:23:45.000000",
    "2020-01-13T01:23:45+00:00",
    "2020-01-13T01:23:45.000Z",
    "2020-01-13T01:23:45Z",
    "2020-01-13T01:23:45"
  })
  void canHandleLoanDateInAnyValidFormat(String loanDate) {
    Loan loan = buildLoan(loanDate, DUE_DATE_1, RETURN_DATE_1, item1.getId(), loanPolicy.getId(),
      overdueFinePolicy.getId(), lostItemFeePolicy.getId());

    removeStub(loanStubMapping);
    loanStubMapping = createStubForCollection(LOANS_PATH, List.of(loan), "loans");

    Account account = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_1, item1.getId(), loan,
      OWNER_ID_1, OWNER_1, withTenantTz("2020-01-02 02:00:00"), CREATED_AT_ID_1, SOURCE_1);

    createAction(USER_ID_1, 2, account, withTenantTz("2020-01-03 00:10:00"),
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT_ID_1, SOURCE_1);

    requestReport(START_DATE, END_DATE, singletonList(CREATED_AT_ID_1), OWNER_ID_1)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData[0].loanDate", equalTo("1/12/20, 8:23 PM"));
  }

  @Test
  public void validResponseWhenStubsAreMissing() {
    validResponseWithStubsMissing(() -> {
      removeStub(userStubMapping);
      removeStub(userGroupStubMapping);
      removeStub(itemStubMapping);
      removeStub(loanStubMapping);
      removeStub(holdingsStubMapping);
      removeStub(instanceStubMapping);
      removeStub(locationStubMapping);
      removeStub(servicePointStubMapping);
    });
  }

  @Test
  public void validResponseWhenUserGroupStubIsMissing() {
    validResponseWithStubsMissing(() -> {
      removeStub(userGroupStubMapping);
    });
  }

  @Test
  public void validResponseWhenFeeFinePoliciesAreMissing() {
    validResponseWithStubsMissing(() -> {
      deleteOverdueFinePolicy(overdueFinePolicy);
      deleteLostItemFeePolicy(lostItemFeePolicy);
    });
  }

  @Test
  public void validResponseWhenLoanPolicyIsMissing() {
    validResponseWithStubsMissing(() -> {
      removeStub(loanPolicyStubMapping);
    });
  }

  public void validResponseWithStubsMissing(Runnable removeStubsRunnable) {
    Pair<Account, Feefineaction> sourceObjects = createMinimumViableReportData();

    removeStubsRunnable.run();

    assert sourceObjects.getLeft() != null;
    requestReport(START_DATE, END_DATE, List.of(CREATED_AT_ID_1), OWNER_ID_1).then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(1))
      .body("reportData[0].feeFineId", is(sourceObjects.getLeft().getId()));
  }

  @Test
  public void validResponseWhenIdsAreInvalid() {
    String chargeActionDate = withTenantTz("2020-01-01 00:00:01");
    Account account1 = charge(USER_ID_1, 10.0, FEE_FINE_TYPE_1, item1.getId(), loan1,
      OWNER_ID_1, OWNER_1, chargeActionDate, CREATED_AT_ID_1, SOURCE_1);

    createAction(USER_ID_1, 1, account1, withTenantTz("2020-01-01 00:10:00"),
      PAID_PARTIALLY, PAYMENT_METHOD_1, 3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO, CREATED_AT_ID_1 + "-not-a-uuid", SOURCE_1);

    requestReport(START_DATE, END_DATE, List.of(CREATED_AT_ID_1), OWNER_ID_1).then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(0));
  }

  private void requestAndCheck(FinancialTransactionsDetailReport report) {
    requestAndCheck(report, START_DATE, END_DATE, List.of(CREATED_AT_ID_1, CREATED_AT_ID_2),
      OWNER_ID_1);
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
      .body("reportData", iterableWithSize(not(0)));
    ;
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
