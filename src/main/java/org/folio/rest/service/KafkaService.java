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
  private final BooleanSupplier topicCreationEnabled;

  public KafkaService(Vertx vertx) {
    this(new KafkaAdminClientService(vertx)::createKafkaTopics, KafkaService::isKafkaConfigured);
  }

  KafkaService(BiFunction<KafkaTopic[], String, Future<Void>> topicCreator) {
    this(topicCreator, () -> true);
  }

  KafkaService(BiFunction<KafkaTopic[], String, Future<Void>> topicCreator,
    BooleanSupplier topicCreationEnabled) {
    this.topicCreator = requireNonNull(topicCreator);
    this.topicCreationEnabled = requireNonNull(topicCreationEnabled);
  }

  public Future<Void> createTopics(String tenantId) {
    if (!topicCreationEnabled.getAsBoolean()) {
      return Future.succeededFuture();
    }

    return topicCreator.apply(FeeFineKafkaTopic.values(), tenantId);
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
