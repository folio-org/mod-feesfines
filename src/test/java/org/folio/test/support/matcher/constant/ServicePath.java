package org.folio.test.support.matcher.constant;

public class ServicePath {
  public static final String ACTIONS_PATH = "/feefineactions";
  public static final String OWNERS_PATH = "/owners";
  public static final String ACCOUNTS_PATH = "/accounts";
  public static final String FEEFINES_PATH = "/feefines";
  public static final String ITEMS_PATH = "/item-storage/items";
  public static final String HOLDINGS_PATH = "/holdings-storage/holdings";
  public static final String INSTANCES_PATH = "/instance-storage/instances";
  public static final String LOCATIONS_PATH = "/locations";
  public static final String INSTITUTIONS_PATH = "/location-units/institutions";
  public static final String CAMPUSES_PATH = "/location-units/campuses";
  public static final String LIBRARIES_PATH = "/location-units/libraries";
  public static final String USERS_PATH = "/users";
  public static final String USERS_GROUPS_PATH = "/groups";

  private ServicePath() {
    throw new UnsupportedOperationException("Do not instantiate");
  }
}
