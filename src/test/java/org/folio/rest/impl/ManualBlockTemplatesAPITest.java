package org.folio.rest.impl;

import static io.restassured.http.ContentType.JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.http.HttpStatus;
import org.folio.rest.jaxrs.model.Manualblock;
import org.folio.rest.jaxrs.model.ManualblockTemplate;
import org.folio.rest.jaxrs.model.Manualblocktemplate;
import org.folio.rest.jaxrs.model.ManualblocktemplateCollection;
import org.folio.rest.jaxrs.model.TemplateInfo;
import org.folio.test.support.ApiTests;
import org.folio.test.support.EntityBuilder;
import org.junit.Test;

public class ManualBlockTemplatesAPITest extends ApiTests {

  @Test
  public void testGetPostPutDelete() {
    ManualblockTemplate initialTemplate = EntityBuilder.buildManualBlockTemplate();

    // create template
    ManualblockTemplate createdTemplate = manualBlockTemplatesClient.create(initialTemplate)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .extract()
      .response()
      .as(ManualblockTemplate.class);

    assertTrue(EqualsBuilder
      .reflectionEquals(createdTemplate, initialTemplate, false, null, true, "metadata"));

    ManualblocktemplateCollection createTemplateCollection = manualBlockTemplatesClient.getAll()
      .as(ManualblocktemplateCollection.class);
    assertEquals(1, createTemplateCollection.getTotalRecords().intValue());
    assertTrue(EqualsBuilder
      .reflectionEquals(createTemplateCollection.getManualblockTemplates().get(0), initialTemplate,
        false, null, true, "metadata"));

    ManualblockTemplate initalTemplateChanged = initialTemplate
      .withTemplateInfo(new TemplateInfo().withDesc("CHANGED").withName("CHANGED"));
    manualBlockTemplatesClient
      .update(initialTemplate.getId(), initalTemplateChanged)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    ManualblockTemplate changedTemplate = manualBlockTemplatesClient
      .getById(initialTemplate.getId())
      .as(ManualblockTemplate.class);
    assertTrue(EqualsBuilder
      .reflectionEquals(changedTemplate, initalTemplateChanged,
        false, null, true, "metadata"));

    manualBlockTemplatesClient.delete(initialTemplate.getId())
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    ManualblocktemplateCollection emptyCollection = manualBlockTemplatesClient.getAll()
      .as(ManualblocktemplateCollection.class);
    assertEquals(0, emptyCollection.getTotalRecords().intValue());
  }


}
