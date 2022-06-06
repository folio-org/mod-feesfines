package org.folio.test.support.matcher;

import io.restassured.response.Response;
import org.folio.rest.jaxrs.model.Account;
import org.hamcrest.Matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.PAID_FULLY;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;

public final class AccountMatchers {

  private AccountMatchers() {}

  public static Matcher<Response> isPaidFully() {
    return new TypeMappingMatcher<>(
      response -> response.getBody().asString(),
      allOf(
        hasJsonPath("status.name", is("Closed")),
        hasJsonPath("paymentStatus.name", is(PAID_FULLY.value())),
        hasJsonPath("remaining", is(0.0))
      ));
  }

  public static Matcher<Response> singleAccountMatcher(Account account) {
    return new TypeMappingMatcher<>(
      response -> response.getBody().asString(),
      allOf(
        hasJsonPath("amount", is(account.getAmount().toString()))//,
//        hasJsonPath("remaining", is(account.getRemaining().toString())),
//        hasJsonPath("status.name", is(account.getStatus().getName())),
//        hasJsonPath("paymentStatus.name", is(PAID_FULLY.value())),
//        hasJsonPath("feeFineType", is(account.getFeeFineType())),
//        hasJsonPath("feeFineOwner", is(account.getFeeFineOwner())),
//        hasJsonPath("title", is(account.getTitle())),
//        hasJsonPath("callNumber", is(account.getCallNumber())),
//        hasJsonPath("barcode", is(account.getBarcode())),
//        hasJsonPath("materialType", is(account.getMaterialType())),
//        hasJsonPath("itemStatus.name", is(account.getItemStatus().getName())),
//        hasJsonPath("location", is(account.getLocation())),
//        hasJsonPath("dueDate", is(account.getDueDate())),
//        hasJsonPath("returnedDate", is(account.getReturnedDate())),
//        hasJsonPath("loanId", is(account.getLoanId())),
//        hasJsonPath("userId", is(account.getUserId())),
//        hasJsonPath("itemId", is(account.getItemId())),
//        hasJsonPath("materialTypeId", is(account.getMaterialTypeId())),
//        hasJsonPath("feeFineId", is(account.getFeeFineId())),
//        hasJsonPath("ownerId", is(account.getOwnerId())),
//        hasJsonPath("loanPolicyId", is(account.getLoanPolicyId())),
//        hasJsonPath("overdueFinePolicyId", is(account.getOverdueFinePolicyId())),
//        hasJsonPath("lostItemFeePolicyId", is(account.getLostItemFeePolicyId()))
      ));
  }
}
