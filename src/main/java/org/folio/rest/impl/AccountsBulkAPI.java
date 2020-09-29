package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.domain.Action;
import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.BulkCheckActionRequest;
import org.folio.rest.jaxrs.model.BulkCheckActionResponse;
import org.folio.rest.jaxrs.resource.AccountsBulk;
import org.folio.rest.service.action.validation.ActionValidationService;
import org.folio.rest.service.action.validation.DefaultActionValidationService;
import org.folio.rest.utils.ActionResultAdapter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class AccountsBulkAPI implements AccountsBulk {

  private final static Logger logger = LoggerFactory.getLogger(AccountsBulkAPI.class);
  private static final Logger logger = LoggerFactory.getLogger(AccountsBulkAPI.class);

  @Override
  public void postAccountsBulkCheckPay(BulkCheckActionRequest entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkBulkAction(entity, asyncResultHandler,
      new DefaultActionValidationService(okapiHeaders, vertxContext), Action.PAY);
  }

  @Override
  public void postAccountsBulkCheckTransfer(BulkCheckActionRequest entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkBulkAction(entity, asyncResultHandler,
      new DefaultActionValidationService(okapiHeaders, vertxContext), Action.TRANSFER);
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
}
