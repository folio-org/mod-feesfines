package org.folio.rest.utils;

import java.util.function.Function;

import org.folio.rest.jaxrs.model.ActionFailureResponse;
import org.folio.rest.jaxrs.model.ActionSuccessResponse;
import org.folio.rest.jaxrs.model.BulkActionFailureResponse;
import org.folio.rest.jaxrs.model.BulkActionSuccessResponse;
import org.folio.rest.jaxrs.model.BulkCheckActionResponse;
import org.folio.rest.jaxrs.model.CheckActionResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsCancelByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsCheckPayByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsCheckWaiveByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsCheckTransferByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsCheckRefundByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsPayByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsRefundByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsTransferByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsWaiveByAccountIdResponse;
import org.folio.rest.jaxrs.resource.AccountsBulk;
import org.folio.rest.jaxrs.resource.AccountsBulk.PostAccountsBulkCheckPayResponse;
import org.folio.rest.jaxrs.resource.support.ResponseDelegate;

public enum ActionResultAdapter {
  PAY(
    PostAccountsCheckPayByAccountIdResponse::respond200WithApplicationJson,
    PostAccountsCheckPayByAccountIdResponse::respond404WithTextPlain,
    PostAccountsCheckPayByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsCheckPayByAccountIdResponse::respond500WithTextPlain,
    PostAccountsPayByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsPayByAccountIdResponse::respond404WithTextPlain,
    PostAccountsPayByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsPayByAccountIdResponse::respond500WithTextPlain,
    PostAccountsBulkCheckPayResponse::respond200WithApplicationJson,
    PostAccountsBulkCheckPayResponse::respond404WithTextPlain,
    PostAccountsBulkCheckPayResponse::respond422WithApplicationJson,
    PostAccountsBulkCheckPayResponse::respond500WithTextPlain,
    null,
    null,
    null,
    null
  ),
  WAIVE(
    PostAccountsCheckWaiveByAccountIdResponse::respond200WithApplicationJson,
    PostAccountsCheckWaiveByAccountIdResponse::respond404WithTextPlain,
    PostAccountsCheckWaiveByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsCheckWaiveByAccountIdResponse::respond500WithTextPlain,
    PostAccountsWaiveByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsWaiveByAccountIdResponse::respond404WithTextPlain,
    PostAccountsWaiveByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsWaiveByAccountIdResponse::respond500WithTextPlain,
    null,
    null,
    null,
    null,
    AccountsBulk.PostAccountsBulkWaiveResponse::respond201WithApplicationJson,
    AccountsBulk.PostAccountsBulkWaiveResponse::respond404WithTextPlain,
    AccountsBulk.PostAccountsBulkWaiveResponse::respond422WithApplicationJson,
    AccountsBulk.PostAccountsBulkWaiveResponse::respond500WithTextPlain
  ),
  TRANSFER(
    PostAccountsCheckTransferByAccountIdResponse::respond200WithApplicationJson,
    PostAccountsCheckTransferByAccountIdResponse::respond404WithTextPlain,
    PostAccountsCheckTransferByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsCheckTransferByAccountIdResponse::respond500WithTextPlain,
    PostAccountsTransferByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsTransferByAccountIdResponse::respond404WithTextPlain,
    PostAccountsTransferByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsTransferByAccountIdResponse::respond500WithTextPlain,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null
  ),
  CANCEL(
    null,
    null,
    null,
    null,
    PostAccountsCancelByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsCancelByAccountIdResponse::respond404WithTextPlain,
    PostAccountsCancelByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsCancelByAccountIdResponse::respond500WithTextPlain,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null
  ),
  REFUND(
    PostAccountsCheckRefundByAccountIdResponse::respond200WithApplicationJson,
    PostAccountsCheckRefundByAccountIdResponse::respond404WithTextPlain,
    PostAccountsCheckRefundByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsCheckRefundByAccountIdResponse::respond500WithTextPlain,
    PostAccountsRefundByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsRefundByAccountIdResponse::respond404WithTextPlain,
    PostAccountsRefundByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsRefundByAccountIdResponse::respond500WithTextPlain,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null
  );

  public final Function<CheckActionResponse, ResponseDelegate> check200;
  public final Function<String, ResponseDelegate> check404;
  public final Function<CheckActionResponse, ResponseDelegate> check422;
  public final Function<String, ResponseDelegate> check500;
  public final Function<ActionSuccessResponse, ResponseDelegate> action201;
  public final Function<String, ResponseDelegate> action404;
  public final Function<ActionFailureResponse, ResponseDelegate> action422;
  public final Function<String, ResponseDelegate> action500;
  public final Function<BulkCheckActionResponse, ResponseDelegate> bulkCheck200;
  public final Function<String, ResponseDelegate> bulkCheck404;
  public final Function<BulkCheckActionResponse, ResponseDelegate> bulkCheck422;
  public final Function<String, ResponseDelegate> bulkCheck500;
  public final Function<BulkActionSuccessResponse, ResponseDelegate> bulkAction201;
  public final Function<String, ResponseDelegate> bulkAction404;
  public final Function<BulkActionFailureResponse, ResponseDelegate> bulkAction422;
  public final Function<String, ResponseDelegate> bulkAction500;

  ActionResultAdapter(Function<CheckActionResponse, ResponseDelegate> check200,
    Function<String, ResponseDelegate> check404,
    Function<CheckActionResponse, ResponseDelegate> check422,
    Function<String, ResponseDelegate> check500,
    Function<ActionSuccessResponse, ResponseDelegate> action201,
    Function<String, ResponseDelegate> action404,
    Function<ActionFailureResponse, ResponseDelegate> action422,
    Function<String, ResponseDelegate> action500,
    Function<BulkCheckActionResponse, ResponseDelegate> bulkCheck200,
    Function<String, ResponseDelegate> bulkCheck404,
    Function<BulkCheckActionResponse, ResponseDelegate> bulkCheck422,
    Function<String, ResponseDelegate> bulkCheck500,
    Function<BulkActionSuccessResponse, ResponseDelegate> bulkAction201,
    Function<String, ResponseDelegate> bulkAction404,
    Function<BulkActionFailureResponse, ResponseDelegate> bulkAction422,
    Function<String, ResponseDelegate> bulkAction500) {

    this.check200 = check200;
    this.check404 = check404;
    this.check422 = check422;
    this.check500 = check500;
    this.action201 = action201;
    this.action404 = action404;
    this.action422 = action422;
    this.action500 = action500;
    this.bulkCheck200 = bulkCheck200;
    this.bulkCheck404 = bulkCheck404;
    this.bulkCheck422 = bulkCheck422;
    this.bulkCheck500 = bulkCheck500;
    this.bulkAction201 = bulkAction201;
    this.bulkAction404 = bulkAction404;
    this.bulkAction422 = bulkAction422;
    this.bulkAction500 = bulkAction500;
  }
}
