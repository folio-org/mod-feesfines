package org.folio.rest.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.RefundReport;
import org.folio.rest.jaxrs.model.RefundReportRequest;
import org.folio.rest.jaxrs.resource.FeefineReports;
import org.folio.rest.service.report.RefundReportService;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FeeFineReportsAPI implements FeefineReports {
  private static final Logger log = LoggerFactory.getLogger(FeeFineReportsAPI.class);

  private static final String INVALID_START_DATE_MESSAGE =
    "Invalid startDate parameter";
  private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";

  @Validate
  @Override
  public void postFeefineReportsRefund(RefundReportRequest entity, Map<String,
    String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    log.info("Refund report requested, parameters: startDate={}, endDate={}",
      entity.getStartDate(), entity.getEndDate());

    DateTime startDate = parseDate(entity.getStartDate());
    DateTime endDate = parseDate(entity.getEndDate());

    if (startDate == null && endDate == null) {
      startDate = new DateTime(Long.MIN_VALUE);
      endDate = DateTime.now();
    }

    if (startDate == null) {
      log.error("Invalid parameter: startDate is null");

      handleRefundReportResult(
        failedFuture(new FailedValidationException(INVALID_START_DATE_MESSAGE)),
        asyncResultHandler);
    } else {
      if (endDate == null) {
        endDate = DateTime.now();
      }
      new RefundReportService(okapiHeaders, vertxContext)
        .buildReport(startDate, endDate, entity.getFeeFineOwners())
        .onComplete(result -> handleRefundReportResult(result, asyncResultHandler));
    }
  }

  private void handleRefundReportResult(AsyncResult<RefundReport> asyncResult,
    Handler<AsyncResult<Response>> asyncResultHandler) {
    if (asyncResult.succeeded()) {
      asyncResultHandler.handle(succeededFuture(FeefineReports.PostFeefineReportsRefundResponse
        .respond200WithApplicationJson(asyncResult.result())));
    }
    else if (asyncResult.failed()) {
      final Throwable cause = asyncResult.cause();
      if (cause instanceof FailedValidationException) {
        log.error("Report parameters validation failed: " + cause.getLocalizedMessage());
        asyncResultHandler.handle(succeededFuture(FeefineReports.PostFeefineReportsRefundResponse
          .respond422WithTextPlain(cause.getLocalizedMessage())));
      } else {
        log.error("Failed to build report: " + cause.getLocalizedMessage());
        asyncResultHandler.handle(succeededFuture(FeefineReports.PostFeefineReportsRefundResponse
          .respond500WithTextPlain(INTERNAL_SERVER_ERROR_MESSAGE)));
      }
    }
  }

  private DateTime parseDate(String date) {
    if (date == null) {
      return null;
    }

    try {
      return DateTime.parse(date, ISODateTimeFormat.date());
    }
    catch (IllegalArgumentException e) {
      return null;
    }
  }
}
