package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.exception.http.HttpNotFoundException;
import org.folio.rest.jaxrs.model.ActualCostFeeFineBill;
import org.folio.rest.jaxrs.model.ActualCostFeeFineCancel;
import org.folio.rest.jaxrs.resource.ActualCostFeeFine;
import org.folio.rest.service.ActualCostFeeFineBillingService;
import org.folio.rest.service.ActualCostFeeFineCancellationService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class ActualCostFeeFineAPI implements ActualCostFeeFine {

  @Override
  public void postActualCostFeeFineCancel(ActualCostFeeFineCancel request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new ActualCostFeeFineCancellationService(vertxContext, okapiHeaders)
      .cancel(request)
      .onSuccess(actualCostRecord -> asyncResultHandler.handle(succeededFuture(
        PostActualCostFeeFineCancelResponse.respond201WithApplicationJson(actualCostRecord))))
      .onFailure(throwable -> {
        if (throwable instanceof FailedValidationException || throwable instanceof HttpNotFoundException) {
          asyncResultHandler.handle(succeededFuture(PostActualCostFeeFineCancelResponse
            .respond422WithTextPlain(throwable.getMessage())));
        } else {
          asyncResultHandler.handle(succeededFuture(
            PostActualCostFeeFineCancelResponse.respond500WithTextPlain(throwable)));
        }
      });
  }

  @Override
  public void postActualCostFeeFineBill(ActualCostFeeFineBill request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new ActualCostFeeFineBillingService(vertxContext, okapiHeaders)
      .bill(request)
      .onSuccess(actualCostRecord -> asyncResultHandler.handle(succeededFuture(
        PostActualCostFeeFineBillResponse.respond201WithApplicationJson(actualCostRecord))))
      .onFailure(throwable -> {
        if (throwable instanceof FailedValidationException || throwable instanceof HttpNotFoundException) {
          asyncResultHandler.handle(succeededFuture(PostActualCostFeeFineBillResponse
            .respond422WithTextPlain(throwable.getMessage())));
        } else {
          asyncResultHandler.handle(succeededFuture(
            PostActualCostFeeFineBillResponse.respond500WithTextPlain(throwable)));
        }
      });
  }

}
