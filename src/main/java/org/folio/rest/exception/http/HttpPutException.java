package org.folio.rest.exception.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;

public class HttpPutException extends HttpException {

  public HttpPutException(String url, HttpResponse<Buffer> response) {
    super(HttpMethod.PUT, url, response);
  }
}
