package org.folio.test.support.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;

import org.hamcrest.Matcher;

import io.restassured.response.Response;

public final class AccountMatchers {

  private AccountMatchers() {}

  public static Matcher<Response> isPaidFully() {
    return new TypeMappingMatcher<>(
      response -> response.getBody().asString(),
      allOf(
        hasJsonPath("status.name", is("Closed")),
        hasJsonPath("paymentStatus.name", is("Paid fully")),
        hasJsonPath("remaining", is(0.0))
      ));
  }
}
