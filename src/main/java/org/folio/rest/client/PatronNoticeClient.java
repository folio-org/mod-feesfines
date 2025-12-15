package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.HttpStatus.HTTP_OK;

import java.util.Map;

import org.folio.rest.jaxrs.model.PatronNotice;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class PatronNoticeClient extends OkapiClient {

  public PatronNoticeClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<Void> postPatronNotice(PatronNotice notice) {
    return okapiPostAbs("/patron-notice")
      .sendJson(notice)
      .compose(this::interpretResponse);
  }

  private Future<Void> interpretResponse(HttpResponse<Buffer> response) {
    return response.statusCode() == HTTP_OK.toInt()
      ? succeededFuture()
      : failedFuture(String.format("Failed to send patron notice: [%d] %s",
      response.statusCode(), response.bodyAsString()));
  }

}
