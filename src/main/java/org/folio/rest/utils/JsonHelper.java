package org.folio.rest.utils;

import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import io.vertx.core.json.JsonObject;

public class JsonHelper {

  private JsonHelper() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  public static void write(JsonObject target, String property, String value) {
    if (value != null) {
      target.put(property, value);
    }
  }

  public static void write(JsonObject target, String property, Double value) {
    if (value != null) {
      target.put(property, value);
    }
  }

  public static void writeIfNotBlank(JsonObject target, String key, String value) {
    if (argumentsAreValid(target, key, value)) {
      target.put(key, value);
    }
  }

  public static void writeIfDoesNotExist(JsonObject target, String key, String value) {
    if (!target.containsKey(key)) {
      writeIfNotBlank(target, key, value);
    }
  }

  private static boolean argumentsAreValid(JsonObject target, String key, String value) {
    return target != null && isNoneBlank(key, value);
  }
}
