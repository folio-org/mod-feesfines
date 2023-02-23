package org.folio.rest.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.okapi.common.ModuleId;
import org.folio.okapi.common.SemVer;
import org.folio.rest.jaxrs.model.TenantAttributes;

public class MigrationHelper {

  private static final Logger log = LogManager.getLogger(MigrationHelper.class);

  private MigrationHelper() {
    throw new UnsupportedOperationException("Utility class, do not instantiate");
  }

  public static boolean shouldSkipMigration(TenantAttributes tenantAttributes,
    String moduleVersionForMigration) {

    final String moduleFrom = tenantAttributes.getModuleFrom();
    final String moduleTo = tenantAttributes.getModuleTo();

    log.info("shouldSkipMigration:: moduleFrom={}, moduleTo={}", moduleFrom, moduleTo);

    if (moduleFrom == null) {
      log.info("shouldSkipMigration:: skipping migration as moduleFrom is null");
      return true;
    }

    if (moduleTo == null) {
      log.info("shouldSkipMigration:: skipping migration as moduleTo is null");
      return true;
    }

    SemVer moduleFromVersion = moduleVersionToSemVer(moduleFrom);
    SemVer moduleToVersion = moduleVersionToSemVer(moduleTo);
    SemVer migrationVersion = moduleVersionToSemVer(moduleVersionForMigration);

    if (moduleToVersion.compareTo(migrationVersion) < 0) {
      log.info("shouldSkipMigration:: skipping migration for version {}: should be {} or higher",
        moduleToVersion, migrationVersion);
      return true;
    }

    if (moduleFromVersion.compareTo(migrationVersion) >= 0) {
      log.info("shouldSkipMigration:: skipping migration for version {}: previous version {} is " +
          "already migrated",
        moduleToVersion, moduleFromVersion);
      return true;
    }

    return false;
  }

  private static SemVer moduleVersionToSemVer(String version) {
    try {
      return new SemVer(version);
    } catch (IllegalArgumentException ex) {
      return new ModuleId(version).getSemVer();
    }
  }
}
