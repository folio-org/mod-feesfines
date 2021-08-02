package org.folio.rest.service.report.utils;

import static java.math.BigDecimal.ZERO;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.ReportTotalsEntry;

public class ReportStatsHelper {
  public static void calculateTotals(List<ReportTotalsEntry> totalsEntries, List<Feefineaction> actions,
    Function<Feefineaction, String> categoryNameFunction, String totalsCategoryName) {

    List<String> categories = actions.stream()
      .map(categoryNameFunction)
      .filter(Objects::nonNull)
      .filter(category -> !category.isEmpty())
      .distinct()
      .collect(Collectors.toList());

    // Calculate categories
    categories.forEach(category -> totalsEntries.add(new ReportTotalsEntry()
      .withName(category)
      .withTotalAmount(actions.stream()
        .filter(filterByCategory(category, categoryNameFunction))
        .map(Feefineaction::getAmountAction)
        .filter(Objects::nonNull)
        .map(MonetaryValue::new)
        .reduce(MonetaryValue::add)
        .orElse(new MonetaryValue(ZERO))
        .toString())
      .withTotalCount(String.valueOf(actions.stream()
        .filter(filterByCategory(category, categoryNameFunction))
        .count()))));

    // Calculate total
    totalsEntries.add(new ReportTotalsEntry()
      .withName(totalsCategoryName)
      .withTotalAmount(actions.stream()
        .filter(filterByCategories(categories, categoryNameFunction))
        .map(Feefineaction::getAmountAction)
        .filter(Objects::nonNull)
        .map(MonetaryValue::new)
        .reduce(MonetaryValue::add)
        .orElse(new MonetaryValue(ZERO))
        .toString())
      .withTotalCount(String.valueOf(actions.stream()
        .filter(filterByCategories(categories, categoryNameFunction))
        .count())));
  }

  private static Predicate<Feefineaction> filterByCategory(String category,
    Function<Feefineaction, String> categoryNameFunction) {

    return action -> category.equals(categoryNameFunction.apply(action));
  }

  private static Predicate<Feefineaction> filterByCategories(List<String> categories,
    Function<Feefineaction, String> categoryNameFunction) {

    return action -> categories.contains(categoryNameFunction.apply(action));
  }
}
