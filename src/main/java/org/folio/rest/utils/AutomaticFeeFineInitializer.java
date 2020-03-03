package org.folio.rest.utils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import static java.util.Arrays.asList;

public class AutomaticFeeFineInitializer {

  private static final String FEEFINES_TABLE_NAME = "feefines";
  private PostgresClient postgresClient;
  private Handler<AsyncResult<String>> replyHandler;
  private List<String> feefines;

  public AutomaticFeeFineInitializer(PostgresClient postgresClient) {
    this.postgresClient = postgresClient;
    this.feefines = asList("Overdue fine", "Lost item fee", "Lost item processing fee", "Replacement processing fee");
  }

  public void initAutomaticFeefines() {
    feefines.forEach(this::saveAutomaticFeeFine);
  }

  private void saveAutomaticFeeFine(String feeFineType) {
    postgresClient.save(FEEFINES_TABLE_NAME, generateNewAutomaticFeeFine(feeFineType), replyHandler);
  }

  private Feefine generateNewAutomaticFeeFine(String feeFineType) {
    Feefine feeFine = new Feefine();
    feeFine.setFeeFineType(feeFineType);
    feeFine.setAutomatic(true);
    feeFine.setId(UUID.randomUUID().toString());
    feeFine.setMetadata(generateMetadata());
    return feeFine;
  }

  private Metadata generateMetadata() {
    Metadata metadata = new Metadata();
    metadata.setCreatedDate(new Date());
    return metadata;
  }

  public void setPostgresClient(PostgresClient postgresClient) {
    this.postgresClient = postgresClient;
  }

  public void setReplyHandler(Handler<AsyncResult<String>> replyHandler) {
    this.replyHandler = replyHandler;
  }

  public PostgresClient getPostgresClient() {
    return postgresClient;
  }
}
