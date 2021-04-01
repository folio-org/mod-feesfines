package org.folio.rest.utils;

import static java.util.stream.Collectors.toMap;

import java.util.LinkedHashMap;
import java.util.Map;

public class CollectionUtils {

  private CollectionUtils() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  public static <K, V extends Comparable<V>> Map<K, V> sortByValue(Map<K, V> map) {
    return map.entrySet()
      .stream()
      .sorted(Map.Entry.comparingByValue())
      .collect(toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (e1, e2) -> e1,
        LinkedHashMap::new
      ));
  }
}
