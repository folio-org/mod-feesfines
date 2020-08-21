package org.folio.rest.utils;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.rest.utils.JsonHelper.writeIfDoesNotExist;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.domain.Money;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Context;
import org.folio.rest.jaxrs.model.Contributor;
import org.folio.rest.jaxrs.model.EffectiveCallNumberComponents;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.PatronNotice;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.User;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.vertx.core.json.JsonObject;

public class PatronNoticeBuilder {
  private static final String PATRON_COMMENTS_KEY = "PATRON";
  private static final String LIST_VALUES_SEPARATOR = "; ";
  private static final String BARCODE = "barcode";
  public static final String TITLE = "title";
  public static final String CALL_NUMBER = "callNumber";
  public static final String EFFECTIVE_LOCATION_SPECIFIC = "effectiveLocationSpecific";

  private PatronNoticeBuilder() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static PatronNotice buildNotice(FeeFineNoticeContext ctx) {
    return new PatronNotice()
      .withDeliveryChannel("email")
      .withOutputFormat("text/html")
      .withRecipientId(ctx.getUserId())
      .withTemplateId(ctx.getTemplateId())
      .withContext(buildContext(ctx));
  }

  private static Context buildContext(FeeFineNoticeContext ctx) {
    return new Context()
      .withAdditionalProperty("item", buildItemContext(ctx))
      .withAdditionalProperty("user", buildUserContext(ctx))
      .withAdditionalProperty("feeCharge", buildFeeChargeContext(ctx))
      .withAdditionalProperty("feeAction", buildFeeActionContext(ctx));
  }

  private static JsonObject buildUserContext(FeeFineNoticeContext ctx) {
    final User user = ctx.getUser();
    final JsonObject userContext = new JsonObject();

    if (user == null) {
      return userContext;
    }

    userContext.put(BARCODE, user.getBarcode());

    Personal personal = user.getPersonal();
    if (personal != null) {
      userContext
        .put("firstName", personal.getFirstName())
        .put("lastName", personal.getLastName())
        .put("middleName", personal.getMiddleName());
    }

    return userContext;
  }

  private static JsonObject buildItemContext(FeeFineNoticeContext ctx) {
    final Item item = ctx.getItem();
    final Instance instance = ctx.getInstance();
    final HoldingsRecord holdingsRecord = ctx.getHoldingsRecord();
    final Account account = ctx.getAccount();
    final Location location = ctx.getEffectiveLocation();

    final JsonObject itemContext = new JsonObject();

    if (item != null) {
      itemContext
        .put(BARCODE, item.getBarcode())
        .put("enumeration", item.getEnumeration())
        .put("volume", item.getVolume())
        .put("chronology", item.getChronology())
        .put("yearCaption", String.join(LIST_VALUES_SEPARATOR, item.getYearCaption()))
        .put("copy", getCopyNumber(item, holdingsRecord))
        .put("numberOfPieces", item.getNumberOfPieces())
        .put("descriptionOfPieces", item.getDescriptionOfPieces());

      EffectiveCallNumberComponents callNumberComponents = item.getEffectiveCallNumberComponents();
      if (callNumberComponents != null) {
        itemContext
          .put(CALL_NUMBER, callNumberComponents.getCallNumber())
          .put("callNumberPrefix", callNumberComponents.getPrefix())
          .put("callNumberSuffix", callNumberComponents.getSuffix());
      }
    }

    if (instance != null) {
      itemContext
        .put(TITLE, instance.getTitle())
        .put("primaryContributor", getPrimaryContributor(instance))
        .put("allContributors", getAllContributors(instance));
    }

    if (location != null) {
      itemContext.put(EFFECTIVE_LOCATION_SPECIFIC, location.getName());

      if (location.getLibrary() != null) {
        itemContext.put("effectiveLocationLibrary",
          getNameFromProperties(location.getLibrary().getAdditionalProperties()));
      }

      if (location.getInstitution() != null) {
        itemContext.put("effectiveLocationInstitution",
          getNameFromProperties(location.getInstitution().getAdditionalProperties()));
      }

      if (location.getCampus() != null) {
        itemContext.put("effectiveLocationCampus",
          getNameFromProperties(location.getCampus().getAdditionalProperties()));
      }
    }

    if (account != null) {
      itemContext.put("materialType", account.getMaterialType());

      writeIfDoesNotExist(itemContext, BARCODE, account.getBarcode());
      writeIfDoesNotExist(itemContext, TITLE, account.getTitle());
      writeIfDoesNotExist(itemContext, CALL_NUMBER, account.getCallNumber());
      writeIfDoesNotExist(itemContext, EFFECTIVE_LOCATION_SPECIFIC, account.getLocation());
    }

    return itemContext;
  }

  private static JsonObject buildFeeChargeContext(FeeFineNoticeContext ctx) {
    final Account account = ctx.getAccount();
    final Feefineaction charge = ctx.getCharge();
    final JsonObject feeChargeContext = new JsonObject();

    if (account == null) {
      return feeChargeContext;
    }

    String paymentStatus = account.getPaymentStatus() == null ?
      null : account.getPaymentStatus().getName();

    feeChargeContext
      .put("owner", account.getFeeFineOwner())
      .put("type", account.getFeeFineType())
      .put("paymentStatus", paymentStatus)
      .put("amount", new Money(account.getAmount()).toString())
      .put("remainingAmount", new Money(account.getRemaining()).toString());

    final Metadata metadata = account.getMetadata();
    if (metadata != null) {
      String chargeDate = dateToString(metadata.getCreatedDate());

      feeChargeContext
        .put("chargeDate", chargeDate)
        .put("chargeDateTime", chargeDate);
    }

    if (charge != null) {
      feeChargeContext.put("additionalInfo", getCommentsFromFeeFineAction(charge));
    }

    return feeChargeContext;
  }

  private static JsonObject buildFeeActionContext(FeeFineNoticeContext ctx) {
    final Feefineaction action = ctx.getAction();
    final JsonObject feeActionContext = new JsonObject();

    if (action == null) {
      return (feeActionContext);
    }

    String actionDate = dateToString(action.getDateAction());

    feeActionContext
      .put("type", action.getTypeAction())
      .put("actionDate", actionDate)
      .put("actionDateTime", actionDate)
      .put("amount", new Money(action.getAmountAction()).toString())
      .put("remainingAmount", new Money(action.getBalance()).toString())
      .put("additionalInfo", getCommentsFromFeeFineAction(action));

    return feeActionContext;
  }

  private static String getCommentsFromFeeFineAction(Feefineaction feefineaction){
    String comments = Optional.ofNullable(feefineaction.getComments()).orElse(StringUtils.EMPTY);
    return parseFeeFineComments(comments).getOrDefault(PATRON_COMMENTS_KEY, StringUtils.EMPTY);
  }

  static Map<String, String> parseFeeFineComments(String comments) {
    return Arrays.stream(comments.split(" \n "))
      .map(s -> s.split(" : "))
      .filter(arr -> arr.length == 2)
      .map(strings -> Pair.of(strings[0], strings[1]))
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (s, s2) -> s));
  }

  private static String getPrimaryContributor(Instance instance) {
    return instance.getContributors().stream()
      .filter(contributor -> isTrue(contributor.getPrimary()))
      .map(Contributor::getName)
      .findFirst()
      .orElse(null);
  }

  private static String getAllContributors(Instance instance) {
    return instance.getContributors().stream()
      .map(Contributor::getName)
      .collect(joining(LIST_VALUES_SEPARATOR));
  }

  private static String getCopyNumber(Item item, HoldingsRecord holdingsRecord) {
    String copyNumber = null;
    if (isNotBlank(item.getCopyNumber())) {
      copyNumber = item.getCopyNumber();
    } else if (holdingsRecord != null && isNotBlank(holdingsRecord.getCopyNumber())) {
      copyNumber = holdingsRecord.getCopyNumber();
    }

    return copyNumber;
  }

  private static String getNameFromProperties(Map<String, Object> properties) {
    return (String) Optional.ofNullable(properties.get("name"))
      .filter(name -> name instanceof String)
      .orElse(null);
  }

  private static String dateToString(Date date) {
    return date != null
      ? new DateTime(date, DateTimeZone.UTC).toString()
      : null;
  }

}
