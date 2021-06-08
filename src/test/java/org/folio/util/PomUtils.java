package org.folio.util;

import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomUtils {
  public static String getModuleVersion() {
    try {
      Model model = new MavenXpp3Reader().read(new FileReader("pom.xml"));
      return model.getVersion();
    }
    catch (IOException | XmlPullParserException e) {
      throw new RuntimeException("Failed to parse pom.xml");
    }
  }

  public static String getModuleNameAndVersion() {
    return "mod-feesfines-" + getModuleVersion().replaceAll("-.*", "");
  }
}
