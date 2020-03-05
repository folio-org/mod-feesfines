package org.folio.rest.utils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.persist.PostgresClient;

import static java.util.Arrays.asList;

public class AutomaticFeeFineInitializer {

  private static final String FEEFINES_TABLE = "feefines";
  private final PostgresClient postgresClient;
  private final List<String> feefines;

  public AutomaticFeeFineInitializer(PostgresClient postgresClient) {
    this.postgresClient = postgresClient;
    this.feefines = asList("Overdue fine", "Lost item fee", "Lost item processing fee",
      "Replacement processing fee");
  }

  public void initAutomaticFeefines() {
    feefines.forEach(this::saveAutomaticFeeFine);
  }

  private void saveAutomaticFeeFine(String feeFineType) {
    postgresClient.save(FEEFINES_TABLE, createNewAutomaticFeeFine(feeFineType), null);
  }

  private Feefine createNewAutomaticFeeFine(String feeFineType) {
    Feefine feeFine = new Feefine();
    feeFine.setFeeFineType(feeFineType);
    feeFine.setAutomatic(true);
    feeFine.setId(UUID.randomUUID().toString());
    feeFine.setMetadata(createMetadata());
    return feeFine;
  }

  private Metadata createMetadata() {
    Metadata metadata = new Metadata();
    metadata.setCreatedDate(new Date());
    return metadata;
  }
}
