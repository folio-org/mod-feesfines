package org.folio.test.support.matcher;

import java.util.Map;
import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import io.vertx.core.json.JsonObject;

public class JsonMatchers {

  /**
   * Checks whether the object being asserted has the same values for properties
   * as expectedProperties has.
   *
   * This is equivalent of {@code expectedProperties.map().containsAll(actual.map())}.
   *
   * @param expectedProperties - expected properties and values to be present.
   * @return true only if all the properties from expectedProperties are present
   * and has the same value in actual object.
   */
  public static Matcher<JsonObject> hasSameProperties(JsonObject expectedProperties) {
    return new TypeSafeMatcher<JsonObject>() {
      @Override
      protected boolean matchesSafely(JsonObject actual) {
        if (actual == null || actual.size() < expectedProperties.size()) {
          return false;
        }

        for (Map.Entry<String, Object> expectedProperty : expectedProperties) {
          final String propertyKey = expectedProperty.getKey();
          final Object expectedValue = expectedProperty.getValue();
          final Object actualValue = actual.getValue(propertyKey);

          if (!Objects.equals(expectedValue, actualValue)) {
            return false;
          }
        }

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Json object has following properties")
          .appendValue(expectedProperties.toString());

      }
    };
  }
}
