package org.folio.test.support;

import static java.lang.String.format;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.OUTSTANDING;
import static org.folio.test.support.ApiTests.randomId;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.BlockTemplate;
import org.folio.rest.jaxrs.model.Campus;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportEntry;
import org.folio.rest.jaxrs.model.Config;
import org.folio.rest.jaxrs.model.Contributor;
import org.folio.rest.jaxrs.model.EffectiveCallNumberComponents;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Institution;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.KvConfigurations;
import org.folio.rest.jaxrs.model.Library;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.ManualBlockTemplate;
import org.folio.rest.jaxrs.model.Manualblock;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.ReportTotalsEntry;
import org.folio.rest.jaxrs.model.ServicePoint;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;

public class EntityBuilder {
  private static final String KEY_NAME = "name";
  private static final String KEY_ID = "id";
  private static final String PRIMARY_CONTRIBUTOR_NAME = "Primary contributor";
  private static final String NON_PRIMARY_CONTRIBUTOR_NAME = "Non-primary contributor";

  private EntityBuilder() {}

  public static User buildUser() {
    return new User()
      .withId(randomId())
      .withBarcode("54321")
      .withPersonal(new Personal()
        .withFirstName("First")
        .withMiddleName("Middle")
        .withLastName("Last")
        .withEmail("test@email.com"));
  }

  public static UserGroup buildUserGroup() {
    return new UserGroup()
      .withId(randomId())
      .withGroup("User Group");
  }

  public static Account buildAccount(String userId, String itemId, String feeFineType,
    double amount, String ownerId, String owner) {

    return new Account()
      .withId(randomId())
      .withOwnerId(ownerId)
      .withUserId(userId)
      .withItemId(itemId)
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType(feeFineType)
      .withFeeFineOwner(owner)
      .withAmount(new MonetaryValue(amount))
      .withRemaining(new MonetaryValue(amount))
      .withPaymentStatus(new PaymentStatus().withName(OUTSTANDING))
      .withStatus(new Status().withName("Open"));
  }

  public static Account buildAccount() {
    return new Account()
      .withId(randomId())
      .withOwnerId(randomId())
      .withUserId(randomId())
      .withItemId(randomId())
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType("book lost")
      .withFeeFineOwner("owner")
      .withAmount(new MonetaryValue(9.0))
      .withRemaining(new MonetaryValue(4.55))
      .withPaymentStatus(new PaymentStatus().withName(OUTSTANDING))
      .withStatus(new Status().withName("Open"));
  }

  public static Account buildAccount(String accountId) {
    return buildAccount().withId(accountId);
  }

  public static Account buildAccount(MonetaryValue amount, MonetaryValue remaining) {
    return buildAccount()
      .withAmount(amount)
      .withRemaining(remaining);
  }

  public static Feefineaction buildFeeFineAction(String userId, String accountId, String type,
      String paymentMethod, MonetaryValue amount, MonetaryValue balance, Date date,
      String commentForStaff, String commentForPatron, String transactionInformation,
      String createdAt, String source) {

    return new Feefineaction()
      .withId(randomId())
      .withUserId(userId)
      .withTypeAction(type)
      .withPaymentMethod(paymentMethod)
      .withAccountId(accountId)
      .withAmountAction(amount)
      .withBalance(balance)
      .withDateAction(date)
      .withComments(format("STAFF : %s \n PATRON : %s", commentForStaff, commentForPatron))
      .withTransactionInformation(transactionInformation)
      .withCreatedAt(createdAt)
      .withSource(source);
  }

  public static Feefineaction buildFeeFineActionWithoutComments(String userId, String accountId, String type,
    String paymentMethod, MonetaryValue amount, MonetaryValue balance, Date date) {

    return new Feefineaction()
      .withId(randomId())
      .withUserId(userId)
      .withTypeAction(type)
      .withPaymentMethod(paymentMethod)
      .withAccountId(accountId)
      .withAmountAction(amount)
      .withBalance(balance)
      .withDateAction(date);
  }

  public static Manualblock buildManualBlock() {
    return new Manualblock()
      .withId(randomId())
      .withUserId(randomId())
      .withDesc("Description")
      .withPatronMessage("Patron message")
      .withStaffInformation("Staff information")
      .withRequests(true)
      .withRenewals(true)
      .withBorrowing(true)
      .withExpirationDate(new Date());
  }

  public static ManualBlockTemplate buildManualBlockTemplate() {
    BlockTemplate blockTemplate = new BlockTemplate()
      .withDesc("Reader card lost")
      .withPatronMessage("Please contact library staff.")
      .withBorrowing(true)
      .withRenewals(true)
      .withRequests(true);
    return new ManualBlockTemplate()
      .withName("Reader card lost")
      .withCode("RCL")
      .withDesc("Use if reader card is lost")
      .withId(randomId())
      .withBlockTemplate(blockTemplate);
  }

  public static Item buildItem(HoldingsRecord holdingsRecord,
    Location location) {
    return new Item()
      .withId(randomId())
      .withHoldingsRecordId(holdingsRecord.getId())
      .withBarcode("12345")
      .withEnumeration("enum")
      .withVolume("vol.1")
      .withChronology("chronology")
      .withYearCaption(new HashSet<>(Collections.singletonList("2000")))
      .withCopyNumber("cp.1")
      .withNumberOfPieces("1")
      .withDescriptionOfPieces("little pieces")
      .withEffectiveLocationId(location.getId())
      .withEffectiveCallNumberComponents(
        new EffectiveCallNumberComponents()
          .withCallNumber("ABC.123.DEF")
          .withPrefix("PREFIX")
          .withSuffix("SUFFIX"));
  }

  public static HoldingsRecord buildHoldingsRecord(Instance instance) {
    return new HoldingsRecord()
      .withId(randomId())
      .withInstanceId(instance.getId())
      .withCopyNumber("cp.2");
  }

  public static Instance buildInstance() {
    return new Instance()
      .withId(randomId())
      .withTitle("Instance title")
      .withContributors(Arrays.asList(
        new Contributor().withName(PRIMARY_CONTRIBUTOR_NAME).withPrimary(true),
        new Contributor().withName(NON_PRIMARY_CONTRIBUTOR_NAME).withPrimary(false)));
  }

  public static Location buildLocation(String name) {
    return new Location()
      .withId(randomId())
      .withName(name);
  }

  public static Location buildLocation(Library library, Campus campus, Institution institution) {
    return new Location()
      .withId(randomId())
      .withName("Specific")
      .withCampusId(String.valueOf(campus.getAdditionalProperties().get(KEY_ID)))
      .withLibraryId(String.valueOf(library.getAdditionalProperties().get(KEY_ID)))
      .withInstitutionId(String.valueOf(institution.getAdditionalProperties().get(KEY_ID)));
  }

  public static Library buildLibrary() {
    return new Library()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Library");
  }

  public static Campus buildCampus() {
    return new Campus()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Campus");
  }

  public static Institution buildInstitution() {
    return new Institution()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Institution");
  }

  public static KvConfigurations buildLocaleSettingsConfigurations() {
    return new KvConfigurations()
      .withConfigs(List.of(new Config()
        .withId(randomId())
        .withModule("ORG")
        .withConfigName("localeSettings")
        .withEnabled(true)
        .withValue(
          "{\"locale\":\"en-US\",\"timezone\":\"America/New_York\",\"currency\":\"USD\"}")))
      .withTotalRecords(1);
  }

  public static KvConfigurations buildLocaleSettingsConfigurationsWithoutCurrency() {
    return new KvConfigurations()
      .withConfigs(List.of(new Config()
        .withId(randomId())
        .withModule("ORG")
        .withConfigName("localeSettings")
        .withEnabled(true)
        .withValue(
          "{\"locale\":\"en-US\",\"timezone\":\"America/New_York\"}")))
      .withTotalRecords(1);
  }

  public static Loan buildLoan(String loanDate, Date dueDate, String returnDate, String itemId,
    String loanPolicyId, String overdueFinePolicyId, String lostItemPolicyId) {

    return new Loan()
      .withId(randomId())
      .withLoanDate(loanDate)
      .withDueDate(dueDate)
      .withReturnDate(returnDate)
      .withItemId(itemId)
      .withLoanPolicyId(loanPolicyId)
      .withOverdueFinePolicyId(overdueFinePolicyId)
      .withLostItemPolicyId(lostItemPolicyId);
  }

  public static LoanPolicy buildLoanPolicy(String name) {
    return new LoanPolicy()
      .withId(randomId())
      .withName(name);
  }

  public static OverdueFinePolicy buildOverdueFinePolicy(String name) {
    return new OverdueFinePolicy()
      .withId(randomId())
      .withName(name);
  }

  public static LostItemFeePolicy buildLostItemFeePolicy(String name) {
    return new LostItemFeePolicy()
      .withId(randomId())
      .withName(name);
  }

  public static ServicePoint buildServicePoint(String id, String name) {
    return new ServicePoint()
      .withId(id)
      .withName(name);
  }

  public static CashDrawerReconciliationReportEntry buildCashDrawerReconciliationReportEntry(
    String source, String paymentMethod, String paidAmount, String feeFineOwner,
    String feeFineType, String paymentDate, String paymentStatus, String transactionInfo,
    String additionalStaffInfo, String additionalPatronInfo, String patronId, String feeFineId) {

    return new CashDrawerReconciliationReportEntry()
      .withSource(source)
      .withPaymentMethod(paymentMethod)
      .withPaidAmount(paidAmount)
      .withFeeFineOwner(feeFineOwner)
      .withFeeFineType(feeFineType)
      .withPaymentDate(paymentDate)
      .withPaymentStatus(paymentStatus)
      .withTransactionInfo(transactionInfo)
      .withAdditionalStaffInfo(additionalStaffInfo)
      .withAdditionalPatronInfo(additionalPatronInfo)
      .withPatronId(patronId)
      .withFeeFineId(feeFineId);
  }

  public static ReportTotalsEntry buildReportTotalsEntry(String name, String totalAmount,
    String totalCount) {

    return new ReportTotalsEntry()
      .withName(name)
      .withTotalAmount(totalAmount)
      .withTotalCount(totalCount);
  }
}
