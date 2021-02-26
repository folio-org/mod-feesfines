package org.folio.migration;

import static org.folio.rest.utils.ResourceClients.tenantClient;
import static org.folio.test.support.matcher.FeeFineMatchers.hasAllAutomaticFeeFineTypes;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.PomReader;
import org.folio.test.support.ApiTests;
import org.junit.Test;

import io.vertx.core.Vertx;

public class FeeFineTypesDefaultReferenceRecordsTest extends ApiTests {
  private static final String LOST_FEE_FOR_ACTUAL_COST_ID = "73785370-d3bd-4d92-942d-ae2268e02ded";
  private static final String MIGRATION_SCRIPT = loadMigrationScript();

  @Test
  public void lostItemFeeForActualCostIsAddedWhenMigratingFrom15_9To15_10() {
    feeFinesClient.delete(LOST_FEE_FOR_ACTUAL_COST_ID);

    final var tenantAttributes = getTenantAttributes()
      .withModuleFrom(MODULE_NAME + "-15.9.0")
      .withModuleTo(MODULE_NAME + "-" + PomReader.INSTANCE.getVersion());

    createTenant(tenantAttributes);

    feeFinesClient.getAll().then()
      .body(hasAllAutomaticFeeFineTypes());
  }

  @Test
  public void subsequentRunOfMigrationDoesNotCauseIssues() {
    executeMigration();
    executeMigration();

    feeFinesClient.getAll().then()
      .body(hasAllAutomaticFeeFineTypes());
  }

  private static String loadMigrationScript() {
    try (final var resourceAsStream = FeeFineTypesDefaultReferenceRecordsTest.class
      .getResourceAsStream("/templates/db_scripts/" +
        "add-lost-fee-for-actual-cost.sql")) {

      return new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8)
        .replaceAll("\\$\\{myuniversity}", TENANT_NAME)
        .replaceAll("\\$\\{mymodule}", MODULE_NAME.replace("-", "_"));

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private void executeMigration() {
    final var future = new CompletableFuture<Void>();

    PostgresClient.getInstance(Vertx.vertx(), TENANT_NAME)
      .execute(MIGRATION_SCRIPT, result -> {
        if (result.succeeded()) {
          future.complete(null);
        } else {
          future.completeExceptionally(result.cause());
        }
      });

    get(future);
  }
}
