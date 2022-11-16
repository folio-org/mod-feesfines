package org.folio.rest.exception.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;

public class HttpNotFoundException extends HttpException {

  public HttpNotFoundException(HttpMethod httpMethod, String url, HttpResponse<Buffer> response) {
    super(httpMethod, url, response);
  }
}
