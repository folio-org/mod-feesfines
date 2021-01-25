package org.folio.test.support.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.util.Arrays;

import org.folio.rest.jaxrs.model.DefaultActionRequest;
import org.folio.rest.jaxrs.model.DefaultBulkActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.hamcrest.Matcher;

import io.vertx.core.json.JsonObject;

public class FeeFineActionMatchers {
  private FeeFineActionMatchers(){}

  public static Matcher<JsonObject> feeFineAction(String accountId, String userId, double balance,
    double amount, String actionType, String transactionInfo, DefaultActionRequest request) {

    return feeFineAction(accountId, userId, balance, amount, actionType, transactionInfo,
      request.getUserName(), request.getComments(), request.getNotifyPatron(),
      request.getServicePointId(), request.getPaymentMethod());
  }

  public static Matcher<JsonObject> feeFineAction(String accountId, String userId, double balance,
    double amount, String actionType, String transactionInfo, DefaultBulkActionRequest request) {

    return feeFineAction(accountId, userId, balance, amount, actionType, transactionInfo,
      request.getUserName(), request.getComments(), request.getNotifyPatron(),
      request.getServicePointId(), request.getPaymentMethod());
  }

  public static Matcher<JsonObject> feeFineAction(String accountId, String userId, double balance,
    double amount, String actionType, String transactionInfo, String userName, String comments,
    boolean notifyPatron, String createdAt, String paymentMethod) {

    return allOf(Arrays.asList(
      hasJsonPath("typeAction", is(actionType)),
      hasJsonPath("amountAction", is((float) amount)),
      hasJsonPath("balance", is((float) balance)),
      hasJsonPath("transactionInformation", is(transactionInfo)),
      hasJsonPath("accountId", is(accountId)),
      hasJsonPath("userId", is(userId)),
      hasJsonPath("id", notNullValue(String.class)),
      hasJsonPath("dateAction", notNullValue(String.class)),
      hasJsonPath("source", is(userName)),
      hasJsonPath("comments", is(comments)),
      hasJsonPath("notify", is(notifyPatron)),
      hasJsonPath("createdAt", is(createdAt)),
      hasJsonPath("paymentMethod", is(paymentMethod))
    ));
  }

  public static Matcher<JsonObject> feeFineAction(Feefineaction feefineaction) {
    return allOf(Arrays.asList(
      hasJsonPath("typeAction", is(feefineaction.getTypeAction())),
      hasJsonPath("amountAction", is(feefineaction.getAmountAction().floatValue())),
      hasJsonPath("balance", is(feefineaction.getBalance().floatValue())),
      hasJsonPath("accountId", is(feefineaction.getAccountId())),
      hasJsonPath("userId", is(feefineaction.getUserId())),
      hasJsonPath("id", notNullValue(String.class)),
      hasJsonPath("dateAction", notNullValue(String.class)),
      hasJsonPath("comments", is(feefineaction.getComments()))
    ));
  }
}
