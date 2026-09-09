package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.service.KafkaService;
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

    if (isTenantDisable(tenantAttributes)) {
      postTenantForDisable(tenantAttributes, headers, handler, context);
    } else {
      postTenantForEnableOrUpgrade(tenantAttributes, headers, handler, context);
    }
  }

  protected KafkaService kafkaService(Vertx vertx) {
    return kafkaServiceFactory.apply(vertx);
  }

  private void postTenantForEnableOrUpgrade(TenantAttributes tenantAttributes,
    Map<String, String> headers, Handler<AsyncResult<Response>> handler, Context context) {

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
            handlePostTenantFailure(performResponse.cause(), handler);
            return;
          }

          try {
            createKafkaTopics(vertx, headers)
              .onSuccess(v -> {
                log.info("postTenant executed successfully");
                handler.handle(res);
              })
              .onFailure(t -> handlePostTenantFailure(t, handler));
          } catch (Exception e) {
            handlePostTenantFailure(e, handler);
          }
        });
    }, context);
  }

  private void postTenantForDisable(TenantAttributes tenantAttributes,
    Map<String, String> headers, Handler<AsyncResult<Response>> handler, Context context) {

    Vertx vertx = context.owner();
    // RMB treats disable as absent module_to and rejects blank strings.
    tenantAttributes.withModuleTo(null);
    super.postTenantSync(tenantAttributes, headers, res -> {
      if (res.failed()) {
        handler.handle(res);
        return;
      }

      if (!isPurgeRequested(tenantAttributes)) {
        handler.handle(res);
        return;
      }

      try {
        deleteKafkaTopics(vertx, headers)
          .onSuccess(v -> {
            log.info("postTenant tenant purge executed successfully");
            handler.handle(res);
          })
          .onFailure(t -> handlePostTenantFailure(t, handler));
      } catch (Exception e) {
        handlePostTenantFailure(e, handler);
      }
    }, context);
  }

  private Future<Void> createKafkaTopics(Vertx vertx, Map<String, String> headers) {
    return kafkaService(vertx).createTopics(tenantId(headers));
  }

  private Future<Void> deleteKafkaTopics(Vertx vertx, Map<String, String> headers) {
    return kafkaService(vertx).deleteTopics(tenantId(headers));
  }

  private static boolean isTenantDisable(TenantAttributes tenantAttributes) {
    return isConfigured(tenantAttributes.getModuleFrom())
      && !isConfigured(tenantAttributes.getModuleTo());
  }

  private static boolean isPurgeRequested(TenantAttributes tenantAttributes) {
    return TRUE.equals(tenantAttributes.getPurge());
  }

  private static boolean isConfigured(String value) {
    return value != null && !value.isBlank();
  }

  private static void handlePostTenantFailure(Throwable throwable,
    Handler<AsyncResult<Response>> handler) {

    log.error("postTenant failure", throwable);
    handler.handle(succeededFuture(PostTenantResponse
      .respond500WithTextPlain(throwable.getLocalizedMessage())));
  }
}
