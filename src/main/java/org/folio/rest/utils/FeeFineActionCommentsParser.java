package org.folio.rest.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

public class FeeFineActionCommentsParser {

  private FeeFineActionCommentsParser() {
  }

  public static Map<String, String> parseFeeFineComments(String comments) {
    return Arrays.stream(comments.split(" \n "))
      .map(s -> s.split(" : "))
      .filter(arr -> arr.length == 2)
      .map(strings -> Pair.of(strings[0], strings[1]))
      .collect(Collectors.toMap(Pair::getKey, Pair::getValue, (s, s2) -> s));
  }
}
