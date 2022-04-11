package org.folio.rest.impl;

import org.apache.http.HttpStatus;
import org.folio.rest.domain.AutomaticFeeFineType;
import org.folio.test.support.ApiTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.vertx.core.json.JsonObject;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class FeeFinesAPITest extends ApiTests {
  private static final String REST_PATH = "/feefines";
  private static final String FEEFINES_TABLE = "feefines";
  private static final String OWNERS_TABLE = "owners";

  @Before
  public void setUp() {
    removeAllFromTable(FEEFINES_TABLE);
    removeAllFromTable(OWNERS_TABLE);
  }

  @Test
  public void canCreateNewFeefine() {
    String ownerId = randomId();
    createOwner(ownerId, "test_owner");
    String entity = createFeefineJson(randomId(), "book lost", ownerId);

    post(entity)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  @Test
  public void cannotCreateFeefineWithDuplicateId() {
    String ownerId1 = randomId();
    String ownerId2 = randomId();

    createOwner(ownerId1, "test_owner_1");
    createOwner(ownerId2, "test_owner_2");

    String feefineId = randomId();

    String entity1 = createFeefineJson(feefineId, "type_1", ownerId1);
    String entity2 = createFeefineJson(feefineId, "type_2", ownerId2);

    post(entity1)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    post(entity2)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void cannotCreateDuplicateFeefineTypeForSameOwner() {
    String ownerId = randomId();
    createOwner(ownerId, "test_owner");

    String entity1 = createFeefineJson(randomId(), "book lost", ownerId);
    String entity2 = createFeefineJson(randomId(), "book lost", ownerId);

    post(entity1)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    post(entity2)
      .then()
      .statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  public void canCreateDuplicateFeefineTypeForDifferentOwners() {
    String ownerId1 = randomId();
    String ownerId2 = randomId();

    createOwner(ownerId1, "test_owner_1");
    createOwner(ownerId2, "test_owner_2");

    String entity1 = createFeefineJson(randomId(), "book lost", ownerId1);
    String entity2 = createFeefineJson(randomId(), "book lost", ownerId2);

    post(entity1)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);

    post(entity2)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(ContentType.JSON);
  }

  @Test
  @Parameters(source = AutomaticFeeFineType.class)
  public void cannotDeleteAutomaticFeeFineType(AutomaticFeeFineType automaticFeeFineType) {
    client.delete(REST_PATH + "/" + automaticFeeFineType.getId())
      .then()
      .statusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY)
      .contentType(ContentType.JSON);
  }

  private String createFeefineJson(String id, String type, String ownerId) {
    return new JsonObject()
      .put("id", id)
      .put("automatic", false)
      .put("feeFineType", type)
      .put("defaultAmount", "1.00")
      .put("chargeNoticeId", randomId())
      .put("actionNoticeId", randomId())
      .put("ownerId", ownerId)
      .encodePrettily();
  }

  private Response post(String entity) {
    return client.post(REST_PATH, entity);
  }

  private void createOwner(String id, String name) {
    String ownerJson = new JsonObject()
      .put("id", id)
      .put("owner", name)
      .put("defaultChargeNoticeId", randomId())
      .put("defaultActionNoticeId", randomId())
      .encodePrettily();

    client.post("/owners", ownerJson)
      .then()
      .statusCode(HttpStatus.SC_CREATED);
  }
}
