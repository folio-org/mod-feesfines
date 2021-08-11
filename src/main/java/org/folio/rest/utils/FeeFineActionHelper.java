package org.folio.rest.utils;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.folio.rest.domain.Action.TRANSFER;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        reducing(MonetaryValue.ZERO, Feefineaction::getAmountAction, MonetaryValue::add)
      ));
  }

  public static MonetaryValue getTotalAmount(Collection<Feefineaction> feeFineActions) {
    return getTotalAmount(feeFineActions, ffa -> true);
  }

  public static MonetaryValue getTotalAmount(Collection<Feefineaction> feeFineActions,
    Action action) {

    return getTotalAmount(feeFineActions, actionPredicate(action));
  }

  public static MonetaryValue getTotalAmount(Collection<Feefineaction> feeFineActions,
    Collection<Action> actions) {

    return getTotalAmount(feeFineActions, actionsPredicate(actions));
  }

  public static MonetaryValue getTotalAmount(Collection<Feefineaction> feeFineActions,
    Predicate<Feefineaction> filter) {

    return feeFineActions.stream()
      .filter(filter)
      .map(Feefineaction::getAmountAction)
      .reduce(MonetaryValue.ZERO, MonetaryValue::add);
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
    return actionsPredicate(List.of(action));
  }

  public static Predicate<Feefineaction> actionsPredicate(Collection<Action> actions) {
    return ffa -> actions.stream()
      .anyMatch(action -> action.isActionForResult(ffa.getTypeAction()));
  }

}
