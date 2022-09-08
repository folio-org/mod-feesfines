package org.folio.rest.exception.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import lombok.Getter;

@Getter
public class HttpGetException extends HttpException {
  private final Class<?> objectType;

  public HttpGetException(String url, HttpResponse<Buffer> response, Class<?> objectType) {
    super(HttpMethod.GET, url, response);
    this.objectType = objectType;
  }
}
