package org.folio.rest.impl;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.BulkCheckActionRequest;
import org.folio.rest.jaxrs.model.BulkCheckActionResponse;
import org.folio.rest.jaxrs.resource.AccountsBulk;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.service.action.validation.ActionValidationService;
import org.folio.rest.service.action.validation.DefaultActionValidationService;
import org.folio.rest.service.action.validation.RefundActionValidationService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class AccountsBulkAPI implements AccountsBulk {
  @Override
  public void postAccountsBulkCheckPay(BulkCheckActionRequest entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    AccountRepository accountRepository = new AccountRepository(vertxContext, okapiHeaders);
    ActionValidationService actionValidationService = new DefaultActionValidationService(
      accountRepository);

    checkBulkAction(entity, asyncResultHandler,
      new RefundActionValidationService(okapiHeaders, vertxContext));
  }

  private void checkBulkAction(BulkCheckActionRequest request,
    Handler<AsyncResult<Response>> asyncResultHandler,
    ActionValidationService validationService) {

    List<String> accountIds = request.getAccountIds();
    String rawAmount = request.getAmount();

    validationService.validateByIds(accountIds, rawAmount)
      .onSuccess(result -> {
        BulkCheckActionResponse response = new BulkCheckActionResponse()
          .withAccountIds(accountIds)
          .withAmount(request.getAmount())
          .withAllowed(true)
          .withRemainingAmount(result.getRemainingAmount());

        asyncResultHandler.handle(Future.succeededFuture(
          AccountsBulk.PostAccountsBulkCheckPayResponse
            .respond200WithApplicationJson(response)));
      }).onFailure(throwable -> {
      String errorMessage = throwable.getLocalizedMessage();
      if (throwable instanceof FailedValidationException) {
        BulkCheckActionResponse response = new BulkCheckActionResponse()
          .withAccountIds(accountIds)
          .withAmount(request.getAmount())
          .withAllowed(false)
          .withErrorMessage(errorMessage);
        asyncResultHandler.handle(Future.succeededFuture(
          AccountsBulk.PostAccountsBulkCheckPayResponse
            .respond422WithApplicationJson(response)));
      } else if (throwable instanceof AccountNotFoundValidationException) {
        asyncResultHandler.handle(Future.succeededFuture(
          AccountsBulk.PostAccountsBulkCheckPayResponse
            .respond404WithTextPlain(errorMessage)));
      } else {
        asyncResultHandler.handle(Future.succeededFuture(
          AccountsBulk.PostAccountsBulkCheckPayResponse
            .respond500WithTextPlain(errorMessage)));
      }
    });
  }

  private BulkCheckActionResponse buildResponse(List<String> accountIds, String amount,
    boolean allowed, String remainingAmount) {

    return new BulkCheckActionResponse()
      .withAccountIds(accountIds)
      .withAmount(amount)
      .withAllowed(allowed)
      .withRemainingAmount(remainingAmount);
  }
}
