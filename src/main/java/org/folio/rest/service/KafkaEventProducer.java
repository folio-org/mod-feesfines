package org.folio.rest.service;

import static java.util.Objects.requireNonNull;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Map;
import java.util.function.Function;

import org.folio.kafka.KafkaConfig;
import org.folio.kafka.SimpleKafkaProducerManager;
import org.folio.kafka.headers.FolioKafkaHeaders;
import org.folio.kafka.services.KafkaEnvironmentProperties;
import org.folio.rest.domain.EventType;
import org.folio.rest.domain.FeeFineKafkaTopic;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

public class KafkaEventProducer {
  private static final String PRODUCER_NAME = "mod-feesfines-events";

  private final String tenantId;
  private final Function<KafkaProducerRecord<String, String>, Future<Void>> sender;

  public KafkaEventProducer(Vertx vertx, Map<String, String> okapiHeaders) {
    this(tenantId(okapiHeaders), createSender(vertx));
  }

  KafkaEventProducer(String tenantId,
    Function<KafkaProducerRecord<String, String>, Future<Void>> sender) {

    this.tenantId = requireNonNull(tenantId);
    this.sender = requireNonNull(sender);
  }

  public Future<Void> publish(EventType eventType, String payload) {
    return sender.apply(createRecord(eventType, payload));
  }

  private KafkaProducerRecord<String, String> createRecord(EventType eventType, String payload) {
    KafkaProducerRecord<String, String> producerRecord = KafkaProducerRecord.create(
      FeeFineKafkaTopic.from(eventType).fullTopicName(tenantId), eventType.name(), payload);

    producerRecord.addHeader(FolioKafkaHeaders.TENANT_ID, tenantId);
    return producerRecord;
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
