package org.folio.test.support.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.PAID_FULLY;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.folio.rest.jaxrs.model.Account;
import org.hamcrest.Matcher;

import io.restassured.response.Response;

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
    return allOf(List.of(
      hasJsonPath("amount", is(account.getAmount().getAmount().doubleValue())),
      hasJsonPath("remaining", is(account.getRemaining().getAmount().doubleValue())),
      hasJsonPath("status.name", is(account.getStatus().getName())),
      hasJsonPath("paymentStatus.name", is(account.getPaymentStatus().getName().toString())),
      hasJsonPath("feeFineType", is(account.getFeeFineType())),
      hasJsonPath("feeFineOwner", is(account.getFeeFineOwner())),
      hasJsonPath("title", is(account.getTitle())),
      hasJsonPath("callNumber", is(account.getCallNumber())),
      hasJsonPath("barcode", is(account.getBarcode())),
      hasJsonPath("materialType", is(account.getMaterialType())),
      hasJsonPath("materialTypeId", is(account.getMaterialTypeId())),
      hasJsonPath("itemStatus.name", is(account.getItemStatus().getName())),
      hasJsonPath("location", is(account.getLocation())),
      hasJsonPath("dueDate", is(formatDate(account.getDueDate()))),
      hasJsonPath("returnedDate", is(formatDate(account.getReturnedDate()))),
      hasJsonPath("loanId", is(account.getLoanId())),
      hasJsonPath("userId", is(account.getUserId())),
      hasJsonPath("itemId", is(account.getItemId())),
      hasJsonPath("feeFineId", is(account.getFeeFineId())),
      hasJsonPath("ownerId", is(account.getOwnerId())),
      hasJsonPath("loanPolicyId", is(account.getLoanPolicyId())),
      hasJsonPath("overdueFinePolicyId", is(account.getOverdueFinePolicyId())),
      hasJsonPath("lostItemFeePolicyId", is(account.getLostItemFeePolicyId()))
    ));
  }

  private static String formatDate(Date date) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormat.format(date) + "+00:00";
  }
}
