package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.RefundReport;
import org.folio.rest.jaxrs.resource.FeefineReports;
import org.folio.rest.service.report.RefundReportService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FeeFineReportsAPI implements FeefineReports {
  private static final Logger log = LoggerFactory.getLogger(FeeFineReportsAPI.class);

  private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";

  @Override
  public void getFeefineReportsRefund(String startDate, String endDate,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    log.info(format("Refund report requested, parameters: startDate=%s, endDate=%s",
      startDate, endDate));

    new RefundReportService(okapiHeaders, vertxContext)
      .buildReport(startDate, endDate)
      .onComplete(result -> handleRefundReportResult(result, asyncResultHandler));
  }

  private void handleRefundReportResult(AsyncResult<RefundReport> asyncResult,
    Handler<AsyncResult<Response>> asyncResultHandler) {
    if (asyncResult.succeeded()) {
      asyncResultHandler.handle(succeededFuture(FeefineReports.GetFeefineReportsRefundResponse
        .respond200WithApplicationJson(asyncResult.result())));
    }
    else if (asyncResult.failed()) {
      final Throwable cause = asyncResult.cause();
      if (cause instanceof FailedValidationException) {
        asyncResultHandler.handle(succeededFuture(FeefineReports.GetFeefineReportsRefundResponse
          .respond400WithTextPlain(cause.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(succeededFuture(FeefineReports.GetFeefineReportsRefundResponse
          .respond500WithTextPlain(INTERNAL_SERVER_ERROR_MESSAGE)));
      }
    }
  }
}
