package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.domain.Action.CANCEL;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.domain.Action.WAIVE;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.ActionRequest;
import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.BulkActionFailureResponse;
import org.folio.rest.jaxrs.model.BulkActionSuccessResponse;
import org.folio.rest.jaxrs.model.BulkCheckActionRequest;
import org.folio.rest.jaxrs.model.BulkCheckActionResponse;
import org.folio.rest.jaxrs.model.CancelBulkActionRequest;
import org.folio.rest.jaxrs.model.DefaultBulkActionRequest;
import org.folio.rest.jaxrs.resource.AccountsBulk;
import org.folio.rest.service.action.CancelActionService;
import org.folio.rest.service.action.PayActionService;
import org.folio.rest.service.action.RefundActionService;
import org.folio.rest.service.action.TransferActionService;
import org.folio.rest.service.action.WaiveActionService;
import org.folio.rest.service.action.context.ActionContext;
import org.folio.rest.service.action.validation.ActionValidationService;
import org.folio.rest.service.action.validation.DefaultActionValidationService;
import org.folio.rest.service.action.validation.RefundActionValidationService;
import org.folio.rest.utils.ActionResultAdapter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AccountsBulkAPI implements AccountsBulk {
  private static final Logger logger = LoggerFactory.getLogger(AccountsBulkAPI.class);

  @Override
  public void postAccountsBulkCheckPay(BulkCheckActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkBulkAction(request, asyncResultHandler,
      new DefaultActionValidationService(okapiHeaders, vertxContext), PAY);
  }

  @Override
  public void postAccountsBulkCheckTransfer(BulkCheckActionRequest entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkBulkAction(entity, asyncResultHandler,
      new DefaultActionValidationService(okapiHeaders, vertxContext), TRANSFER);
  }

  @Override
  public void postAccountsBulkCheckWaive(BulkCheckActionRequest entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    checkBulkAction(entity, asyncResultHandler,
      new DefaultActionValidationService(okapiHeaders, vertxContext), WAIVE);
  }

  @Override
  public void postAccountsBulkCheckRefund(BulkCheckActionRequest entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkBulkAction(entity, asyncResultHandler,
      new RefundActionValidationService(okapiHeaders, vertxContext), Action.REFUND);
  }

  @Override
  public void postAccountsBulkPay(DefaultBulkActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    ActionRequest actionRequest = ActionRequest.from(request);

    new PayActionService(okapiHeaders, vertxContext)
      .performAction(actionRequest)
      .onComplete(result -> handleActionResult(actionRequest, result, asyncResultHandler, PAY));
  }

  @Override
  public void postAccountsBulkWaive(DefaultBulkActionRequest request, Map<String,
    String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    ActionRequest actionRequest = ActionRequest.from(request);

    new WaiveActionService(okapiHeaders, vertxContext)
      .performAction(actionRequest)
      .onComplete(result -> handleActionResult(actionRequest, result, asyncResultHandler, WAIVE));
  }

  @Override
  public void postAccountsBulkCancel(CancelBulkActionRequest request, Map<String,
    String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    ActionRequest actionRequest = ActionRequest.from(request);

    new CancelActionService(okapiHeaders, vertxContext)
      .performAction(actionRequest)
      .onComplete(result -> handleActionResult(actionRequest, result, asyncResultHandler, CANCEL));
  }

  @Override
  public void postAccountsBulkTransfer(DefaultBulkActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    ActionRequest actionRequest = ActionRequest.from(request);

    new TransferActionService(okapiHeaders, vertxContext)
      .performAction(actionRequest)
      .onComplete(result -> handleActionResult(actionRequest, result, asyncResultHandler, TRANSFER));
  }

  @Override
  public void postAccountsBulkRefund(DefaultBulkActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    ActionRequest actionRequest = ActionRequest.from(request);

    new RefundActionService(okapiHeaders, vertxContext)
      .performAction(actionRequest)
      .onComplete(result -> handleActionResult(actionRequest, result, asyncResultHandler, Action.REFUND));
  }

  private void checkBulkAction(BulkCheckActionRequest request,
    Handler<AsyncResult<Response>> asyncResultHandler,
    ActionValidationService validationService, Action action) {

    List<String> accountIds = request.getAccountIds();
    String rawAmount = request.getAmount();

    ActionResultAdapter resultAdapter = action.getActionResultAdapter();
    if (resultAdapter == null) {
      String errorMessage = "Unprocessable action: " + action.name();
      logger.error(errorMessage);
      asyncResultHandler.handle(succeededFuture(AccountsBulk.PostAccountsBulkCheckPayResponse
        .respond500WithTextPlain(errorMessage)));
      return;
    }

    validationService.validateByIds(accountIds, rawAmount)
      .onSuccess(result -> {
        BulkCheckActionResponse response = buildResponse(accountIds, request.getAmount(), true,
          result.getRemainingAmount(), null);
        asyncResultHandler.handle(succeededFuture(resultAdapter.bulkCheck200.apply(response)));
      })
      .onFailure(throwable -> {
        String errorMessage = throwable.getLocalizedMessage();

        if (throwable instanceof FailedValidationException) {
          BulkCheckActionResponse response = buildResponse(accountIds, request.getAmount(), false,
            null, errorMessage);
          asyncResultHandler.handle(succeededFuture(resultAdapter.bulkCheck422.apply(response)));
        }
        else if (throwable instanceof AccountNotFoundValidationException) {
          asyncResultHandler.handle(succeededFuture(
            resultAdapter.bulkCheck404.apply(errorMessage)));
        }
        else {
          asyncResultHandler.handle(succeededFuture(
            resultAdapter.bulkCheck500.apply(errorMessage)));
        }
      });
  }

  private BulkCheckActionResponse buildResponse(List<String> accountIds, String amount,
    boolean allowed, String remainingAmount, String errorMessage) {

    BulkCheckActionResponse bulkCheckActionResponse = new BulkCheckActionResponse()
      .withAccountIds(accountIds)
      .withAmount(amount)
      .withAllowed(allowed);

    if (remainingAmount != null) {
      bulkCheckActionResponse.withRemainingAmount(remainingAmount);
    }

    if (errorMessage != null) {
      bulkCheckActionResponse.withErrorMessage(errorMessage);
    }

    return bulkCheckActionResponse;
  }

  private void handleActionResult(ActionRequest request,
    AsyncResult<ActionContext> asyncResult, Handler<AsyncResult<Response>> asyncResultHandler,
    Action action) {

    ActionResultAdapter resultAdapter = action.getActionResultAdapter();
    if (resultAdapter == null) {
      String errorMessage = "Unprocessable action: " + action.name();
      logger.error(errorMessage);
      asyncResultHandler.handle(succeededFuture(AccountsBulk.PostAccountsBulkPayResponse
        .respond500WithTextPlain(errorMessage)));
      return;
    }

    if (asyncResult.succeeded()) {
      final ActionContext actionContext = asyncResult.result();
      BulkActionSuccessResponse response = new BulkActionSuccessResponse()
        .withAccountIds(request.getAccountIds())
        .withFeefineactions(actionContext.getFeeFineActions());
      if (actionContext.getRequestedAmount() != null) {
        response.withAmount(actionContext.getRequestedAmount().toString());
      }
      asyncResultHandler.handle(succeededFuture(resultAdapter.bulkAction201.apply(response)));
    } else if (asyncResult.failed()) {
      final Throwable cause = asyncResult.cause();
      String errorMessage = cause.getLocalizedMessage();
      logger.error(errorMessage, cause);
      if (cause instanceof FailedValidationException) {
        BulkActionFailureResponse response = new BulkActionFailureResponse()
          .withAccountIds(request.getAccountIds())
          .withErrorMessage(errorMessage)
          .withAmount(request.getAmount());
        asyncResultHandler.handle(succeededFuture(resultAdapter.bulkAction422.apply(response)));
      } else if (cause instanceof AccountNotFoundValidationException) {
        asyncResultHandler.handle(succeededFuture(resultAdapter.bulkAction404.apply(errorMessage)));
      } else {
        asyncResultHandler.handle(succeededFuture(resultAdapter.bulkAction500.apply(errorMessage)));
      }
    }
  }
}
