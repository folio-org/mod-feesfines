package org.folio.test.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.util.PostgresTester;
import org.testcontainers.containers.Network;

public class ReadyPostgresTester implements PostgresTester {
  private static final Duration PORT_TIMEOUT = Duration.ofSeconds(15);
  private static final int CONNECT_TIMEOUT_MS = 200;
  private static final int RETRY_DELAY_MS = 100;

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
    long deadline = System.nanoTime() + PORT_TIMEOUT.toNanos();
    IOException lastException = null;

    while (System.nanoTime() < deadline) {
      try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        return;
      } catch (IOException ex) {
        lastException = ex;
        sleepBeforeRetry();
      }
    }

    throw new IllegalStateException("Timed out waiting for Postgres tester port "
      + host + ':' + port, lastException);
  }

  private static void sleepBeforeRetry() {
    try {
      Thread.sleep(RETRY_DELAY_MS);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for Postgres tester port", ex);
    }
  }
}
