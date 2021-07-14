package org.folio.rest.utils;

import static org.folio.rest.utils.FeeFineActionHelper.parseFeeFineComments;
import static org.folio.rest.utils.PatronNoticeBuilder.buildNotice;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Campus;
import org.folio.rest.jaxrs.model.Context;
import org.folio.rest.jaxrs.model.Contributor;
import org.folio.rest.jaxrs.model.EffectiveCallNumberComponents;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Institution;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Library;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.jaxrs.model.PatronNotice;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import io.vertx.core.json.JsonObject;

public class PatronNoticeBuilderTest {
  private static final String NAME = "name";

  private static final String CHARGE_COMMENT_FOR_PATRON = "Charge comment";
  private static final String ACTION_COMMENT_FOR_PATRON = "Action comment";

  private static final String PRIMARY_CONTRIBUTOR = "Primary contributor";
  private static final String NON_PRIMARY_CONTRIBUTOR = "Non-primary contributor";

  private static final String ACCOUNT_AMOUNT = "12.34";
  private static final String ACCOUNT_REMAINING = "5.67";
  private static final String ACTION_AMOUNT = "6.67";

  @Test
  public void createContextWithAllAvailableFields() {
    final User user = createUser();
    final Item item = createItem();
    final Location location = createLocation();
    final HoldingsRecord holdingsRecord = createHoldingsRecord();
    final Owner owner = createOwner();
    final Feefine feefine = createFeeFine();
    final Account account = createAccount();
    final Feefineaction action = createAction();
    final Feefineaction chargeAction = createChargeAction();
    final Instance instance = createInstance();

    final FeeFineNoticeContext sourceContext = new FeeFineNoticeContext()
      .withUser(createUser())
      .withItem(createItem())
      .withInstance(instance)
      .withHoldingsRecord(holdingsRecord)
      .withEffectiveLocation(location)
      .withOwner(owner)
      .withFeefine(feefine)
      .withAccount(account)
      .withAction(action)
      .withCharge(chargeAction);

    final PatronNotice patronNotice = buildNotice(sourceContext);

    assertEquals("email", patronNotice.getDeliveryChannel());
    assertEquals("text/html", patronNotice.getOutputFormat());
    assertEquals(sourceContext.getUserId(), patronNotice.getRecipientId());
    assertEquals(sourceContext.getTemplateId(), patronNotice.getTemplateId());

    final Context context = patronNotice.getContext();

    final JsonObject userContext = (JsonObject) context.getAdditionalProperties().get("user");

    assertEquals(user.getBarcode(), userContext.getString("barcode"));
    assertEquals(user.getPersonal().getFirstName(), userContext.getString("firstName"));
    assertEquals(user.getPersonal().getMiddleName(), userContext.getString("middleName"));
    assertEquals(user.getPersonal().getLastName(), userContext.getString("lastName"));

    final JsonObject itemContext = (JsonObject) context.getAdditionalProperties().get("item");

    assertEquals(item.getBarcode(), itemContext.getString("barcode"));
    assertEquals(item.getEnumeration(), itemContext.getString("enumeration"));
    assertEquals(item.getVolume(), itemContext.getString("volume"));
    assertEquals(item.getChronology(), itemContext.getString("chronology"));
    assertEquals("2001; 2000", itemContext.getString("yearCaption"));
    assertEquals(item.getCopyNumber(), itemContext.getString("copy"));
    assertEquals(item.getNumberOfPieces(), itemContext.getString("numberOfPieces"));
    assertEquals(item.getDescriptionOfPieces(), itemContext.getString("descriptionOfPieces"));

    final EffectiveCallNumberComponents callNumberComponents =
      item.getEffectiveCallNumberComponents();

    assertEquals(callNumberComponents.getCallNumber(), itemContext.getString("callNumber"));
    assertEquals(callNumberComponents.getPrefix(), itemContext.getString("callNumberPrefix"));
    assertEquals(callNumberComponents.getSuffix(), itemContext.getString("callNumberSuffix"));

    assertEquals(instance.getTitle(), itemContext.getString("title"));
    assertEquals(PRIMARY_CONTRIBUTOR, itemContext.getString("primaryContributor"));
    assertEquals(PRIMARY_CONTRIBUTOR + "; " + NON_PRIMARY_CONTRIBUTOR,
      itemContext.getString("allContributors"));

    assertEquals(location.getName(), itemContext.getString("effectiveLocationSpecific"));
    assertEquals(location.getLibrary().getAdditionalProperties().get(NAME),
      itemContext.getString("effectiveLocationLibrary"));
    assertEquals(location.getInstitution().getAdditionalProperties().get(NAME),
      itemContext.getString("effectiveLocationInstitution"));
    assertEquals(location.getCampus().getAdditionalProperties().get(NAME),
      itemContext.getString("effectiveLocationCampus"));

    assertEquals(account.getMaterialType(), itemContext.getString("materialType"));

    final JsonObject chargeContext = (JsonObject) context.getAdditionalProperties().get("feeCharge");

    assertEquals(account.getFeeFineOwner(), chargeContext.getString("owner"));
    assertEquals(account.getFeeFineType(), chargeContext.getString("type"));
    assertEquals(account.getPaymentStatus().getName(), chargeContext.getString("paymentStatus"));
    assertEquals(ACCOUNT_AMOUNT, chargeContext.getString("amount"));
    assertEquals(ACCOUNT_REMAINING, chargeContext.getString("remainingAmount"));
    assertEquals(dateToString(account.getMetadata().getCreatedDate()), chargeContext.getString("chargeDate"));
    assertEquals(dateToString(account.getMetadata().getCreatedDate()), chargeContext.getString("chargeDateTime"));
    assertEquals(CHARGE_COMMENT_FOR_PATRON, chargeContext.getString("additionalInfo"));


    final JsonObject actionContext = (JsonObject) context.getAdditionalProperties().get("feeAction");

    assertEquals(action.getTypeAction(), actionContext.getString("type"));
    assertEquals(dateToString(action.getDateAction()), actionContext.getString("actionDate"));
    assertEquals(dateToString(action.getDateAction()), actionContext.getString("actionDateTime"));
    assertEquals(action.getAmountAction().toString(), actionContext.getString("amount"));
    assertEquals(action.getBalance().toString(), actionContext.getString("remainingAmount"));
    assertEquals(ACTION_COMMENT_FOR_PATRON, actionContext.getString("additionalInfo"));
  }

  @Test
  public void useFallbackValuesFromAccountForItemContext() {
    final Account account = new Account()
      .withAmount(new MonetaryValue(new BigDecimal("10.0")))
      .withRemaining(new MonetaryValue(new BigDecimal("4.56")))
      .withBarcode("Account-level barcode")
      .withTitle("Account-level title")
      .withCallNumber("Account-level call number")
      .withLocation("Account-level location");

    final FeeFineNoticeContext sourceContext = new FeeFineNoticeContext()
      .withAccount(account)
      .withAction(createAction())
      .withFeefine(createFeeFine())
      .withOwner(createOwner());

    final JsonObject itemContext = (JsonObject) buildNotice(sourceContext)
      .getContext()
      .getAdditionalProperties()
      .get("item");

    assertEquals(account.getBarcode(), itemContext.getString("barcode"));
    assertEquals(account.getTitle(), itemContext.getString("title"));
    assertEquals(account.getCallNumber(), itemContext.getString("callNumber"));
    assertEquals(account.getLocation(), itemContext.getString("effectiveLocationSpecific"));
  }

  @Test
  public void canParseSeveralComments() {
    Map<String, String> parsedComments =
      parseFeeFineComments("STAFF : staff comment \n PATRON : patron comment");

    assertThat(parsedComments.size(), is(2));

    assertThat(parsedComments, allOf(
      hasEntry("STAFF", "staff comment"),
      hasEntry("PATRON", "patron comment")));
  }

  @Test
  public void canParseSingleComment() {
    Map<String, String> parsedComments =
      parseFeeFineComments("STAFF : staff comment");

    assertThat(parsedComments.size(), is(1));

    assertThat(parsedComments, hasEntry("STAFF", "staff comment"));
  }

  @Test
  public void canParseEmptyComment() {
    Map<String, String> parsedComments =
      parseFeeFineComments(StringUtils.EMPTY);

    assertThat(parsedComments.size(), is(0));
  }

  @Test
  public void canHandleDuplicateKeysInComments() {
    Map<String, String> parsedComments =
      parseFeeFineComments("STAFF : staff comment \n STAFF : second staff comment");

    assertThat(parsedComments.size(), is(1));

    assertThat(parsedComments, hasEntry("STAFF", "staff comment"));
  }

  @Test
  public void canHandleInvalidFormattingInComments() {
    Map<String, String> parsedComments =
      parseFeeFineComments("STAFF : staff comment \n PATRON:patron comment");

    assertThat(parsedComments.size(), is(1));

    assertThat(parsedComments, hasEntry("STAFF", "staff comment"));
  }

  private static Feefineaction createAction() {
    return new Feefineaction()
      .withTypeAction("Paid partially")
      .withDateAction(new Date())
      .withAmountAction(new MonetaryValue(new BigDecimal("4.45")))
      .withBalance(new MonetaryValue(new BigDecimal("8.55")))
      .withPaymentMethod("Cash")
      .withComments("STAFF : staff comment \n PATRON : " + ACTION_COMMENT_FOR_PATRON);
  }

  private static Feefineaction createChargeAction() {
    return new Feefineaction()
      .withTypeAction("Book lost")
      .withDateAction(new Date())
      .withAmountAction(new MonetaryValue(new BigDecimal(ACCOUNT_AMOUNT)))
      .withBalance(new MonetaryValue(new BigDecimal(ACCOUNT_REMAINING)))
      .withComments("STAFF : staff comment \n PATRON : " + CHARGE_COMMENT_FOR_PATRON);
  }

  private static Feefine createFeeFine() {
    return new Feefine()
      .withActionNoticeId(UUID.randomUUID().toString())
      .withChargeNoticeId(UUID.randomUUID().toString());
  }

  private static Instance createInstance() {
    return new Instance()
      .withTitle("Instance title")
      .withContributors(Arrays.asList(
        new Contributor().withName(PRIMARY_CONTRIBUTOR).withPrimary(true),
        new Contributor().withName(NON_PRIMARY_CONTRIBUTOR).withPrimary(false)));
  }

  private static Account createAccount() {
    return new Account()
      .withBarcode("Account-level barcode")
      .withTitle("Account-level title")
      .withCallNumber("Account-level call number")
      .withLocation("Account-level location")
      .withPaymentStatus(new PaymentStatus().withName("Partially paid"))
      .withFeeFineOwner("Owner")
      .withFeeFineType("Fine type")
      .withMaterialType("book")
      .withAmount(new MonetaryValue(new BigDecimal(ACCOUNT_AMOUNT)))
      .withRemaining(new MonetaryValue(new BigDecimal(ACCOUNT_REMAINING)))
      .withMetadata(new Metadata().withCreatedDate(new Date()));
  }

  private static HoldingsRecord createHoldingsRecord() {
    return new HoldingsRecord()
      .withCopyNumber("cp.2");
  }

  private static Location createLocation() {
    return new Location()
      .withName("Specific")
      .withLibrary(new Library().withAdditionalProperty(NAME, "Library"))
      .withInstitution(new Institution().withAdditionalProperty(NAME, "Institution"))
      .withCampus(new Campus().withAdditionalProperty(NAME, "Campus"));
  }

  private static Item createItem() {
    return new Item()
      .withBarcode("12345")
      .withEnumeration("enum")
      .withVolume("vol.1")
      .withChronology("chronology")
      .withYearCaption(new HashSet<>(Arrays.asList("2000", "2001")))
      .withCopyNumber("cp.1")
      .withNumberOfPieces("1")
      .withDescriptionOfPieces("little pieces")
      .withEffectiveCallNumberComponents(
        new EffectiveCallNumberComponents()
          .withCallNumber("ABC.123.DEF")
          .withPrefix("PREFIX")
          .withSuffix("SUFFIX"));
  }

  private static User createUser() {
    return new User()
      .withBarcode("54321")
      .withPersonal(new Personal()
        .withFirstName("First")
        .withMiddleName("Middle")
        .withLastName("Last"));
  }

  private static Owner createOwner() {
    return new Owner()
      .withDefaultActionNoticeId(UUID.randomUUID().toString())
      .withDefaultChargeNoticeId(UUID.randomUUID().toString());
  }

  static String dateToString(Date date) {
    return new DateTime(date, DateTimeZone.UTC).toString();
  }

}
