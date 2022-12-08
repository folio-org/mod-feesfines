package org.folio.rest.exception.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import lombok.Getter;

@Getter
public class HttpNotFoundException extends HttpException {
  private final Class<?> objectType;
  private final String objectId;

  public HttpNotFoundException(Class<?> objectType, String objectId, HttpMethod httpMethod,
    String url, HttpResponse<Buffer> response) {

    super(httpMethod, url, response);
    this.objectType = objectType;
    this.objectId = objectId;
  }

  @Override
  public String getMessage() {
    return String.format("%s %s was not found", objectType.getSimpleName(), objectId);
  }
}
