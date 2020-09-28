package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import java.util.Map;
import org.folio.rest.jaxrs.model.PatronNotice;

public class PatronNoticeClient extends OkapiClient {

  public PatronNoticeClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<Void> postPatronNotice(PatronNotice notice) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    okapiPostAbs("/patron-notice").sendJson(notice, promise);

    return promise.future().compose(response -> response.statusCode() == 200 ?
      succeededFuture() :
      failedFuture("Failed to post patron notice. Returned status code: " + response.statusCode()));
  }
}
