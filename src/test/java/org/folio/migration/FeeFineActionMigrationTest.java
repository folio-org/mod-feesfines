package org.folio.migration;

import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.util.PomUtils.getModuleId;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Date;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FeeFineActionMigrationTest extends ApiTests {

  private static final String CREATED_AT_KEY = "createdAt";
  private static final String ORIGINAL_CREATED_AT_KEY = "originalCreatedAt";
  private static final String FEE_FINE_ACTIONS_TABLE = "feefineactions";
  private static final String OLDER_VERSION = "mod-feesfines-18.2.0";
  private static final String MIGRATION_VERSION = "mod-feesfines-18.3.0";

  private final ResourceClient feeFineActionsClient = buildFeeFineActionsClient();

  @BeforeEach
  void beforeEach() {
    // Module was enabled in @BeforeAll with 'module_to' equal to current module version from POM.
    // We must downgrade to lower version first if we want to rerun the migration script (see RMB-937)
    postTenant(getModuleId(), OLDER_VERSION);
  }

  @AfterEach
  void afterEach() {
    removeAllFromTable(FEE_FINE_ACTIONS_TABLE);
  }

  @Test
  void actionIsNotUpdatedWhenWhenItDoesNotHaveCreatedAt() {
    String actionId = createAction(null);
    runMigration();
    verifyKeyValue(actionId, CREATED_AT_KEY, nullValue());
    verifyKeyValue(actionId, ORIGINAL_CREATED_AT_KEY, nullValue());
  }

  @Test
  void actionIsNotUpdatedWhenItAlreadyContainsValidCreatedAtValue() {
    String uuid = randomId();
    String actionId = createAction(uuid);
    runMigration();
    verifyKeyValue(actionId, CREATED_AT_KEY, uuid);
    verifyKeyValue(actionId, ORIGINAL_CREATED_AT_KEY, nullValue());
  }

  @Test
  void actionIsUpdatedWhenCreatedAtContainsNonUuidValue() {
    String createdAt = "not-a-uuid";
    String actionId = createAction(createdAt);
    runMigration();
    verifyKeyValue(actionId, CREATED_AT_KEY, nullValue());
    verifyKeyValue(actionId, ORIGINAL_CREATED_AT_KEY, createdAt);
  }

  private static void runMigration() {
    postTenant(OLDER_VERSION, MIGRATION_VERSION);
  }
  
  private String createAction(String createdAt) {
    Feefineaction action = buildAction(createdAt);
    get(pgClient.save(FEE_FINE_ACTIONS_TABLE, action.getId(), action));
    
    return action.getId();
  }

  private static Feefineaction buildAction(String createdAt) {
    return new Feefineaction()
      .withId(randomId())
      .withTypeAction("Test action type")
      .withAmountAction(new MonetaryValue(1.0))
      .withUserId(randomId())
      .withDateAction(new Date())
      .withAccountId(randomId())
      .withBalance(new MonetaryValue(1.0))
      .withPaymentMethod("Cash")
      .withSource("test")
      .withNotify(false)
      .withTransactionInformation("-")
      .withCreatedAt(createdAt);
  }

  private void verifyKeyValue(String actionId, String key, String value) {
    verifyKeyValue(actionId, key, is(value));
  }

  private void verifyKeyValue(String actionId, String key, Matcher<Object> valueMatcher) {
    feeFineActionsClient.getById(actionId)
      .then()
      .body(key, valueMatcher);
  }

}
