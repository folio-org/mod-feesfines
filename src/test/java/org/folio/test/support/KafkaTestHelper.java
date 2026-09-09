package org.folio.test.support;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.kafka.admin.KafkaAdminClient;

public final class KafkaTestHelper {
  private static final int TIMEOUT_SECONDS = 30;
  private static final int POLL_TIMEOUT_SECONDS = 5;
  private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka-native:4.2.0");
  private static final String LEGACY_KAFKA_HOST_PROPERTY = "kafka-host";
  private static final String LEGACY_KAFKA_PORT_PROPERTY = "kafka-port";
  private static final String KAFKA_HOST_PROPERTY = "KAFKA_HOST";
  private static final String KAFKA_PORT_PROPERTY = "KAFKA_PORT";

  private static KafkaTestHelper instance;

  private final KafkaContainer kafkaContainer;
  private final Vertx vertx;
  private final KafkaAdminClient adminClient;
  private final String previousLegacyKafkaHost;
  private final String previousLegacyKafkaPort;
  private final String previousKafkaHost;
  private final String previousKafkaPort;
  private final Thread shutdownHook;

  private boolean closed;

  private KafkaTestHelper() {
    previousLegacyKafkaHost = System.getProperty(LEGACY_KAFKA_HOST_PROPERTY);
    previousLegacyKafkaPort = System.getProperty(LEGACY_KAFKA_PORT_PROPERTY);
    previousKafkaHost = System.getProperty(KAFKA_HOST_PROPERTY);
    previousKafkaPort = System.getProperty(KAFKA_PORT_PROPERTY);

    kafkaContainer = new KafkaContainer(KAFKA_IMAGE);
    kafkaContainer.start();

    String host = kafkaContainer.getHost();
    String port = String.valueOf(kafkaContainer.getFirstMappedPort());
    System.setProperty(LEGACY_KAFKA_HOST_PROPERTY, host);
    System.setProperty(LEGACY_KAFKA_PORT_PROPERTY, port);
    System.setProperty(KAFKA_HOST_PROPERTY, host);
    System.setProperty(KAFKA_PORT_PROPERTY, port);

    vertx = Vertx.vertx();
    adminClient = createAdminClient(host + ":" + port);

    shutdownHook = new Thread(this::close);
    Runtime.getRuntime().addShutdownHook(shutdownHook);
  }

  public static synchronized KafkaTestHelper getInstance() {
    if (instance == null) {
      instance = new KafkaTestHelper();
    }

    return instance;
  }

  public synchronized void close() {
    if (closed) {
      return;
    }

    closed = true;
    removeShutdownHook();

    try {
      waitFor(adminClient.close());
    } catch (Exception ignored) {
      // Cleanup must not mask test process termination.
    }

    try {
      waitFor(vertx.close());
    } catch (Exception ignored) {
      // Cleanup must not mask test process termination.
    }

    try {
      if (kafkaContainer.isRunning()) {
        kafkaContainer.stop();
      }
    } catch (Exception ignored) {
      // Cleanup must not mask test process termination.
    }

    restoreSystemProperties();
    clearInstance(this);
  }

  public Set<String> listTopics() {
    return waitFor(adminClient.listTopics());
  }

  public void deleteTopics(Collection<String> topicNames) {
    List<String> existingTopicNames = listTopics().stream()
      .filter(topicNames::contains)
      .toList();

    if (!existingTopicNames.isEmpty()) {
      waitFor(adminClient.deleteTopics(existingTopicNames));
    }

    verifyTopicsDoNotExist(topicNames);
  }

  public void verifyTopicsExist(Collection<String> topicNames) {
    await().atMost(TIMEOUT_SECONDS, SECONDS)
      .until(() -> listTopics().containsAll(topicNames));
  }

  public void verifyTopicsDoNotExist(Collection<String> topicNames) {
    await().atMost(TIMEOUT_SECONDS, SECONDS)
      .until(() -> listTopics().stream().noneMatch(topicNames::contains));
  }

  /**
   * Polls the given Kafka topic for messages published at or after the given timestamp.
   * Uses a fresh consumer group with earliest offset reset to read all messages,
   * filtering by record timestamp. Polls for up to {@code POLL_TIMEOUT_SECONDS} seconds.
   *
   * @param topic           Kafka topic name
   * @param fromTimestampMs only return messages with timestamp &gt;= this value
   * @return list of message values (strings)
   */
  public List<String> pollMessages(String topic, long fromTimestampMs) {
    String bootstrapServers = kafkaContainer.getHost() + ":" + kafkaContainer.getFirstMappedPort();
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    List<String> messages = new ArrayList<>();
    long pollDeadline = System.currentTimeMillis() + POLL_TIMEOUT_SECONDS * 1000L;

    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
      consumer.subscribe(List.of(topic));
      while (System.currentTimeMillis() < pollDeadline) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
        for (ConsumerRecord<String, String> rec : records) {
          if (rec.timestamp() >= fromTimestampMs) {
            messages.add(rec.value());
          }
        }
      }
    }

    return messages;
  }

  public static <T> T waitFor(Future<T> future) {
    try {
      return future.toCompletionStage()
        .toCompletableFuture()
        .get(TIMEOUT_SECONDS, SECONDS);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private KafkaAdminClient createAdminClient(String kafkaUrl) {
    Properties config = new Properties();
    config.put("bootstrap.servers", kafkaUrl);

    return KafkaAdminClient.create(vertx, config);
  }

  private void removeShutdownHook() {
    try {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    } catch (IllegalStateException ignored) {
      // JVM shutdown already started; the hook is being run or can no longer be removed.
    }
  }

  private void restoreSystemProperties() {
    restoreSystemProperty(LEGACY_KAFKA_HOST_PROPERTY, previousLegacyKafkaHost);
    restoreSystemProperty(LEGACY_KAFKA_PORT_PROPERTY, previousLegacyKafkaPort);
    restoreSystemProperty(KAFKA_HOST_PROPERTY, previousKafkaHost);
    restoreSystemProperty(KAFKA_PORT_PROPERTY, previousKafkaPort);
  }

  private static void restoreSystemProperty(String name, String previousValue) {
    if (previousValue == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, previousValue);
    }
  }

  private static synchronized void clearInstance(KafkaTestHelper helper) {
    if (instance == helper) {
      instance = null;
    }
  }
}
