package org.folio.rest.utils;

import static org.apache.commons.lang.StringUtils.defaultString;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.toMap;
import static org.folio.rest.domain.Action.TRANSFER;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Feefineaction;

public class FeeFineActionHelper {
  public static final String PATRON_COMMENTS_KEY = "PATRON";
  public static final String STAFF_COMMENTS_KEY = "STAFF";

    private FeeFineActionHelper() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  public static boolean isCharge(Feefineaction action) {
    return !isAction(action) && action.getPaymentMethod() == null;
  }

  public static boolean isAction(Feefineaction action) {
    return Action.isActionResult(action.getTypeAction());
  }

  public static boolean isActionOfType(Feefineaction feefineaction, Action... actions) {
    return Arrays.stream(actions)
      .anyMatch(action -> action.isActionForResult(feefineaction.getTypeAction()));
  }

  public static String getStaffInfoFromComment(Feefineaction action) {
    return defaultString(parseFeeFineComments(action.getComments()).get(STAFF_COMMENTS_KEY));
  }

  public static String getPatronInfoFromComment(Feefineaction action) {
    return defaultString(parseFeeFineComments(action.getComments()).get(PATRON_COMMENTS_KEY));
  }

  public static Map<String, String> parseFeeFineComments(String comments) {
    return Arrays.stream(defaultString(comments).split(" \n "))
      .map(s -> s.split(" : "))
      .filter(arr -> arr.length == 2)
      .map(strings -> Pair.of(strings[0], strings[1]))
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (s, s2) -> s));
  }

  public static Map<String, List<Feefineaction>> groupFeeFineActionsByAccountId(
    List<Feefineaction> refundableFeeFineActions) {

    return refundableFeeFineActions.stream()
      .collect(groupingBy(Feefineaction::getAccountId));
  }

  public static Map<String, MonetaryValue> groupTransferredAmountsByTransferAccount(
    Collection<Feefineaction> feeFineActions) {

    return feeFineActions.stream()
      .filter(actionPredicate(TRANSFER))
      .collect(groupingBy(
        Feefineaction::getPaymentMethod,
        collectingAndThen(
          summingDouble(Feefineaction::getAmountAction),
          MonetaryValue::new
        )));
  }

  public static MonetaryValue getTotalAmount(Collection<Feefineaction> feeFineActions) {
    return getTotalAmount(feeFineActions, ffa -> true);
  }

  public static MonetaryValue getTotalAmount(Collection<Feefineaction> feeFineActions,
    Action action) {

    return getTotalAmount(feeFineActions, actionPredicate(action));
  }

  public static MonetaryValue getTotalAmount(Collection<Feefineaction> feeFineActions,
    Predicate<Feefineaction> filter) {

    return feeFineActions.stream()
      .filter(filter)
      .collect(
        collectingAndThen(
          summingDouble(Feefineaction::getAmountAction),
          MonetaryValue::new
        ));
  }

  public static <K> Map<K, MonetaryValue> getTotalAmounts(
    Map<K, List<Feefineaction>> feeFineActions) {

    return feeFineActions.entrySet()
      .stream()
      .collect(toMap(
        Map.Entry::getKey,
        entry -> getTotalAmount(entry.getValue())
      ));
  }

  public static Predicate<Feefineaction> actionPredicate(Action action) {
    return ffa -> action.isActionForResult(ffa.getTypeAction());
  }
}
