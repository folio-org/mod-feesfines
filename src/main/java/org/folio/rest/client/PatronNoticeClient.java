package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.HttpStatus.HTTP_OK;

import java.util.Map;

import org.folio.rest.jaxrs.model.PatronNotice;
import org.folio.rest.utils.PatronNoticeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;

public class PatronNoticeClient extends OkapiClient {
  private static final Logger log = LoggerFactory.getLogger(PatronNoticeClient.class);
  public PatronNoticeClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<Void> postPatronNotice(PatronNotice notice) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    final JsonObject chargeContext = (JsonObject) notice.getContext().getAdditionalProperties().get("feeCharge");
    final JsonObject actionContext = (JsonObject) notice.getContext().getAdditionalProperties().get("feeAction");
    log.info("amount value from json [{}]", chargeContext.getString("amount"));
    log.info("remainingAmount value from json [{}]", chargeContext.getString("remainingAmount"));
    log.info("buildFeeActionContext");
    log.info("amount value from json [{}]", actionContext.getString("amount"));
    log.info("remainingAmount value from json [{}]", actionContext.getString("remainingAmount"));
    okapiPostAbs("/patron-notice").sendJson(notice, promise);

    return promise.future()
      .compose(this::interpretResponse);
  }

  private Future<Void> interpretResponse(HttpResponse<Buffer> response) {
    return response.statusCode() == HTTP_OK.toInt()
      ? succeededFuture()
      : failedFuture(String.format("Failed to send patron notice: [%d] %s",
      response.statusCode(), response.bodyAsString()));
  }

}
