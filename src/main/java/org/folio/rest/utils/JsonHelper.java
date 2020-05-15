package org.folio.rest.utils;

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
}
