package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.DoNotBillActualCost;
import org.folio.rest.jaxrs.resource.DoNotBillActualCostLostItemFee;
import org.folio.rest.service.ActualCostRecordService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

public class DoNotBillActualCostLostItemFeeAPI implements DoNotBillActualCostLostItemFee {

  @Override
  public void postDoNotBillActualCostLostItemFee(DoNotBillActualCost entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new ActualCostRecordService(vertxContext.owner(), okapiHeaders)
      .cancelActualCostRecord(entity)
      .onSuccess(response -> asyncResultHandler.handle(succeededFuture(
        PostDoNotBillActualCostLostItemFeeResponse.respond201WithApplicationJson(entity))))
      .onFailure(throwable -> asyncResultHandler.handle(succeededFuture(
        PostDoNotBillActualCostLostItemFeeResponse.respond500WithTextPlain(entity))));
  }
}
