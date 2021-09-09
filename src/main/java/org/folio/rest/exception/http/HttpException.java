package org.folio.rest.exception.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HttpException extends RuntimeException {
  private final HttpMethod httpMethod;
  private final String url;
  private final int responseStatus;
  private final String responseBody;

  public HttpException(HttpMethod httpMethod, String url, HttpResponse<Buffer> response) {
    this.httpMethod = httpMethod;
    this.url = url;
    this.responseStatus = response.statusCode();
    this.responseBody = response.bodyAsString();
  }

  @Override
  public String getMessage() {
    return String.format("%s %s failed: [%d] %s", httpMethod, url, responseStatus, responseBody);
  }
}
