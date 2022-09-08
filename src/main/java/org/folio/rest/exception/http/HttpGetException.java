package org.folio.rest.exception.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import lombok.Getter;

@Getter
public class HttpGetException extends HttpException {

  public HttpGetException(String url, HttpResponse<Buffer> response) {
    super(HttpMethod.GET, url, response);
  }
}
