package org.folio.rest.domain;

import java.util.HashMap;
import java.util.Map;

public enum AutomaticFeeFineType {
  // IDs come from /resources/templates/db_scripts/populate-feefines.sql

  OVERDUE_FINE("9523cb96-e752-40c2-89da-60f3961a488d"),
  REPLACEMENT_PROCESSING_FEE("d20df2fb-45fd-4184-b238-0d25747ffdd9"),
  LOST_ITEM_FEE("cf238f9f-7018-47b7-b815-bb2db798e19f"),
  LOST_ITEM_ACTUAL_COST_FEE("73785370-d3bd-4d92-942d-ae2268e02ded"),
  LOST_ITEM_PROCESSING_FEE("c7dede15-aa48-45ed-860b-f996540180e0");

  private final String id;
  private static final Map<String, AutomaticFeeFineType> idIndex = new HashMap<>(AutomaticFeeFineType.values().length);

  static {
    for (AutomaticFeeFineType automaticFeeFineType : AutomaticFeeFineType.values()) {
      idIndex.put(automaticFeeFineType.id, automaticFeeFineType);
    }
  }

  AutomaticFeeFineType(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public static AutomaticFeeFineType getById(String id) {
    return idIndex.get(id);
  }
}
