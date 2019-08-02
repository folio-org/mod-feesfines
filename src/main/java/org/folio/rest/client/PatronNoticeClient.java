package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;

import java.util.Map;

import org.folio.rest.jaxrs.model.PatronNotice;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class PatronNoticeClient {

  private static final String OKAPI_URL_HEADER = "x-okapi-url";

  private WebClient webClient;
  private String okapiUrl;
  private String tenant;
  private String token;

  public PatronNoticeClient(WebClient webClient, Map<String, String> okapiHeaders) {
    this.webClient = webClient;
    okapiUrl = okapiHeaders.get(OKAPI_URL_HEADER);
    tenant = okapiHeaders.get(OKAPI_HEADER_TENANT);
    token = okapiHeaders.get(OKAPI_HEADER_TOKEN);
  }

  public Future<Void> postPatronNotice(PatronNotice notice) {
    Future<HttpResponse<Buffer>> future = Future.future();
    webClient.postAbs(okapiUrl + "/patron-notice")
      .putHeader(ACCEPT, APPLICATION_JSON)
      .putHeader(OKAPI_HEADER_TENANT, tenant)
      .putHeader(OKAPI_URL_HEADER, okapiUrl)
      .putHeader(OKAPI_HEADER_TOKEN, token)
        .sendJson(notice, future);

    return future.compose(response -> response.statusCode() == 200 ?
      succeededFuture() :
      failedFuture("Failed to post patron notice. Returned status code: " + response.statusCode()));
  }
}
