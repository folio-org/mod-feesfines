package org.folio.test.support.matcher;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.core.AllOf.allOf;

import org.hamcrest.Matcher;

public final class FeeFineMatchers {
  private FeeFineMatchers() {}

  @SuppressWarnings("all")
  public static Matcher<?> hasAllAutomaticFeeFineTypes() {
    return hasJsonPath("feefines", allOf(iterableWithSize(5),
      hasItems(
        allOf(
          hasJsonPath("id", is("9523cb96-e752-40c2-89da-60f3961a488d")),
          hasJsonPath("automatic", is(true)),
          hasJsonPath("feeFineType", is("Overdue fine"))),
        allOf(
          hasJsonPath("id", is("cf238f9f-7018-47b7-b815-bb2db798e19f")),
          hasJsonPath("automatic", is(true)),
          hasJsonPath("feeFineType", is("Lost item fee"))),
        allOf(
          hasJsonPath("id", is("73785370-d3bd-4d92-942d-ae2268e02ded")),
          hasJsonPath("automatic", is(true)),
          hasJsonPath("feeFineType", is("Lost item fee (actual cost)"))),
        allOf(
          hasJsonPath("id", is("c7dede15-aa48-45ed-860b-f996540180e0")),
          hasJsonPath("automatic", is(true)),
          hasJsonPath("feeFineType", is("Lost item processing fee"))),
        allOf(
          hasJsonPath("id", is("d20df2fb-45fd-4184-b238-0d25747ffdd9")),
          hasJsonPath("automatic", is(true)),
          hasJsonPath("feeFineType", is("Replacement processing fee")))
      )));
  }
}
