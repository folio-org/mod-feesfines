package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.http.ContentType.JSON;
import static io.vertx.core.json.Json.decodeValue;
import static org.folio.rest.service.LogEventPublisher.LOG_EVENT_TYPE;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.MANUAL_BLOCK_CREATED;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.MANUAL_BLOCK_DELETED;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.MANUAL_BLOCK_MODIFIED;
import static org.folio.rest.service.LogEventPublisher.PAYLOAD;
import static org.folio.test.support.EntityBuilder.buildManualBlock;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.folio.rest.domain.EventType;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.folio.rest.jaxrs.model.Manualblock;
import org.folio.rest.service.LogEventPublisher;
import org.folio.test.support.ApiTests;
import org.folio.util.pubsub.PubSubClientUtils;
import org.junit.Test;

import com.github.tomakehurst.wiremock.verification.FindRequestsResult;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

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

    assertTrue(EqualsBuilder.reflectionEquals(createdManualBlock, initialManualBlock, Collections.singletonList("metadata")));

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

  private Event getLastLogEvent() {
    return getLastPublishedEventOfType(EventType.LOG_RECORD.toString());
  }

  private Event getLastPublishedEventOfType(String eventType) {
    final FindRequestsResult requests = getOkapi()
      .findRequestsMatching(postRequestedFor(urlPathMatching("/pubsub/publish")).build());

    return requests.getRequests()
      .stream()
      .filter(request -> StringUtils.isNotBlank(request.getBodyAsString()))
      .filter(request -> decodeValue(request.getBodyAsString(), Event.class).getEventType()
        .equals(eventType))
      .max(Comparator.comparing(LoggedRequest::getLoggedDate))
      .map(LoggedRequest::getBodyAsString)
      .map(JsonObject::new)
      .map(json -> json.mapTo(Event.class))
      .orElse(null);
  }

  private void assertManualBlockLogEventPublished(Manualblock manualBlockExpected, LogEventPublisher.LogEventPayloadType payloadType) {
    Awaitility.await()
      .atMost(5, TimeUnit.SECONDS)
      .until(() -> getLastLogEvent() != null);

    final Event event = getLastLogEvent();
    assertThat(event, notNullValue());

    EventMetadata eventMetadata = event.getEventMetadata();

    assertEquals(EventType.LOG_RECORD.name(), event.getEventType());
    assertEquals(PubSubClientUtils.constructModuleName(), eventMetadata.getPublishedBy());
    assertEquals(TENANT_NAME, eventMetadata.getTenantId());
    assertEquals(1, eventMetadata.getEventTTL()
      .intValue());

    final JsonObject eventPayload = new JsonObject(event.getEventPayload());

    Manualblock manualBlockActual = eventPayload.getJsonObject(PAYLOAD)
      .mapTo(Manualblock.class);

    assertThat(payloadType.value(), equalTo(eventPayload.getString(LOG_EVENT_TYPE)));
    assertTrue(EqualsBuilder.reflectionEquals(manualBlockActual, manualBlockExpected, Collections.singletonList("metadata")));
  }
}
