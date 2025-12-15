package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.service.PubSubRegistrationService;
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

    log.info("postTenant:: tenant attributes: {}", JsonObject.mapFrom(tenantAttributes));

    Vertx vertx = context.owner();
    super.postTenantSync(tenantAttributes, headers, context)
      .onFailure(t -> handleException(t, handler))
      .onSuccess(postTenantResponse -> {
        if (postTenantResponse.getStatus() != 204) {
          handler.handle(succeededFuture(postTenantResponse));
          return;
        }

        TenantLoading tenantLoading = new TenantLoading();
        tenantLoading.withKey("loadReference").withLead("reference-data")
          .withIdContent()
          .add("lost-item-fees-policies")
          .add("overdue-fines-policies")
          .perform(tenantAttributes, headers, vertx, performResponse -> {
            if (performResponse.failed()) {
              log.error("postTenant:: failed to load reference data", performResponse.cause());
              handler.handle(succeededFuture(PostTenantResponse
                .respond500WithTextPlain(performResponse.cause().getLocalizedMessage())));
              return;
            }

            vertx.executeBlocking(() -> new PubSubRegistrationService(vertx, headers).registerModule()
              .onSuccess(v -> {
                log.info("postTenant:: module successfully registered in mod-pubsub");
                handler.handle(succeededFuture(postTenantResponse));
              })
              .onFailure(t -> {
                log.error("postTenant:: failed to register in mod-pubsub", t);
                handler.handle(succeededFuture(PostTenantResponse
                  .respond500WithTextPlain(t.getLocalizedMessage())));
              }));
          });
      });
  }

  private static void handleException(Throwable t, Handler<AsyncResult<Response>> handler) {
    log.error("handleException:: postTenant failure", t);
    handler.handle(succeededFuture(PostTenantResponse
      .respond500WithTextPlain(t.getLocalizedMessage())));
  }
}
