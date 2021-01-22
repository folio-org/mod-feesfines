package org.folio.test.support;

import static java.lang.String.format;
import static org.folio.test.support.ApiTests.randomId;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.BlockTemplate;
import org.folio.rest.jaxrs.model.Campus;
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
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.ManualBlockTemplate;
import org.folio.rest.jaxrs.model.Manualblock;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;

public class EntityBuilder {
  private static final String KEY_NAME = "name";
  private static final String KEY_ID = "id";
  private static final String PRIMARY_CONTRIBUTOR_NAME = "Primary contributor";
  private static final String NON_PRIMARY_CONTRIBUTOR_NAME = "Non-primary contributor";

  private EntityBuilder() {}

  public static User createUser() {
    return new User()
      .withId(randomId())
      .withBarcode("54321")
      .withPersonal(new Personal()
        .withFirstName("First")
        .withMiddleName("Middle")
        .withLastName("Last"));
  }

  public static UserGroup createUserGroup() {
    return new UserGroup()
      .withId(randomId())
      .withGroup("User Group");
  }

  public static Account buildAccount(String userId, String itemId, String feeFineType,
    Double amount, String ownerId) {

    return new Account()
      .withId(randomId())
      .withOwnerId(ownerId)
      .withUserId(userId)
      .withItemId(itemId)
      .withLoanId(randomId())
      .withMaterialTypeId(randomId())
      .withFeeFineId(randomId())
      .withFeeFineType(feeFineType)
      .withFeeFineOwner("owner")
      .withAmount(amount)
      .withRemaining(amount)
      .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
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
      .withAmount(9.00)
      .withRemaining(4.55)
      .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
      .withStatus(new Status().withName("Open"));
  }

  public static Account buildAccount(String accountId) {
    return buildAccount().withId(accountId);
  }

  public static Account buildAccount(double amount, double remaining) {
    return buildAccount()
      .withAmount(amount)
      .withRemaining(remaining);
  }

  public static Feefineaction buildFeeFineAction(String userId, String accountId, String type,
    String paymentMethod, Double amount, Double balance, Date date,
    String commentForStaff, String commentForPatron) {

    return new Feefineaction()
      .withId(randomId())
      .withUserId(userId)
      .withTypeAction(type)
      .withPaymentMethod(paymentMethod)
      .withAccountId(accountId)
      .withAmountAction(amount)
      .withBalance(balance)
      .withDateAction(date)
      .withComments(format("STAFF : %s \n PATRON : %s", commentForStaff, commentForPatron));
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

  public static Item createItem(HoldingsRecord holdingsRecord,
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

  public static HoldingsRecord createHoldingsRecord(Instance instance) {
    return new HoldingsRecord()
      .withId(randomId())
      .withInstanceId(instance.getId())
      .withCopyNumber("cp.2");
  }

  public static Instance createInstance() {
    return new Instance()
      .withId(randomId())
      .withTitle("Instance title")
      .withContributors(Arrays.asList(
        new Contributor().withName(PRIMARY_CONTRIBUTOR_NAME).withPrimary(true),
        new Contributor().withName(NON_PRIMARY_CONTRIBUTOR_NAME).withPrimary(false)));
  }

  public static Location createLocation(Library library, Campus campus, Institution institution) {
    return new Location()
      .withId(randomId())
      .withName("Specific")
      .withCampusId(String.valueOf(campus.getAdditionalProperties().get(KEY_ID)))
      .withLibraryId(String.valueOf(library.getAdditionalProperties().get(KEY_ID)))
      .withInstitutionId(String.valueOf(institution.getAdditionalProperties().get(KEY_ID)));
  }

  public static Library createLibrary() {
    return new Library()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Library");
  }

  public static Campus createCampus() {
    return new Campus()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Campus");
  }

  public static Institution createInstitution() {
    return new Institution()
      .withAdditionalProperty(KEY_ID, randomId())
      .withAdditionalProperty(KEY_NAME, "Institution");
  }

  public static KvConfigurations createLocaleSettingsConfigurations() {
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
}
