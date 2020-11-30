package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.RefundReport;
import org.folio.rest.jaxrs.resource.FeefineReports;
import org.folio.rest.service.report.RefundReportService;
import org.folio.rest.tools.messages.Messages;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FeeFineReportsAPI implements FeefineReports {
  private static final Logger log = LoggerFactory.getLogger(FeeFineReportsAPI.class);

  private static final int REPORT_ROWS_LIMIT = 1000000;
  private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";

  private final Messages messages = Messages.getInstance();

  @Override
  public void getFeefineReportsRefund(String startDate, String endDate,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    log.info(format("Refund report requested, parameters: startDate=%s, endDate=%s",
      startDate, endDate));

    new RefundReportService(okapiHeaders, vertxContext)
      .buildReport(startDate, endDate)
      .onComplete(result -> handleRefundReportResult(result, asyncResultHandler));

//    try {
//      vertxContext.runOnContext(v -> {
//        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
//
//        try {
//          DateTime startDateTime = parseDateTime(startDate);
//          DateTime endDateTime = parseDateTime(endDate);
//
//          if (startDateTime == null || endDateTime == null) {
//            FeefineReports.GetFeefineReportsRefundResponse.respond400WithTextPlain(
//              INVALID_START_DATE_OR_END_DATE_MESSAGE);
//            return;
//          }
//
//          Criterion criterion =
//            new Criterion(getDateCriteria(DB_FIELD_FEEFINEACTIONS_DATEACTION, ">=", startDateTime))
//            .addCriterion(getDateCriteria(DB_FIELD_FEEFINEACTIONS_DATEACTION, "<=", endDateTime));
//
//          PostgresClient.getInstance(vertxContext.owner(), tenantId).get(DB_TABLE_FEEFINEACTIONS,
//            Feefineaction.class, criterion, true, false, result -> {
//              if (result.failed()) {
//                logger.error(result.result());
//
//                asyncResultHandler.handle(succeededFuture(
//                  FeefineReports.GetFeefineReportsRefundResponse.respond500WithTextPlain(
//                    messages.getMessage(lang, MessageConsts.InternalServerError))));
//              } else {
//                List<Feefineaction> refundFeeFineActionList = result.result().getResults();
//
//
//                asyncResultHandler.handle(
//                    succeededFuture(
//                      Accounts.GetAccountsByAccountIdResponse.respond200WithApplicationJson(
//                        accountList.get(0)))));
//              }
//            });
//        } catch (Exception e) {
//          logger.error(e.getMessage());
//          asyncResultHandler.handle(succeededFuture(
//            Accounts.GetAccountsResponse.respond500WithTextPlain(messages.getMessage(
//              lang, MessageConsts.InternalServerError))));
//        }
//      });
//    } catch (Exception e) {
//      asyncResultHandler.handle(succeededFuture(
//        Accounts.GetAccountsResponse.respond500WithTextPlain(messages.getMessage(
//          lang, MessageConsts.InternalServerError))));
//    }
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
