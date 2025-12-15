package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.HttpStatus;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.service.PubSubRegistrationService;
import org.folio.rest.tools.utils.AsyncResponseResult;
import org.folio.rest.tools.utils.TenantLoading;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class TenantRefAPI extends TenantAPI {
  private static final Logger log = LogManager.getLogger(TenantRefAPI.class);

  @Override
  public void postTenant(TenantAttributes tenantAttributes,
    Map<String, String> headers,
    Handler<AsyncResult<Response>> handler, Context context) {

    log.info("postTenant");
    log.info("Tenant attributes: {}", JsonObject.mapFrom(tenantAttributes));

    Vertx vertx = context.owner();
    super.postTenantSync(tenantAttributes, headers, context)
      .onFailure(t -> {
        log.error("postTenant:: postTenant failure", t);
        handler.handle(succeededFuture(PostTenantResponse
          .respond500WithTextPlain(t.getLocalizedMessage())));
      })
      .onSuccess(res -> {
          AsyncResult<Response> asyncResponse = new AsyncResponseResult().map(res);
          if (res.getStatus() != HttpStatus.HTTP_NO_CONTENT.toInt()) {
            handler.handle(asyncResponse);
            return;
          }

          TenantLoading tenantLoading = new TenantLoading();
          tenantLoading.withKey("loadReference").withLead("reference-data")
            .withIdContent()
            .add("lost-item-fees-policies")
            .add("overdue-fines-policies")
            .perform(tenantAttributes, headers, vertx, performResponse -> {
              if (performResponse.failed()) {
                log.error("postTenant:: postTenant failure", performResponse.cause());
                handler.handle(succeededFuture(PostTenantResponse
                  .respond500WithTextPlain(performResponse.cause().getLocalizedMessage())));
                return;
              } else {
                log.info("postTenant:: reference data loaded successfully");
              }

              vertx.executeBlocking(() -> new PubSubRegistrationService(vertx, headers).registerModule()
                .onFailure(t -> {
                  log.error("postTenant:: registration in pubsub failed", t);
                  handler.handle(succeededFuture(PostTenantResponse
                    .respond500WithTextPlain(t.getLocalizedMessage())));
                })
                .onSuccess(ignored -> {
                  log.info("postTenant executed successfully");
                  handler.handle(asyncResponse);
                }));
            });
        }
      );
  }

}
