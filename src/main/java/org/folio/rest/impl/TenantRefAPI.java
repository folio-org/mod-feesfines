package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.service.PubSubRegistrationService;
import org.folio.rest.service.migration.FeeFineActionMigrationService;
import org.folio.rest.tools.utils.TenantLoading;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
    super.postTenantSync(tenantAttributes, headers, res -> {
      if (res.failed()) {
        handler.handle(res);
        return;
      }

      TenantLoading tenantLoading = new TenantLoading();
      tenantLoading.withKey("loadReference").withLead("reference-data")
        .withIdContent()
        .add("lost-item-fees-policies")
        .add("overdue-fines-policies")
        .perform(tenantAttributes, headers, vertx, performResponse -> {
          if (performResponse.failed()) {
            log.error("postTenant failure", performResponse.cause());
            handler.handle(succeededFuture(PostTenantResponse
              .respond500WithTextPlain(performResponse.cause().getLocalizedMessage())));
            return;
          }

          vertx.executeBlocking((Promise<Void> promise) ->
            new FeeFineActionMigrationService(headers, context).doMigration(tenantAttributes)
              .compose(ignored -> new PubSubRegistrationService(vertx, headers).registerModule())
              .onSuccess(r -> log.info("postTenant success"))
              .onFailure(t -> log.error("postTenant failure", t))
              .onComplete(promise),
            result -> {
              if (result.failed()) {
                log.error("postTenant failure", result.cause());
                handler.handle(succeededFuture(PostTenantResponse
                  .respond500WithTextPlain(result.cause().getLocalizedMessage())));
              } else {
                log.info("postTenant executed successfully");
                handler.handle(res);
              }
            }
          );
        });
    }, context);
  }
}
