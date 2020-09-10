package org.folio.rest.utils;

import java.util.function.Function;

import org.folio.rest.jaxrs.model.ActionFailureResponse;
import org.folio.rest.jaxrs.model.ActionSuccessResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsCancelByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsPayByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsRefundByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsTransferByAccountIdResponse;
import org.folio.rest.jaxrs.resource.Accounts.PostAccountsWaiveByAccountIdResponse;
import org.folio.rest.jaxrs.resource.support.ResponseDelegate;

public enum ActionResultAdapter {
  PAY(
    PostAccountsPayByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsPayByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsPayByAccountIdResponse::respond404WithTextPlain,
    PostAccountsPayByAccountIdResponse::respond500WithTextPlain
  ),
  WAIVE(
    PostAccountsWaiveByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsWaiveByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsWaiveByAccountIdResponse::respond404WithTextPlain,
    PostAccountsWaiveByAccountIdResponse::respond500WithTextPlain
  ),
  TRANSFER(
    PostAccountsTransferByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsTransferByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsTransferByAccountIdResponse::respond404WithTextPlain,
    PostAccountsTransferByAccountIdResponse::respond500WithTextPlain
  ),
  CANCEL(
    PostAccountsCancelByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsCancelByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsCancelByAccountIdResponse::respond404WithTextPlain,
    PostAccountsCancelByAccountIdResponse::respond500WithTextPlain
  ),
  REFUND(
    PostAccountsRefundByAccountIdResponse::respond201WithApplicationJson,
    PostAccountsRefundByAccountIdResponse::respond422WithApplicationJson,
    PostAccountsRefundByAccountIdResponse::respond404WithTextPlain,
    PostAccountsRefundByAccountIdResponse::respond500WithTextPlain
  );

  private final Function<ActionSuccessResponse, ResponseDelegate> handlerFor201;
  private final Function<ActionFailureResponse, ResponseDelegate> handlerFor422;
  private final Function<String, ResponseDelegate> handlerFor404;
  private final Function<String, ResponseDelegate> handlerFor500;

  ActionResultAdapter(
    Function<ActionSuccessResponse, ResponseDelegate> handlerFor201,
    Function<ActionFailureResponse, ResponseDelegate> handlerFor422,
    Function<String, ResponseDelegate> handlerFor404,
    Function<String, ResponseDelegate> handlerFor500) {

    this.handlerFor201 = handlerFor201;
    this.handlerFor422 = handlerFor422;
    this.handlerFor404 = handlerFor404;
    this.handlerFor500 = handlerFor500;
  }

  public ResponseDelegate to201(ActionSuccessResponse response) {
    return handlerFor201.apply(response);
  }

  public ResponseDelegate to422(ActionFailureResponse response) {
    return handlerFor422.apply(response);
  }

  public ResponseDelegate to404(String errorMessage) {
    return handlerFor404.apply(errorMessage);
  }

  public ResponseDelegate to500(String errorMessage) {
    return handlerFor500.apply(errorMessage);
  }
}
