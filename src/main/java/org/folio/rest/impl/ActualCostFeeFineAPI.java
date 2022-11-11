package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.exception.http.HttpGetException;
import org.folio.rest.jaxrs.model.ActualCostFeeFineCancel;
import org.folio.rest.jaxrs.resource.ActualCostFeeFine;
import org.folio.rest.service.ActualCostRecordService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class ActualCostFeeFineAPI implements ActualCostFeeFine {

  private static final Logger logger = LogManager.getLogger(ActualCostFeeFineAPI.class);

  @Override
  public void postActualCostFeeFineCancel(ActualCostFeeFineCancel entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new ActualCostRecordService(vertxContext.owner(), okapiHeaders)
      .cancelActualCostRecord(entity)
      .onSuccess(response -> asyncResultHandler.handle(succeededFuture(
        PostActualCostFeeFineCancelResponse.respond201WithApplicationJson(entity))))
      .onFailure(throwable -> {
        if (throwable instanceof FailedValidationException) {
          asyncResultHandler.handle(succeededFuture(PostActualCostFeeFineCancelResponse
            .respond422WithTextPlain(throwable.getMessage())));
        } else if (throwable instanceof HttpGetException) {
          asyncResultHandler.handle(succeededFuture(PostActualCostFeeFineCancelResponse
            .respond404WithTextPlain(throwable.getMessage())));
        } else {
          asyncResultHandler.handle(succeededFuture(
            PostActualCostFeeFineCancelResponse.respond500WithTextPlain(throwable)));
        }
      });
  }
}
