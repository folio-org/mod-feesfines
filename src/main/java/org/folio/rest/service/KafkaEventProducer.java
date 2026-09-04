package org.folio.rest.service;

import static java.util.Objects.requireNonNull;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import com.fasterxml.jackson.databind.util.RawValue;
import org.folio.kafka.KafkaConfig;
import org.folio.kafka.SimpleKafkaProducerManager;
import org.folio.kafka.services.KafkaEnvironmentProperties;
import org.folio.kafka.services.KafkaProducerRecordBuilder;
import org.folio.rest.domain.EventType;
import org.folio.rest.domain.FeeFineKafkaTopic;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

public class KafkaEventProducer {
  private static final String PRODUCER_NAME = "mod-feesfines-events";

  private final Function<KafkaProducerRecord<String, String>, Future<Void>> sender;

  public KafkaEventProducer(Vertx vertx) {
    this(createSender(vertx));
  }

  KafkaEventProducer(Function<KafkaProducerRecord<String, String>, Future<Void>> sender) {
    this.sender = requireNonNull(sender);
  }

  public Future<Void> publish(EventType eventType, String payload, Map<String, String> okapiHeaders) {
    return sender.apply(createRecord(eventType, payload, okapiHeaders));
  }

  private KafkaProducerRecord<String, String> createRecord(EventType eventType, String payload,
    Map<String, String> okapiHeaders) {

    String tenantId = tenantId(okapiHeaders);
    String kafkaTopic = FeeFineKafkaTopic.from(eventType).fullTopicName(tenantId);

    return new KafkaProducerRecordBuilder<String, Object>(tenantId)
      .key(UUID.randomUUID().toString())
      .value(new RawValue(payload))
      .topic(kafkaTopic)
      .propagateOkapiHeaders(okapiHeaders)
      .build();
  }

  private static Function<KafkaProducerRecord<String, String>, Future<Void>> createSender(Vertx vertx) {
    KafkaConfig kafkaConfig = KafkaConfig.builder()
      .kafkaHost(KafkaEnvironmentProperties.host())
      .kafkaPort(KafkaEnvironmentProperties.port())
      .envId(KafkaEnvironmentProperties.environment())
      .replicationFactor(KafkaEnvironmentProperties.replicationFactor())
      .build();

    KafkaProducer<String, String> producer = new SimpleKafkaProducerManager(vertx, kafkaConfig)
      .createShared(PRODUCER_NAME);

    return producerRecord -> producer.send(producerRecord).mapEmpty();
  }
}
