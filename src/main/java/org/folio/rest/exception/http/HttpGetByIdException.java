package org.folio.rest.exception.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import lombok.Getter;

@Getter
public class HttpGetByIdException extends HttpException {
  private final Class<?> objectType;
  private final String id;

  public HttpGetByIdException(String url, HttpResponse<Buffer> response, Class<?> objectType, String id) {
    super(HttpMethod.GET, url, response);
    this.objectType = objectType;
    this.id = id;
  }

  @Override
  public String getMessage() {
    return String.format("Failed to fetch %s %s: [%d] %s", objectType.getSimpleName(), id,
      getResponseStatus(), getResponseBody());
  }
}
