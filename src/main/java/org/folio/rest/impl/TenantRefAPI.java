package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.requireNonNull;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.service.KafkaService;
import org.folio.rest.service.PubSubRegistrationService;
import org.folio.rest.tools.utils.TenantLoading;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class TenantRefAPI extends TenantAPI {
  private static final Logger log = LogManager.getLogger(TenantRefAPI.class);
  private static Function<Vertx, KafkaService> kafkaServiceFactory = KafkaService::new;

  static void setKafkaServiceFactory(Function<Vertx, KafkaService> factory) {
    kafkaServiceFactory = requireNonNull(factory);
  }

  static void resetKafkaServiceFactory() {
    kafkaServiceFactory = KafkaService::new;
  }

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

          vertx.executeBlocking(() -> createKafkaTopics(vertx, headers)
            .compose(v -> new PubSubRegistrationService(vertx, headers).registerModule())
            .onSuccess(v -> {
              log.info("postTenant executed successfully");
              handler.handle(res);
            })
            .onFailure(t -> {
              log.error("postTenant failure", t);
              handler.handle(succeededFuture(PostTenantResponse
                .respond500WithTextPlain(t.getLocalizedMessage())));
            }));
        });
    }, context);
  }

  protected KafkaService kafkaService(Vertx vertx) {
    return kafkaServiceFactory.apply(vertx);
  }

  private Future<Void> createKafkaTopics(Vertx vertx, Map<String, String> headers) {
    return kafkaService(vertx).createTopics(tenantId(headers));
  }
}
