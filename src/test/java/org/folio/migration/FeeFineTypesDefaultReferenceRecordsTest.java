package org.folio.migration;

import static org.folio.test.support.matcher.FeeFineMatchers.hasAllAutomaticFeeFineTypes;
import static org.folio.util.PomUtils.getModuleVersion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.AutomaticFeeFineType;
import org.folio.rest.persist.PostgresClient;
import org.folio.test.support.ApiTests;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;

public class FeeFineTypesDefaultReferenceRecordsTest extends ApiTests {
  private static final String MIGRATION_SCRIPT = loadMigrationScript();

  @Test
  public void lostItemFeeForActualCostIsAddedWhenMigratingFrom15_9To15_10() {
    // module was enabled in @BeforeAll with moduleTo=current_version
    // we must downgrade to 15.9.0 first if we want to rerun the migration script (see RMB-937)
    createTenant(getModuleVersion(), "15.9.0");

    // use SQL to delete, API refuses deleting automatic type
    var deleted = get(PostgresClient.getInstance(vertx, TENANT_NAME)
        .delete(FEEFINES_TABLE, AutomaticFeeFineType.LOST_FEE_FOR_ACTUAL_COST.getId()));
    assertThat(deleted.rowCount(), is(1));

    createTenant("15.9.0", "15.10.0");

    feeFinesClient.getAll().then()
      .body(hasAllAutomaticFeeFineTypes());
  }

  private static void createTenant(String moduleFromVersion, String moduleToVersion) {
    final var tenantAttributes = getTenantAttributes()
      .withModuleFrom(MODULE_NAME + "-" + moduleFromVersion)
      .withModuleTo(MODULE_NAME + "-" + moduleToVersion);

    CompletableFuture<Response> future = new CompletableFuture<>();
    createTenant(tenantAttributes, future);

    Response postTenantResponse = get(future);
    assertThat(postTenantResponse.getStatus(), is(HttpStatus.SC_NO_CONTENT));
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
