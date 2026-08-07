package org.folio.rest.service;

import static java.util.Objects.requireNonNull;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import org.folio.kafka.services.KafkaAdminClientService;
import org.folio.kafka.services.KafkaTopic;
import org.folio.rest.domain.FeeFineKafkaTopic;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class KafkaService {
  private static final String KAFKA_HOST_ENV = "KAFKA_HOST";
  private static final String KAFKA_HOST_SYS_PROP = "kafka-host";

  private final BiFunction<KafkaTopic[], String, Future<Void>> topicCreator;
  private final BiFunction<KafkaTopic[], String, Future<Void>> topicDeleter;
  private final BooleanSupplier topicAdministrationEnabled;

  public KafkaService(Vertx vertx) {
    this(new KafkaAdminClientService(vertx));
  }

  private KafkaService(KafkaAdminClientService kafkaAdminClientService) {
    this(kafkaAdminClientService::createKafkaTopics,
      kafkaAdminClientService::deleteKafkaTopics, KafkaService::isKafkaConfigured);
  }

  KafkaService(BiFunction<KafkaTopic[], String, Future<Void>> topicCreator) {
    this(topicCreator, noOpTopicAdmin(), () -> true);
  }

  KafkaService(BiFunction<KafkaTopic[], String, Future<Void>> topicCreator,
    BooleanSupplier topicAdministrationEnabled) {

    this(topicCreator, noOpTopicAdmin(), topicAdministrationEnabled);
  }

  KafkaService(BiFunction<KafkaTopic[], String, Future<Void>> topicCreator,
    BiFunction<KafkaTopic[], String, Future<Void>> topicDeleter) {

    this(topicCreator, topicDeleter, () -> true);
  }

  KafkaService(BiFunction<KafkaTopic[], String, Future<Void>> topicCreator,
    BiFunction<KafkaTopic[], String, Future<Void>> topicDeleter,
    BooleanSupplier topicAdministrationEnabled) {

    this.topicCreator = requireNonNull(topicCreator);
    this.topicDeleter = requireNonNull(topicDeleter);
    this.topicAdministrationEnabled = requireNonNull(topicAdministrationEnabled);
  }

  public Future<Void> createTopics(String tenantId) {
    if (!topicAdministrationEnabled.getAsBoolean()) {
      return Future.succeededFuture();
    }

    return topicCreator.apply(FeeFineKafkaTopic.values(), tenantId);
  }

  public Future<Void> deleteTopics(String tenantId) {
    if (!topicAdministrationEnabled.getAsBoolean()) {
      return Future.succeededFuture();
    }

    return topicDeleter.apply(FeeFineKafkaTopic.values(), tenantId);
  }

  private static BiFunction<KafkaTopic[], String, Future<Void>> noOpTopicAdmin() {
    return (topics, tenantId) -> Future.succeededFuture();
  }

  private static boolean isKafkaConfigured() {
    return isKafkaConfigured(System.getenv(KAFKA_HOST_ENV),
      System.getProperty(KAFKA_HOST_ENV), System.getProperty(KAFKA_HOST_SYS_PROP));
  }

  static boolean isKafkaConfigured(String kafkaHostEnv, String kafkaHostSystemProperty,
    String kafkaHostLegacySystemProperty) {

    return isConfigured(kafkaHostEnv)
      || isConfigured(kafkaHostSystemProperty)
      || isConfigured(kafkaHostLegacySystemProperty);
  }

  private static boolean isConfigured(String value) {
    return value != null && !value.isBlank();
  }
}
