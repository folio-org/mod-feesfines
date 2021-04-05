package org.folio.rest.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.DateUtils.parseDateReportParameter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.annotations.Validate;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.CashDrawerReconciliationReportRequest;
import org.folio.rest.jaxrs.model.RefundReportRequest;
import org.folio.rest.jaxrs.resource.FeefineReports;
import org.folio.rest.service.report.CashDrawerReconciliationReportService;
import org.folio.rest.service.report.RefundReportService;
import org.joda.time.DateTime;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class FeeFineReportsAPI implements FeefineReports {
  private static final Logger log = LogManager.getLogger(FeeFineReportsAPI.class);

  private static final String INVALID_START_DATE_MESSAGE = "Start date should not be empty when end date is specified";
  private static final String START_DATE_IS_NULL_MESSAGE = "Start date should not be empty";
  private static final String INVALID_START_DATE_OR_END_DATE_MESSAGE = "Invalid startDate or endDate parameter";
  private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";

  @Validate
  @Override
  public void postFeefineReportsRefund(RefundReportRequest entity, Map<String,
    String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    log.info("Refund report requested, parameters: startDate={}, endDate={}",
      entity.getStartDate(), entity.getEndDate());

    String rawStartDate = entity.getStartDate();
    String rawEndDate = entity.getEndDate();

    if (rawStartDate == null && rawEndDate != null){
      log.error("startDate is null and endDate is not null");

      handleRefundReportResult(
        failedFuture(new FailedValidationException(INVALID_START_DATE_MESSAGE)),
        asyncResultHandler);
      return;
    }

    DateTime startDate;
    DateTime endDate;

    try {
      startDate = parseDateReportParameter(rawStartDate);
      endDate = parseDateReportParameter(rawEndDate);
    } catch (IllegalArgumentException e) {
      logInvalidDatesAndHandleResult(rawStartDate, rawEndDate, asyncResultHandler);
      return;
    }

    new RefundReportService(okapiHeaders, vertxContext)
      .buildReport(startDate, endDate, entity.getFeeFineOwners())
      .onComplete(result -> handleRefundReportResult(result, asyncResultHandler,
        PostFeefineReportsRefundResponse::respond200WithApplicationJson));
  }

  @Override
  public void postFeefineReportsCashDrawerReconciliation(CashDrawerReconciliationReportRequest entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    String rawStartDate = entity.getStartDate();
    String rawEndDate = entity.getEndDate();
    String createdAt = entity.getCreatedAt();
    List<String> sources = entity.getSources();

    log.info("Cash drawer reconciliation report requested, parameters: startDate={}, endDate={}, " +
        "createdAt={}, sources={}", rawStartDate, rawEndDate, createdAt, sources);

    if (rawStartDate == null) {
      log.error("startDate parameter is null");

      handleRefundReportResult(
        failedFuture(new FailedValidationException(START_DATE_IS_NULL_MESSAGE)),
        asyncResultHandler);
      return;
    }

    DateTime startDate;
    DateTime endDate;

    try {
      startDate = parseDateReportParameter(rawStartDate);
      endDate = parseDateReportParameter(rawEndDate);
    } catch (IllegalArgumentException e) {
      logInvalidDatesAndHandleResult(rawStartDate, rawEndDate, asyncResultHandler);
      return;
    }

    new CashDrawerReconciliationReportService(okapiHeaders, vertxContext, startDate, endDate,
      entity.getCreatedAt(), entity.getSources())
      .build()
      .onComplete(result -> handleRefundReportResult(result, asyncResultHandler,
        PostFeefineReportsCashDrawerReconciliationResponse::respond200WithApplicationJson));
  }

  private <T> void handleRefundReportResult(AsyncResult<T> asyncResult,
    Handler<AsyncResult<Response>> asyncResultHandler) {

    handleRefundReportResult(asyncResult, asyncResultHandler, null);
  }

  private <T> void handleRefundReportResult(AsyncResult<T> asyncResult,
    Handler<AsyncResult<Response>> asyncResultHandler, Function<T, Response> responseFunction) {
    if (asyncResult.succeeded()) {
      asyncResultHandler.handle(succeededFuture(responseFunction.apply(asyncResult.result())));
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

//  private void handleRefundReportResult(AsyncResult<RefundReport> asyncResult,
//    Handler<AsyncResult<Response>> asyncResultHandler) {
//    if (asyncResult.succeeded()) {
//      asyncResultHandler.handle(succeededFuture(FeefineReports.PostFeefineReportsRefundResponse
//        .respond200WithApplicationJson(asyncResult.result())));
//    }
//    else if (asyncResult.failed()) {
//      final Throwable cause = asyncResult.cause();
//      if (cause instanceof FailedValidationException) {
//        log.error("Report parameters validation failed: " + cause.getLocalizedMessage());
//        asyncResultHandler.handle(succeededFuture(FeefineReports.PostFeefineReportsRefundResponse
//          .respond422WithTextPlain(cause.getLocalizedMessage())));
//      } else {
//        log.error("Failed to build report: " + cause.getLocalizedMessage());
//        asyncResultHandler.handle(succeededFuture(FeefineReports.PostFeefineReportsRefundResponse
//          .respond500WithTextPlain(INTERNAL_SERVER_ERROR_MESSAGE)));
//      }
//    }
//  }

  private void logInvalidDatesAndHandleResult(String rawStartDate, String rawEndDate,
    Handler<AsyncResult<Response>> asyncResultHandler) {

    log.error("Invalid request parameters: startDate={}, endDate={}", rawStartDate, rawEndDate);

    handleRefundReportResult(
      failedFuture(new FailedValidationException(INVALID_START_DATE_OR_END_DATE_MESSAGE)),
      asyncResultHandler);
  }
}
