package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.exception.http.HttpNotFoundException;
import org.folio.rest.jaxrs.model.ActualCostFeeFineCancel;
import org.folio.rest.jaxrs.resource.ActualCostFeeFine;
import org.folio.rest.service.ActualCostRecordService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class ActualCostFeeFineAPI implements ActualCostFeeFine {

  @Override
  public void postActualCostFeeFineCancel(ActualCostFeeFineCancel entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new ActualCostRecordService(vertxContext.owner(), okapiHeaders)
      .cancelActualCostRecord(entity)
      .onSuccess(actualCostRecord -> asyncResultHandler.handle(succeededFuture(
        PostActualCostFeeFineCancelResponse.respond201WithApplicationJson(actualCostRecord))))
      .onFailure(throwable -> {
        if (throwable instanceof FailedValidationException) {
          asyncResultHandler.handle(succeededFuture(PostActualCostFeeFineCancelResponse
            .respond422WithTextPlain(throwable.getMessage())));
        } else if (throwable instanceof HttpNotFoundException) {
          asyncResultHandler.handle(succeededFuture(PostActualCostFeeFineCancelResponse
            .respond404WithTextPlain(format("Actual cost record %s was not found",
              entity.getActualCostRecordId()))));
        } else {
          asyncResultHandler.handle(succeededFuture(
            PostActualCostFeeFineCancelResponse.respond500WithTextPlain(throwable)));
        }
      });
  }
}
