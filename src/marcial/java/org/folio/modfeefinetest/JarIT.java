package org.folio.modfeefinestest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class JarIT {
  class InputStreamCollector implements Runnable {
    private BufferedReader reader;
    private String output = "";

    public InputStreamCollector(InputStream inputStream) {
      reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public void run() {
      try {
        while (true) {
          String line = reader.readLine();
          if (line == null) {
            break;
          }
          output += line + System.lineSeparator();
        }
      } catch (IOException e) {
        e.printStackTrace();
        output = null;
      } finally {
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
          output = null;
        }
      }
    }
  }

  /**
   * Check for broken jar. When postgres-runner is after domain-models-runtime
   * in pom.xml's dependencies section then RestLauncher stops with
   * "The command 'run' is not a valid command." This is also triggered when
   * invoking with the version command. This is a jar issue, invoking
   * RestLauncher.main directly always works.<p />
   * https://issues.folio.org/browse/FOLIO-532
   */
  @Test
  public void jar(TestContext context) throws Exception {
    // invoke the jar packaged by maven
    Process process = new ProcessBuilder("java", "-jar", "target/mod-feefines-fat.jar", "version")
        .redirectErrorStream(true).start();
    InputStreamCollector inputStreamCollector = new InputStreamCollector(process.getInputStream());
    new Thread(inputStreamCollector, "inputStreamCollector").start();
    process.waitFor();
    String output = inputStreamCollector.output;
    context.assertTrue(! output.contains("is not a valid command"),
        "jar RestLauncher invokes valid command: " + output);
    context.assertEquals(0, process.exitValue());
  }
}
