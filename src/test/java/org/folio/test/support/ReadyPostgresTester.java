package org.folio.test.support;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.util.PostgresTester;
import org.testcontainers.containers.Network;

public class ReadyPostgresTester implements PostgresTester {
  private static final Duration PORT_TIMEOUT = Duration.ofSeconds(15);
  private static final Duration RETRY_DELAY = Duration.ofMillis(100);
  private static final int CONNECT_TIMEOUT_MS = 200;

  private final PostgresTester delegate;

  public ReadyPostgresTester() {
    this(new PostgresTesterContainer());
  }

  ReadyPostgresTester(PostgresTester delegate) {
    this.delegate = delegate;
  }

  @Override
  public void start(String database, String username, String password) {
    delegate.start(database, username, password);
    waitUntilConnectable(getHost(), getPort());
    waitUntilConnectable(getReadHost(), getReadPort());
  }

  @Override
  public Integer getPort() {
    return delegate.getPort();
  }

  @Override
  public String getHost() {
    return delegate.getHost();
  }

  @Override
  public String getReadHost() {
    return delegate.getReadHost();
  }

  @Override
  public Integer getReadPort() {
    return delegate.getReadPort();
  }

  @Override
  public Network getNetwork() {
    return delegate.getNetwork();
  }

  @Override
  public boolean isStarted() {
    return delegate.isStarted();
  }

  @Override
  public void close() {
    delegate.close();
  }

  private static void waitUntilConnectable(String host, Integer port) {
    await("Postgres tester port " + host + ':' + port)
      .pollInterval(RETRY_DELAY)
      .atMost(PORT_TIMEOUT)
      .untilAsserted(() -> assertConnectable(host, port));
  }

  private static void assertConnectable(String host, Integer port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
    } catch (IOException ex) {
      throw new AssertionError("Postgres tester port is not connectable: " + host + ':' + port, ex);
    }
  }
}
