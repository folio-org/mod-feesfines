package org.folio.test.support.matcher;

import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public final class MappableMatcher<T, V> extends TypeSafeDiagnosingMatcher<T> {
  private final Function<T, V> mapper;
  private final Matcher<V> matcher;

  public MappableMatcher(Function<T, V> mapper, Matcher<V> matcher) {
    this.mapper = mapper;
    this.matcher = matcher;
  }

  @Override
  protected final boolean matchesSafely(T entity, Description description) {
    if (entity == null) {
      description.appendText("Actual is null, expected ")
        .appendDescriptionOf(matcher);
      return false;
    }

    try {
      return matcher.matches(mapper.apply(entity));
    } catch (Exception ex) {
      description.appendText("Unable to map value ").appendValue(ex);
      return false;
    }
  }

  @Override
  public final void describeTo(Description description) {
    description.appendDescriptionOf(matcher);
  }
}
