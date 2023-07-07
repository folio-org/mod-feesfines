package org.folio.rest.impl;

import static io.restassured.http.ContentType.JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Objects;

import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.ManualBlockTemplate;
import org.folio.rest.jaxrs.model.ManualBlockTemplateCollection;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.junit.jupiter.api.Test;

public class ManualBlockTemplatesAPITest extends ApiTests {

  @Test
  public void testGetPostPutDelete() {
    ManualBlockTemplate initialTemplate = EntityBuilder.buildManualBlockTemplate();

    // create template
    ManualBlockTemplate createdTemplate = manualBlockTemplatesClient.create(initialTemplate)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .extract()
      .response()
      .as(ManualBlockTemplate.class);

    assertTrue(areManualBlockTemplatesEqual(createdTemplate, initialTemplate));

    // get all templates
    ManualBlockTemplateCollection createTemplateCollection = manualBlockTemplatesClient.getAll()
      .as(ManualBlockTemplateCollection.class);
    assertEquals(1, createTemplateCollection.getTotalRecords().intValue());
    assertTrue(areManualBlockTemplatesEqual(
      createTemplateCollection.getManualBlockTemplates().get(0), initialTemplate));

    // update template
    ManualBlockTemplate initalTemplateChanged = initialTemplate
      .withName("CHANGED").withDesc("CHANGED");
    manualBlockTemplatesClient
      .update(initialTemplate.getId(), initalTemplateChanged)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    // get template by id
    ManualBlockTemplate changedTemplate = manualBlockTemplatesClient
      .getById(initialTemplate.getId())
      .as(ManualBlockTemplate.class);
    assertTrue(areManualBlockTemplatesEqual(changedTemplate, initalTemplateChanged));

    // delete template
    manualBlockTemplatesClient.delete(initialTemplate.getId())
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    ManualBlockTemplateCollection emptyCollection = manualBlockTemplatesClient.getAll()
      .as(ManualBlockTemplateCollection.class);
    assertEquals(0, emptyCollection.getTotalRecords().intValue());
  }

  private boolean areManualBlockTemplatesEqual(ManualBlockTemplate createdTemplate,
    ManualBlockTemplate initialTemplate) {

    var createdBlockTemplate = createdTemplate.getBlockTemplate();
    var initialBlockTemplate = initialTemplate.getBlockTemplate();

    return Objects.equals(createdTemplate.getName(), initialTemplate.getName())
      && Objects.equals(createdTemplate.getCode(), initialTemplate.getCode())
      && Objects.equals(createdTemplate.getDesc(), initialTemplate.getDesc())
      && Objects.equals(createdTemplate.getId(), initialTemplate.getId())
      && Objects.equals(createdBlockTemplate.getDesc(), initialBlockTemplate.getDesc())
      && Objects.equals(createdBlockTemplate.getPatronMessage(), initialBlockTemplate.getPatronMessage())
      && Objects.equals(createdBlockTemplate.getBorrowing(), initialBlockTemplate.getBorrowing())
      && Objects.equals(createdBlockTemplate.getRenewals(), initialBlockTemplate.getRenewals())
      && Objects.equals(createdBlockTemplate.getRequests(), initialBlockTemplate.getRequests());
  }
}
