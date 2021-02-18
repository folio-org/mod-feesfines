package org.folio.rest.impl;

import static java.lang.String.format;
import static org.folio.HttpStatus.HTTP_OK;
import static org.folio.HttpStatus.HTTP_UNPROCESSABLE_ENTITY;
import static org.folio.rest.utils.ResourceClients.buildRefundReportClient;
import static org.folio.test.support.EntityBuilder.createCampus;
import static org.folio.test.support.EntityBuilder.createHoldingsRecord;
import static org.folio.test.support.EntityBuilder.createInstance;
import static org.folio.test.support.EntityBuilder.createInstitution;
import static org.folio.test.support.EntityBuilder.createItem;
import static org.folio.test.support.EntityBuilder.createLibrary;
import static org.folio.test.support.EntityBuilder.createLocaleSettingsConfigurations;
import static org.folio.test.support.EntityBuilder.createLocation;
import static org.folio.test.support.matcher.RefundReportEntryMatcher.refundReportEntryMatcher;
import static org.folio.test.support.matcher.constant.DbTable.ACCOUNTS_TABLE;
import static org.folio.test.support.matcher.constant.DbTable.FEEFINES_TABLE;
import static org.folio.test.support.matcher.constant.DbTable.FEE_FINE_ACTIONS_TABLE;
import static org.folio.test.support.matcher.constant.ServicePath.ACCOUNTS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.HOLDINGS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.INSTANCES_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.USERS_GROUPS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.USERS_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Campus;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Institution;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.KvConfigurations;
import org.folio.rest.jaxrs.model.Library;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.RefundReportEntry;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;
import org.folio.rest.utils.ReportResourceClient;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.folio.test.support.matcher.constant.ServicePath;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

public class FeeFineReportsAPITest extends ApiTests {
  private static final String USER_ID_1 = randomId();
  private static final String USER_ID_2 = randomId();
  private static final String OWNER_ID_1 = randomId();
  private static final String OWNER_ID_2 = randomId();
  private static final String START_DATE = "2020-01-01";
  private static final String END_DATE = "2020-01-15";
  private static final DateTimeZone TENANT_TZ = DateTimeZone.forID("America/New_York");

  private static final String PAYMENT_METHOD = "payment-method";
  private static final String REFUND_REASON = "refund-reason";
  private static final String TRANSFER_ACCOUNT = "Bursar";

  private static final String PAID_PARTIALLY = "Paid partially";
  private static final String PAID_FULLY = "Paid fully";
  private static final String TRANSFERRED_PARTIALLY = "Transferred partially";
  private static final String TRANSFERRED_FULLY = "Transferred fully";
  private static final String REFUNDED_PARTIALLY = "Refunded partially";
  private static final String REFUNDED_FULLY = "Refunded fully";

  private static final String PAYMENT_STAFF_INFO = "Payment - info for staff";
  private static final String PAYMENT_PATRON_INFO = "Payment - info for patron";
  private static final String REFUND_STAFF_INFO = "Refund - info for staff";
  private static final String REFUND_PATRON_INFO = "Refund - info for patron";

  private static final String PAYMENT_TX_INFO = "Payment transaction information";
  private static final String REFUND_TX_INFO = "Refund transaction information";
  private static final String TRANSFER_TX_INFO = "Transfer transaction information";
  private static final String REFUNDED_TO_PATRON_TX_INFO = "Refunded to patron";
  private static final String REFUNDED_TO_BURSAR_TX_INFO = "Refunded to Bursar";

  private static final String MULTIPLE = "Multiple";
  private static final String SEE_FEE_FINE_PAGE = "See Fee/fine details page";

  private static final String FEE_FINE_OWNER = "owner";

  private static final DateTimeFormatter dateTimeFormatter =
    DateTimeFormat.forPattern("M/d/yy, h:mm a");

  private final ReportResourceClient refundReportsClient =
    buildRefundReportClient();

  private UserGroup userGroup;
  private User user1;
  private User user2;
  private Item item1;
  private Item item2;
  private Instance instance;

  private StubMapping user1StubMapping;
  private StubMapping userGroupStubMapping;
  private StubMapping localeSettingsStubMapping;
  private StubMapping itemStubMapping;
  private StubMapping holdingsStubMapping;
  private StubMapping instanceStubMapping;

  @Before
  public void setUp() {
    removeAllFromTable(FEEFINES_TABLE);
    removeAllFromTable(ACCOUNTS_TABLE);
    removeAllFromTable(FEE_FINE_ACTIONS_TABLE);

    final KvConfigurations localeSettingsConfigurations = createLocaleSettingsConfigurations();
    localeSettingsStubMapping = createStubForPath(ServicePath.CONFIGURATION_ENTRIES,
      localeSettingsConfigurations, ".*");

    final Library library = createLibrary();
    final Campus campus = createCampus();
    final Institution institution = createInstitution();
    final Location location = createLocation(library, campus, institution);
    instance = createInstance();
    final HoldingsRecord holdingsRecord = createHoldingsRecord(instance);
    item1 = createItem(holdingsRecord, location);

    item2 = createItem(holdingsRecord, location)
    .withBarcode("item2-barcode");

    itemStubMapping = createStub(ServicePath.ITEMS_PATH, item1, item1.getId());
    createStub(ServicePath.ITEMS_PATH, item2, item2.getId());
    holdingsStubMapping = createStub(HOLDINGS_PATH, holdingsRecord, holdingsRecord.getId());
    instanceStubMapping = createStub(INSTANCES_PATH, instance, instance.getId());

    userGroup = EntityBuilder.createUserGroup();
    userGroupStubMapping = createStub(USERS_GROUPS_PATH, userGroup, userGroup.getId());

    user1 = EntityBuilder.createUser()
      .withId(USER_ID_1)
      .withPatronGroup(userGroup.getId());
    user1StubMapping = createStub(USERS_PATH, user1, USER_ID_1);

    user2 = EntityBuilder.createUser()
      .withId(USER_ID_2)
      .withPersonal(new Personal()
        .withFirstName("First2")
        .withLastName("Last2")
        .withMiddleName("Middle2"))
      .withBarcode("77777")
      .withPatronGroup(userGroup.getId());
    createStub(USERS_PATH, user2, USER_ID_2);
  }

  @Test
  public void okResponseWhenLocaleConfigDoesNotExist() {
    removeStub(localeSettingsStubMapping);

    requestAndCheck(List.of());
  }

  @Test
  public void okResponseWhenLocaleConfigExists() {
    requestAndCheck(List.of());
  }

  @Test
  public void emptyReportWhenRefundedAfterEndDate() {
    Account account = charge(10.0, "ff-type", null);

    createAction(1, account, "2020-01-02 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    createAction(1, account, "2020-02-01 12:00:00", REFUNDED_PARTIALLY, REFUND_REASON,
      2.0, 7.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    requestAndCheck(List.of());
  }

  @Test
  public void shouldReturn422WhenRequestIsNotValid() {
    refundReportsClient.getFeeFineRefundReports(null, "2020-01-01", HTTP_UNPROCESSABLE_ENTITY);
    refundReportsClient.getFeeFineRefundReports("not-a-date", "2020-01-01", HTTP_UNPROCESSABLE_ENTITY);
    refundReportsClient.getFeeFineRefundReports("2020-01-01", "not-a-date", HTTP_UNPROCESSABLE_ENTITY);
    refundReportsClient.getFeeFineRefundReports("not-a-date", "not-a-date", HTTP_UNPROCESSABLE_ENTITY);
  }

  @Test
  public void shouldReturn200WhenRequestIsValid() {
    refundReportsClient.getFeeFineRefundReports("2020-01-01", null, HTTP_OK);
    refundReportsClient.getFeeFineRefundReports(null, null, HTTP_OK);
    refundReportsClient.getFeeFineRefundReports("2020-01-01", "2020-02-01", HTTP_OK);
  }

  @Test
  public void returnsResultWhenAccountIsDeleted() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    assert sourceObjects.account != null;
    deleteEntity(ACCOUNTS_PATH, sourceObjects.account.getId());

    assert sourceObjects.refundAction != null;
    requestRefundReport(START_DATE, END_DATE).then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(1))
      .body("reportData[0].feeFineId", is(sourceObjects.account.getId()));
  }

  @Test
  public void returnsResultWhenUserDoesNotExist() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    removeStub(user1StubMapping);

    assert sourceObjects.account != null;
    requestRefundReport(START_DATE, END_DATE).then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(1))
      .body("reportData[0].feeFineId", is(sourceObjects.account.getId()));
  }

  @Test
  public void returnsResultWhenUserGroupDoesNotExist() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    removeStub(userGroupStubMapping);

    assert sourceObjects.account != null;
    requestRefundReport(START_DATE, END_DATE).then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(1))
      .body("reportData[0].feeFineId", is(sourceObjects.account.getId()));
  }

  @Test
  public void validReportWhenItemDoesNotExist() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    removeStub(itemStubMapping);

    requestAndCheck(List.of(createResponseForMinimumViableData(sourceObjects)
      .withItemBarcode("")
      .withInstance("")));
  }

  @Test
  public void validReportWhenHoldingsRecordDoesNotExist() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    removeStub(holdingsStubMapping);

    assert sourceObjects.account != null;
    assert sourceObjects.refundAction != null;

    requestAndCheck(List.of(createResponseForMinimumViableData(sourceObjects)
      .withInstance("")));
  }

  @Test
  public void validReportWhenInstanceDoesNotExist() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    removeStub(instanceStubMapping);

    requestAndCheck(List.of(createResponseForMinimumViableData(sourceObjects)
      .withInstance("")));
  }

  @Test
  public void validReportWhenEndDateIsNull() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    removeStub(instanceStubMapping);

    requestAndCheckWithoutSpecificDates(List.of(createResponseForMinimumViableData(sourceObjects)
      .withInstance("")), null, START_DATE, null);
  }

  @Test
  public void validReportWhenStartDateAndEndDateAreNull() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    removeStub(instanceStubMapping);

    requestAndCheckWithoutSpecificDates(List.of(createResponseForMinimumViableData(sourceObjects)
      .withInstance("")), null, null, null);
  }

  @Test
  public void partiallyRefundedWithNoItem() {
    Account account = charge(10.0, "ff-type", null);

    createAction(1, account, "2020-01-02 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    Feefineaction refundAction = createAction(1, account, "2020-01-03 12:00:00",
      REFUNDED_PARTIALLY, REFUND_REASON, 2.0, 7.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account, refundAction,
        "3.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1), "", "", FEE_FINE_OWNER)
    ));
  }

  @Test
  public void partiallyRefundedWithItem() {
    ReportSourceObjects sourceObjects = createMinimumViableReportData();

    requestAndCheck(List.of(createResponseForMinimumViableData(sourceObjects)));
  }

  @Test
  public void fullyRefundedTimeZoneTest() {
    Account account = charge(10.0, "ff-type", item1.getId());

    createAction(1, account, "2020-01-01 01:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    Feefineaction refundAction = createAction(1, account, "2020-01-15 01:00:00",
      REFUNDED_FULLY, REFUND_REASON, 3.0, 7.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account, refundAction,
        "3.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER)
    ));
  }

  @Test
  public void multiplePaymentsSameMethodFullyRefunded() {
    Account account = charge(10.0, "ff-type", item1.getId());

    createAction(1, account, "2020-01-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.1, 6.9, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    createAction(2, account, "2020-01-02 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      2.1, 4.8, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    Feefineaction refundAction = createAction(1, account, "2020-01-03 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 5.2, 4.8, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account, refundAction,
        "5.20", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER)
    ));
  }

  @Test
  public void refundActionWithoutComments() {
    Account account = charge(10.0, "ff-type", item1.getId());

    createAction(1, account, "2020-01-01 12:00:00", PAID_FULLY, PAYMENT_METHOD,
      10.0, 0.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    Feefineaction refundAction = createActionWithNullComments(account, "2020-01-03 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 5.2, 4.8, REFUND_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account, refundAction,
        "10.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
        "", "", item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER)
    ));
  }

  @Test
  public void multiplePaymentMethodsFullyRefunded() {
    Account account = charge(10.0, "ff-type", item1.getId());

    createAction(1, account, "2020-01-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.1, 6.9, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    createAction(2, account, "2020-01-02 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD + "-different-method", 2.1, 4.8,
      PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    Feefineaction refundAction = createAction(1, account, "2020-01-03 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 5.2, 4.8, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account, refundAction,
        "5.20", MULTIPLE, PAYMENT_TX_INFO, "0.00", "",
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER)
    ));
  }

  @Test
  public void partiallyTransferredFullyRefunded() {
    Account account = charge(10.0, "ff-type", item1.getId());

    createAction(1, account, "2020-01-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    createAction(1, account, "2020-01-02 12:00:00",
      TRANSFERRED_PARTIALLY, TRANSFER_ACCOUNT, 1.5, 8.5, "", "", TRANSFER_TX_INFO);

    Feefineaction refundAction = createAction(1, account, "2020-01-03 12:00:00",
      REFUNDED_PARTIALLY, REFUND_REASON, 1.0, 8.5, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account, refundAction,
        "3.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "1.50", TRANSFER_ACCOUNT,
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER)
    ));
  }

  @Test
  public void partiallyTransferredFullyRefundedToPatron() {
    Account account = charge(10.0, "ff-type", item1.getId());

    createAction(1, account, "2020-01-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    createAction(1, account, "2020-01-02 12:00:00",
      TRANSFERRED_PARTIALLY, TRANSFER_ACCOUNT, 1.5, 8.5, "", "", TRANSFER_TX_INFO);

    Feefineaction refundAction = createAction(1, account, "2020-01-03 12:00:00",
      REFUNDED_PARTIALLY, REFUND_REASON, 1.0, 8.5, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUNDED_TO_PATRON_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account, refundAction,
        "3.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "", "",
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER)
    ));
  }

  @Test
  public void partiallyTransferredFullyRefundedToBursar() {
    Account account = charge(10.0, "ff-type", item1.getId());

    createAction(1, account, "2020-01-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    createAction(1, account, "2020-01-02 12:00:00",
      TRANSFERRED_PARTIALLY, TRANSFER_ACCOUNT, 1.5, 8.5, "", "", TRANSFER_TX_INFO);

    Feefineaction refundAction = createAction(1, account, "2020-01-03 12:00:00",
      REFUNDED_PARTIALLY, REFUND_REASON, 1.0, 8.5, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUNDED_TO_BURSAR_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account, refundAction,
        "", "", "", "1.50", TRANSFER_ACCOUNT,
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER)
    ));
  }

  @Test
  public void multipleAccountMultipleRefunds() {
    Account account1 = charge(10.0, "ff-type-1", item1.getId());

    createAction(1, account1, "2020-01-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.1, 6.9, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    createAction(2, account1, "2020-01-02 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD,
      3.2, 3.7, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO + "-different-info");

    createAction(1, account1, "2020-01-03 12:00:00",
      TRANSFERRED_PARTIALLY, TRANSFER_ACCOUNT, 2.0, 5.7, "", "", TRANSFER_TX_INFO);

    Feefineaction refundAction1 = createAction(1, account1, "2020-01-04 12:00:00",
      REFUNDED_PARTIALLY, REFUND_REASON, 1.0, 5.7, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    createAction(2, account1, "2020-01-05 12:00:00",
      PAID_FULLY, PAYMENT_METHOD + "-different-method",
      5.7, 0.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, REFUND_TX_INFO);

    Feefineaction refundAction2 = createAction(2, account1, "2020-01-06 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 9.0, 0.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    Account account2 = charge(20.0, "ff-type-2", null);

    createAction(1, account2, "2020-01-07 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      17.0, 3.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    Feefineaction refundAction3 = createAction(1, account2, "2020-01-08 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 17.0, 3.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    Account account3 = charge(USER_ID_2, 20.0, "ff-type-3", item2.getId(), OWNER_ID_1);

    createAction(USER_ID_2, 1, account3, "2020-01-08 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      17.0, 3.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);

    Feefineaction refundAction4 = createAction(USER_ID_2, 1, account3, "2020-01-09 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 17.0, 3.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    requestAndCheck(List.of(
      buildRefundReportEntry(account1, refundAction1,
        "6.30", PAYMENT_METHOD, SEE_FEE_FINE_PAGE, "2.00", TRANSFER_ACCOUNT,
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER),
      buildRefundReportEntry(account1, refundAction2,
        "12.00", MULTIPLE, SEE_FEE_FINE_PAGE, "2.00", TRANSFER_ACCOUNT,
        addSuffix(REFUND_STAFF_INFO, 2), addSuffix(REFUND_PATRON_INFO, 2),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER),
      buildRefundReportEntry(account2, refundAction3,
        "17.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        "", "", FEE_FINE_OWNER),
      buildRefundReportEntry(user2, account3, refundAction4,
        "17.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item2.getBarcode(), instance.getTitle(), FEE_FINE_OWNER)
    ));
  }

  @Test
  public void shouldFormReportOnlyForSpecificOwner() {
    Account account1 = charge(USER_ID_1, 10.0, "ff-type-1", item1.getId(), OWNER_ID_1);
    createAction(1, account1, "2020-01-01 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.1, 6.9, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);
    createAction(2, account1, "2020-01-02 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      3.2, 3.7, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO + "-different-info");
    createAction(1, account1, "2020-01-03 12:00:00",
      TRANSFERRED_PARTIALLY, TRANSFER_ACCOUNT, 2.0, 5.7, "", "", TRANSFER_TX_INFO);
    Feefineaction refundAction1 = createAction(1, account1, "2020-01-04 12:00:00",
      REFUNDED_PARTIALLY, REFUND_REASON, 1.0, 5.7, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);
    createAction(2, account1, "2020-01-05 12:00:00",
      PAID_FULLY, PAYMENT_METHOD + "-different-method",
      5.7, 0.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, REFUND_TX_INFO);
    Feefineaction refundAction2 = createAction(2, account1, "2020-01-06 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 9.0, 0.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    Account account2 = charge(USER_ID_1, 20.0, "ff-type-2", item2.getId(), OWNER_ID_2);
    createAction(1, account2, "2020-01-07 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      17.0, 3.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);
    Feefineaction refundAction3 = createAction(1, account2, "2020-01-08 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 17.0, 3.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    Account account3 = charge(USER_ID_2, 20.0, "ff-type-3", item2.getId(), OWNER_ID_1);
    createAction(USER_ID_2, 1, account3, "2020-01-08 12:00:00", PAID_PARTIALLY, PAYMENT_METHOD,
      17.0, 3.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO, PAYMENT_TX_INFO);
    Feefineaction refundAction4 = createAction(USER_ID_2, 1, account3, "2020-01-09 12:00:00",
      REFUNDED_FULLY, REFUND_REASON, 17.0, 3.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    List<RefundReportEntry> refundReportEntriesForFirstOwner = List.of(
      buildRefundReportEntry(account1, refundAction1,
        "6.30", PAYMENT_METHOD, SEE_FEE_FINE_PAGE, "2.00", TRANSFER_ACCOUNT,
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER),
      buildRefundReportEntry(account1, refundAction2,
        "12.00", MULTIPLE, SEE_FEE_FINE_PAGE, "2.00", TRANSFER_ACCOUNT,
        addSuffix(REFUND_STAFF_INFO, 2), addSuffix(REFUND_PATRON_INFO, 2),
        item1.getBarcode(), instance.getTitle(), FEE_FINE_OWNER),
      buildRefundReportEntry(user2, account3, refundAction4,
        "17.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
        addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
        item2.getBarcode(), instance.getTitle(), FEE_FINE_OWNER));

    List<RefundReportEntry> refundReportEntriesForSecondOwner = List.of(buildRefundReportEntry(account2, refundAction3,
      "17.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
      addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1),
      item2.getBarcode(), instance.getTitle(), FEE_FINE_OWNER));

    requestAndCheck(refundReportEntriesForFirstOwner, List.of(OWNER_ID_1));
    requestAndCheck(refundReportEntriesForSecondOwner, List.of(OWNER_ID_2));

    List<RefundReportEntry> summaryRefundReportEntries = new ArrayList<>();
    summaryRefundReportEntries.addAll(refundReportEntriesForFirstOwner);
    summaryRefundReportEntries.addAll(2, refundReportEntriesForSecondOwner);

    requestAndCheck(summaryRefundReportEntries, List.of(OWNER_ID_1, OWNER_ID_2));
  }

  private void requestAndCheck(List<RefundReportEntry> reportEntries) {
    requestAndCheck(reportEntries, null);
  }

  private void requestAndCheck(List<RefundReportEntry> reportEntries,
    List<String> ownerIds) {

    ValidatableResponse response = requestRefundReport(START_DATE, END_DATE, ownerIds)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(reportEntries.size()));

    IntStream.range(0, reportEntries.size())
      .forEach(index -> response.body(format("reportData[%d]", index),
        refundReportEntryMatcher(reportEntries.get(index))));
  }

  private void requestAndCheckWithoutSpecificDates(List<RefundReportEntry> reportEntries,
    List<String> ownerIds, String startDate, String endDate) {

    ValidatableResponse response = requestRefundReport(startDate, endDate, ownerIds)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .body("reportData", iterableWithSize(reportEntries.size()));

    IntStream.range(0, reportEntries.size())
      .forEach(index -> response.body(format("reportData[%d]", index),
        refundReportEntryMatcher(reportEntries.get(index))));
  }

  private Account charge(Double amount, String feeFineType, String itemId) {
    return charge(USER_ID_1, amount, feeFineType, itemId, randomId());
  }

  private Account charge(String userID, Double amount, String feeFineType,
    String itemId, String ownerId) {

    final var account = EntityBuilder.buildAccount(userID, itemId, feeFineType, amount, ownerId);
    createEntity(ACCOUNTS_PATH, account);

    return accountsClient.getById(account.getId()).as(Account.class);
  }

  private ReportSourceObjects createMinimumViableReportData() {
    Account account = charge(10.0, "ff-type", item1.getId());

    Feefineaction payment = createAction(1, account, "2020-01-02 12:00:00",
      PAID_PARTIALLY, PAYMENT_METHOD, 3.0, 7.0, PAYMENT_STAFF_INFO, PAYMENT_PATRON_INFO,
      PAYMENT_TX_INFO);

    Feefineaction refund = createAction(1, account, "2020-01-01 12:00:00",
      REFUNDED_PARTIALLY, REFUND_REASON, 2.0, 7.0, REFUND_STAFF_INFO, REFUND_PATRON_INFO,
      REFUND_TX_INFO);

    return new ReportSourceObjects()
      .withAccount(account)
      .withPaymentAction(payment)
      .withRefundAction(refund);
  }

  private RefundReportEntry createResponseForMinimumViableData(ReportSourceObjects sourceObjects) {
    return buildRefundReportEntry(sourceObjects.account, sourceObjects.refundAction,
      "3.00", PAYMENT_METHOD, PAYMENT_TX_INFO, "0.00", "",
      addSuffix(REFUND_STAFF_INFO, 1), addSuffix(REFUND_PATRON_INFO, 1), item1.getBarcode(),
      instance.getTitle(), FEE_FINE_OWNER);
  }

  private Feefineaction createAction(int actionCounter, Account account, String dateTime,
    String type, String method, Double amount, Double balance, String staffInfo,
    String patronInfo, String txInfo) {

    return createAction(USER_ID_1, actionCounter, account, dateTime, type, method, amount, balance,
      staffInfo, patronInfo, txInfo);
  }

  private Feefineaction createAction(String userId, int actionCounter, Account account, String dateTime,
    String type, String method, Double amount, Double balance, String staffInfo,
    String patronInfo, String txInfo) {

    Feefineaction action = EntityBuilder.buildFeeFineAction(userId, account.getId(),
      type, method, amount, balance, parseDateTime(dateTime),
      addSuffix(staffInfo, actionCounter), addSuffix(patronInfo, actionCounter))
      .withTransactionInformation(txInfo);

    createEntity(ServicePath.ACTIONS_PATH, action);

    return action;
  }

  private Feefineaction createActionWithNullComments(Account account, String dateTime,
    String type, String method, Double amount, Double balance, String txInfo) {

    Feefineaction action = EntityBuilder.buildFeeFineActionWithoutComments(USER_ID_1, account.getId(),
      type, method, amount, balance, parseDateTime(dateTime))
      .withTransactionInformation(txInfo);

    createEntity(ServicePath.ACTIONS_PATH, action);

    return action;
  }

  private RefundReportEntry buildRefundReportEntry(Account account,
    Feefineaction refundAction, String paidAmount, String paymentMethod, String transactionInfo,
    String transferredAmount, String transferAccount, String staffInfo, String patronInfo,
    String itemBarcode, String instance, String feeFineOwner) {
    return buildRefundReportEntry(user1, account, refundAction, paidAmount, paymentMethod,
      transactionInfo, transferredAmount, transferAccount, staffInfo, patronInfo, itemBarcode,
      instance, feeFineOwner);
  }

  private RefundReportEntry buildRefundReportEntry(User user, Account account,
    Feefineaction refundAction, String paidAmount, String paymentMethod, String transactionInfo,
    String transferredAmount, String transferAccount, String staffInfo, String patronInfo,
    String itemBarcode, String instance, String feeFineOwner) {

    if (account == null || refundAction == null) {
      return null;
    }

    return new RefundReportEntry()
      .withPatronName(format("%s, %s %s", user.getPersonal().getLastName(),
        user.getPersonal().getFirstName(), user.getPersonal().getMiddleName()))
      .withPatronBarcode(user.getBarcode())
      .withPatronId(user.getId())
      .withPatronGroup(userGroup.getGroup())
      .withFeeFineType(account.getFeeFineType())
      .withBilledAmount(formatMonetaryValue(account.getAmount()))
      .withDateBilled(formatRefundReportDate(account.getMetadata().getCreatedDate(), TENANT_TZ))
      .withPaidAmount(paidAmount)
      .withPaymentMethod(paymentMethod)
      .withTransactionInfo(transactionInfo)
      .withTransferredAmount(transferredAmount)
      .withTransferAccount(transferAccount)
      .withFeeFineId(account.getId())
      .withRefundDate(formatRefundReportDate(refundAction.getDateAction(), TENANT_TZ))
      .withRefundAmount(formatMonetaryValue(refundAction.getAmountAction()))
      .withRefundAction(refundAction.getTypeAction())
      .withRefundReason(refundAction.getPaymentMethod())
      .withStaffInfo(staffInfo)
      .withPatronInfo(patronInfo)
      .withItemBarcode(itemBarcode)
      .withInstance(instance)
      .withActionCompletionDate("")
      .withStaffMemberName("")
      .withActionTaken("")
      .withFeeFineOwner(feeFineOwner);
  }

  private String addSuffix(String info, int counter) {
    return format("%s %d", info, counter);
  }

  private Response requestRefundReport(String startDate, String endDate) {
    return requestRefundReport(startDate, endDate, null);
  }

  private Response requestRefundReport(String startDate, String endDate, List<String> ownerIds) {
    return refundReportsClient.getFeeFineRefundReports(startDate, endDate, ownerIds);
  }

  private String formatMonetaryValue(Double value) {
    return new MonetaryValue(value).toString();
  }

  private String formatRefundReportDate(Date date, DateTimeZone timeZone) {
    return new DateTime(date).withZone(timeZone).toString(dateTimeFormatter);
  }

  private Date parseDateTime(String date) {
    return DateTime.parse(date, DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")).toDate();
  }

  @With
  @AllArgsConstructor
  @NoArgsConstructor(force = true)
  private static class ReportSourceObjects {
    final Account account;
    final Feefineaction paymentAction;
    final Feefineaction refundAction;
  }
}
