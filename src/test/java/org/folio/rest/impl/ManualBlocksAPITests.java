package org.folio.rest.impl;

import static io.restassured.http.ContentType.JSON;
import static org.folio.rest.service.LogEventPublisher.LOG_EVENT_TYPE;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.MANUAL_BLOCK_CREATED;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.MANUAL_BLOCK_DELETED;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.MANUAL_BLOCK_MODIFIED;
import static org.folio.rest.service.LogEventPublisher.PAYLOAD;
import static org.folio.test.support.EntityBuilder.buildManualBlock;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.FeeFineKafkaTopic;
import org.folio.rest.jaxrs.model.Manualblock;
import org.folio.rest.service.LogEventPublisher;
import org.folio.test.support.ApiTests;
import org.folio.test.support.KafkaTestHelper;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class ManualBlocksAPITests extends ApiTests {

  @Test
  public void testAllMethodsAndEventPublishing() {
    Manualblock initialManualBlock = buildManualBlock();
    String manualBlockId = initialManualBlock.getId();

    // create manual block
    Manualblock createdManualBlock = manualBlocksClient.create(initialManualBlock)
      .then()
      .statusCode(HttpStatus.SC_CREATED)
      .contentType(JSON)
      .extract()
      .response()
      .as(Manualblock.class);

    assertManualBlockLogEventPublished(initialManualBlock, MANUAL_BLOCK_CREATED);

    Manualblock updatedManualBlock = initialManualBlock.withType("Type");

    // put manual block
    manualBlocksClient.update(manualBlockId, updatedManualBlock)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    assertManualBlockLogEventPublished(updatedManualBlock, MANUAL_BLOCK_MODIFIED);

    // delete manual block
    manualBlocksClient.delete(manualBlockId)
      .then()
      .statusCode(HttpStatus.SC_NO_CONTENT);

    assertManualBlockLogEventPublished(updatedManualBlock, MANUAL_BLOCK_DELETED);

  }

  /** Returns the last LOG_RECORD Kafka message payload published since testStartTime, or null. */
  private JsonObject getLastLogEvent() {
    String topic = FeeFineKafkaTopic.LOG_RECORD_TOPIC.fullTopicName(TENANT_NAME);
    List<String> messages = KafkaTestHelper.getInstance().pollMessages(topic, testStartTime);
    if (messages.isEmpty()) {
      return null;
    }
    return new JsonObject(messages.get(messages.size() - 1));
  }

  private void assertManualBlockLogEventPublished(Manualblock manualBlockExpected,
    LogEventPublisher.LogEventPayloadType payloadType) {

    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .until(() -> getLastLogEvent() != null);

    final JsonObject eventPayload = getLastLogEvent();
    assertThat(eventPayload, notNullValue());

    assertThat(payloadType.value(), equalTo(eventPayload.getString(LOG_EVENT_TYPE)));

    Manualblock manualBlockActual = eventPayload.getJsonObject(PAYLOAD)
      .mapTo(Manualblock.class);

    assertTrue(EqualsBuilder.reflectionEquals(manualBlockActual, manualBlockExpected,
      Collections.singletonList("metadata")));

    // special case for x-okapi-user-id header
    if (payloadType == MANUAL_BLOCK_DELETED) {
      assertNotNull(manualBlockActual.getMetadata().getUpdatedByUserId());
    }
  }
}
