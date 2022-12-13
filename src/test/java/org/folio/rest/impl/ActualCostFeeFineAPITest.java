package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.http.RequestMethod.GET;
import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static io.vertx.core.json.JsonObject.mapFrom;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.folio.test.support.matcher.AccountMatchers.singleAccountMatcher;
import static org.folio.test.support.matcher.FeeFineActionMatchers.feeFineAction;
import static org.folio.test.support.matcher.JsonMatchers.hasSameProperties;
import static org.folio.test.support.matcher.constant.ServicePath.ACTUAL_COST_RECORDS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.LOANS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.USERS_PATH;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActualCostFeeFineBill;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.rest.jaxrs.model.ActualCostRecordFeeFine;
import org.folio.rest.jaxrs.model.ActualCostRecordIdentifier;
import org.folio.rest.jaxrs.model.ActualCostRecordInstance;
import org.folio.rest.jaxrs.model.ActualCostRecordItem;
import org.folio.rest.jaxrs.model.ActualCostRecordLoan;
import org.folio.rest.jaxrs.model.ActualCostRecordUser;
import org.folio.rest.jaxrs.model.ContributorData;
import org.folio.rest.jaxrs.model.EffectiveCallNumberComponents;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.jaxrs.model.User;
import org.folio.test.support.ApiTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;

class ActualCostFeeFineAPITest extends ApiTests {
  private static final String ACTUAL_COST_RECORD_ID = "9e13ebc0-d749-4927-a29e-bdb337208e6a";
  private static final String FEE_FINE_TYPE_ID = "615ce21a-1a80-47c3-aab9-cd948244d5ba";
  private static final String OWNER_ID = "b3b16e50-8f6b-447e-9fa5-0709778b9144";
  private static final String MATERIAL_TYPE_ID = "5f3732dc-abf7-4bc4-9d5c-6f9462b80de3";
  private static final String LOAN_ID = "7205a409-13fe-46fc-9345-ffd1296b5251";
  private static final String USER_ID = "7ecd00da-fb50-4a34-8e8f-ad21f5d90e4c";
  private static final String ITEM_ID = "f26997ea-c06c-4d6b-9db1-cd9b1391cb0a";
  private static final String HOLDINGS_RECORD_ID = "012f3b47-8240-4d74-b553-e06a7b57566e";
  private static final String INSTANCE_ID = "a5cea7c9-17b5-44b1-add3-ec43311451a8";
  private static final String LOAN_POLICY_ID = "a94dd1ce-015b-4281-9455-7b7bffa50c81";
  private static final String OVERDUE_FINE_POLICY_ID = "c8997dee-22de-49cb-84a7-d6c5008bad02";
  private static final String LOST_ITEM_FEE_POLICY_ID = "c2642b90-dee3-4f10-ae0a-e2b5854893fd";
  private static final String SERVICE_POINT_ID = "bdfc9624-0d0a-40ac-91c1-2363641e01fa";

  private static final String ACTUAL_COST_CANCEL_PATH = "/actual-cost-fee-fine/cancel";
  private static final String ACTUAL_COST_BILL_PATH = "/actual-cost-fee-fine/bill";
  private static final String ACTUAL_COST_RECORD_PATH_TEMPLATE = ACTUAL_COST_RECORDS_PATH + "/%s";
  private static final String ACTUAL_COST_RECORD_PATH =
    String.format(ACTUAL_COST_RECORD_PATH_TEMPLATE, ACTUAL_COST_RECORD_ID);

  private static final double BILLING_AMOUNT = 9.99;
  private static final String STAFF_COMMENT = "Additional info for staff";
  private static final String PATRON_COMMENT = "Additional info for patron";
  private static final String ACTION_COMMENTS = String.format("STAFF : %s \n PATRON : %s",
    STAFF_COMMENT, PATRON_COMMENT);

  @BeforeEach
  void beforeEach() {
    removeAllFromTable(ACCOUNTS_TABLE);
    removeAllFromTable(FEE_FINE_ACTIONS_TABLE);
  }

  @Test
  void canPostActualCostCancelEntity() {
    String actualCostRecordId = randomId();

    String actualCostFeeFineCancel = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();

    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(ActualCostRecord.Status.OPEN)
      .withLoan(new ActualCostRecordLoan().withId(UUID.randomUUID().toString()));

    createStub(format(ACTUAL_COST_RECORD_PATH_TEMPLATE, actualCostRecordId), actualCostRecord);
    createStub(WireMock.put(urlPathEqualTo(format(ACTUAL_COST_RECORD_PATH_TEMPLATE, actualCostRecordId))),
      aResponse().withStatus(HttpStatus.SC_NO_CONTENT));

    postActualCostCancel(actualCostFeeFineCancel)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON)
      .body(allOf(List.of(
        hasJsonPath("id", is(actualCostRecordId)),
        hasJsonPath("additionalInfoForStaff", is("Test info for staff")),
        hasJsonPath("additionalInfoForPatron", is("Test info for patron")),
        hasJsonPath("status", is("Cancelled"))
      )));
  }

  @Test
  void postActualCostCancelShouldFailIfRecordIsNotFoundOnRetrieve() {
    String actualCostRecordId = randomId();
    String actualCostCancelEntity = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();
    createStub(WireMock.get(urlPathEqualTo(format(ACTUAL_COST_RECORD_PATH_TEMPLATE, actualCostRecordId))),
      aResponse().withStatus(HttpStatus.SC_NOT_FOUND));

    postActualCostCancel(actualCostCancelEntity)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(startsWith(format("ActualCostRecord %s was not found", actualCostRecordId)));
  }

  @Test
  void postActualCostCancelShouldFailIfRecordIsNotFoundOnUpdate() {
    String actualCostRecordId = randomId();
    String actualCostCancelEntity = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();
    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(ActualCostRecord.Status.OPEN);

    createStub(format(ACTUAL_COST_RECORD_PATH_TEMPLATE, actualCostRecordId), actualCostRecord);
    createStub(WireMock.put(urlPathEqualTo(format(ACTUAL_COST_RECORD_PATH_TEMPLATE, actualCostRecordId))),
      aResponse().withStatus(HttpStatus.SC_NOT_FOUND));

    postActualCostCancel(actualCostCancelEntity)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(startsWith(format("ActualCostRecord %s was not found", actualCostRecordId)));
  }

  @Test
  void postActualCostCancelShouldFailIfBadRequestOnUpdate() {
    String actualCostRecordId = randomId();
    String actualCostCancelEntity = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();
    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(ActualCostRecord.Status.OPEN);

    createStub(format(ACTUAL_COST_RECORD_PATH_TEMPLATE, actualCostRecordId), actualCostRecord);
    createStub(WireMock.put(urlPathEqualTo(format(ACTUAL_COST_RECORD_PATH_TEMPLATE, actualCostRecordId))),
      aResponse().withStatus(HttpStatus.SC_BAD_REQUEST));

    postActualCostCancel(actualCostCancelEntity)
      .then()
      .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
  }

  @ParameterizedTest
  @EnumSource(value = ActualCostRecord.Status.class, names = {"CANCELLED", "BILLED", "EXPIRED"})
  void postActualCostCancelShouldFailIfRecordAlreadyProcessed(ActualCostRecord.Status recordStatus) {
    String actualCostRecordId = randomId();
    String actualCostCancelEntity = new JsonObject()
      .put("actualCostRecordId", actualCostRecordId)
      .put("additionalInfoForStaff", "Test info for staff")
      .put("additionalInfoForPatron", "Test info for patron")
      .encodePrettily();
    ActualCostRecord actualCostRecord = new ActualCostRecord()
      .withId(actualCostRecordId)
      .withStatus(recordStatus);
    createStub(format(ACTUAL_COST_RECORD_PATH_TEMPLATE, actualCostRecordId), actualCostRecord);

    postActualCostCancel(actualCostCancelEntity)
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .body(startsWith(format("Actual cost record %s is already %s", actualCostRecordId, recordStatus)));
  }

  @Test
  void actualCostFeeFineIsBilledSuccessfully() {
    ActualCostRecord record = buildActualCostRecord();
    Loan loan = buildLoan();
    User sourceUser = buildUser();

    createStub(ACTUAL_COST_RECORDS_PATH, record, ACTUAL_COST_RECORD_ID);
    createStub(PUT, ACTUAL_COST_RECORD_PATH, noContent());
    createStub(LOANS_PATH, loan, loan.getId());
    createStub(USERS_PATH, sourceUser, sourceUser.getId());

    ActualCostFeeFineBill billingRequest = buildBillingRequest();
    ActualCostRecord billedRecord = bill(billingRequest)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .extract()
      .as(ActualCostRecord.class);

    String accountId = billedRecord.getFeeFine().getAccountId();
    assertThat(accountId, notNullValue());

    ActualCostRecord expectedBilledRecord = record
      .withStatus(ActualCostRecord.Status.BILLED)
      .withAdditionalInfoForPatron(billingRequest.getAdditionalInfoForPatron())
      .withAdditionalInfoForStaff(billingRequest.getAdditionalInfoForStaff())
      .withFeeFine(record.getFeeFine()
        .withAccountId(accountId)
        .withBilledAmount(billingRequest.getAmount()));

    assertThat(mapFrom(billedRecord), hasSameProperties(mapFrom(expectedBilledRecord)));

    okapiDeployment.verify(putRequestedFor(urlPathEqualTo(ACTUAL_COST_RECORD_PATH))
      .withRequestBody(equalToJson(mapFrom(billedRecord).encode())));

    Account expectedAccount = new Account()
      .withAmount(new MonetaryValue(BILLING_AMOUNT))
      .withRemaining(new MonetaryValue(BILLING_AMOUNT))
      .withFeeFineId(FEE_FINE_TYPE_ID)
      .withFeeFineType("fee/fine type")
      .withOwnerId(OWNER_ID)
      .withFeeFineOwner("owner")
      .withTitle("title")
      .withBarcode("item barcode")
      .withCallNumber("call number")
      .withLocation("effective location")
      .withMaterialTypeId(MATERIAL_TYPE_ID)
      .withMaterialType("material type")
      .withLoanId(LOAN_ID)
      .withUserId(USER_ID)
      .withItemId(ITEM_ID)
      .withHoldingsRecordId(HOLDINGS_RECORD_ID)
      .withInstanceId(INSTANCE_ID)
      .withDueDate(loan.getDueDate())
      .withReturnedDate(Date.from(ZonedDateTime.parse(loan.getReturnDate()).toInstant()))
      .withPaymentStatus(new PaymentStatus().withName(PaymentStatus.Name.OUTSTANDING))
      .withStatus(new Status().withName("Open"))
      .withContributors(singletonList(new ContributorData().withName("contributor")))
      .withLoanPolicyId(LOAN_POLICY_ID)
      .withOverdueFinePolicyId(OVERDUE_FINE_POLICY_ID)
      .withLostItemFeePolicyId(LOST_ITEM_FEE_POLICY_ID);

    accountsClient.getById(accountId)
      .then()
      .statusCode(HttpStatus.SC_OK)
      .contentType(JSON)
      .body(singleAccountMatcher(expectedAccount));

    feeFineActionsClient.getAll()
      .then()
      .body("feefineactions", hasSize(1))
      .body("feefineactions[0]", feeFineAction(
        accountId, USER_ID, BILLING_AMOUNT, new MonetaryValue(BILLING_AMOUNT), "fee/fine type",
        "-", "last, first", ACTION_COMMENTS, false, SERVICE_POINT_ID));
  }

  @Test
  void billingFailsWhenActualCostRecordIsNotFound() {
    createStub(GET, ACTUAL_COST_RECORD_PATH, notFound());

    bill(buildBillingRequest())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(TEXT)
      .body(equalTo("ActualCostRecord " + ACTUAL_COST_RECORD_ID + " was not found"));

    assertThatFeeFineWasNotBilled();
  }

  @Test
  void billingFailsWhenLoanIsNotFound() {
    createStub(ACTUAL_COST_RECORD_PATH, buildActualCostRecord());
    createStub(GET, LOANS_PATH + "/" + LOAN_ID, notFound());

    bill(buildBillingRequest())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(TEXT)
      .body(equalTo("Loan " + LOAN_ID + " was not found"));

    assertThatFeeFineWasNotBilled();
  }

  @Test
  void billingFailsWhenRequesterIsNotFound() {
    createStub(ACTUAL_COST_RECORD_PATH, buildActualCostRecord());
    createStub(LOANS_PATH + "/" + LOAN_ID, buildLoan());
    createStub(GET, USERS_PATH + "/" + X_OKAPI_USER_ID, notFound());

    bill(buildBillingRequest())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(TEXT)
      .body(equalTo("User " + X_OKAPI_USER_ID + " was not found"));

    assertThatFeeFineWasNotBilled();
  }

  @ParameterizedTest
  @EnumSource(value = ActualCostRecord.Status.class, mode = EXCLUDE, names = {"OPEN"})
  void billingFailsWhenActualCostRecordIsNotOpen(ActualCostRecord.Status status) {
    createStub(ACTUAL_COST_RECORD_PATH, buildActualCostRecord().withStatus(status));

    bill(buildBillingRequest())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(TEXT)
      .body(equalTo("Actual cost record " + ACTUAL_COST_RECORD_ID + " is already " + status.value()));

    assertThatFeeFineWasNotBilled();
  }

  private static ActualCostRecord buildActualCostRecord() {
    return new ActualCostRecord()
      .withId(ACTUAL_COST_RECORD_ID)
      .withLossType(ActualCostRecord.LossType.DECLARED_LOST)
      .withLossDate(new Date())
      .withStatus(ActualCostRecord.Status.OPEN)
      .withLoan(new ActualCostRecordLoan()
        .withId(LOAN_ID))
      .withItem(new ActualCostRecordItem()
        .withId(ITEM_ID)
        .withBarcode("item barcode")
        .withHoldingsRecordId(HOLDINGS_RECORD_ID)
        .withLoanType("loan type")
        .withLoanTypeId(randomId())
        .withMaterialType("material type")
        .withMaterialTypeId(MATERIAL_TYPE_ID)
        .withPermanentLocation("permanent location")
        .withPermanentLocationId(randomId())
        .withEffectiveLocation("effective location")
        .withEffectiveLocationId(randomId())
        .withEffectiveCallNumberComponents(new EffectiveCallNumberComponents()
          .withCallNumber("call number")
          .withPrefix("prefix")
          .withSuffix("suffix")
          .withTypeId(randomId()))
        .withChronology("chronology")
        .withCopyNumber("copy number")
        .withEnumeration("enumeration")
        .withVolume("volume"))
      .withInstance(new ActualCostRecordInstance()
        .withId(INSTANCE_ID)
        .withTitle("title")
        .withContributors(singletonList(new ContributorData()
          .withName("contributor")))
        .withIdentifiers(singletonList(new ActualCostRecordIdentifier()
          .withIdentifierType("identifier type")
          .withIdentifierTypeId(randomId())
          .withValue("identifier value"))))
      .withUser(new ActualCostRecordUser()
        .withId(USER_ID)
        .withBarcode("user barcode")
        .withFirstName("firstName")
        .withMiddleName("middleName")
        .withLastName("lastName")
        .withPatronGroup("patron group")
        .withPatronGroupId(randomId()))
      .withFeeFine(new ActualCostRecordFeeFine()
        .withType("fee/fine type")
        .withTypeId(FEE_FINE_TYPE_ID)
        .withOwner("owner")
        .withOwnerId(OWNER_ID));
  }

  private static User buildUser() {
    return new User()
      .withId(X_OKAPI_USER_ID)
      .withPersonal(new Personal()
        .withFirstName("first")
        .withLastName("last"));
  }

  private static Loan buildLoan() {
    return new Loan()
      .withId(LOAN_ID)
      .withDueDate(new Date())
      .withReturnDate(ZonedDateTime.now().toString())
      .withLoanPolicyId(LOAN_POLICY_ID)
      .withOverdueFinePolicyId(OVERDUE_FINE_POLICY_ID)
      .withLostItemPolicyId(LOST_ITEM_FEE_POLICY_ID);
  }

  private Response postActualCostCancel(String entity) {
    return client.post(ACTUAL_COST_CANCEL_PATH, entity);
  }

  private Response bill(ActualCostFeeFineBill billingRequest) {
    return client.post(ACTUAL_COST_BILL_PATH, mapFrom(billingRequest));
  }

  private void assertThatFeeFineWasNotBilled() {
    okapiDeployment.verify(0, putRequestedFor(urlPathEqualTo(ACTUAL_COST_RECORD_PATH)));

    accountsClient.getAll()
      .then()
      .body("accounts", empty());

    feeFineActionsClient.getAll()
      .then()
      .body("feefineactions", empty());
  }

  private static ActualCostFeeFineBill buildBillingRequest() {
    return new ActualCostFeeFineBill()
      .withActualCostRecordId(ACTUAL_COST_RECORD_ID)
      .withAmount(new MonetaryValue(BILLING_AMOUNT))
      .withAdditionalInfoForStaff(STAFF_COMMENT)
      .withAdditionalInfoForPatron(PATRON_COMMENT)
      .withServicePointId(SERVICE_POINT_ID);
  }

}
