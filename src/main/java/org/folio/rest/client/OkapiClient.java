package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.folio.util.UuidUtil.isUuid;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.folio.rest.exception.http.HttpGetByIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OkapiClient {
  protected static final Logger log = LoggerFactory.getLogger(OkapiClient.class);
  private static final String OKAPI_URL_HEADER = "x-okapi-url";
  protected static final ObjectMapper objectMapper = new ObjectMapper();

  private final WebClient webClient;
  private final String okapiUrl;
  private final String tenant;
  private final String token;

  OkapiClient(Vertx vertx, Map<String, String> okapiHeaders) {
    this.webClient = WebClientProvider.getWebClient(vertx);
    okapiUrl = okapiHeaders.get(OKAPI_URL_HEADER);
    tenant = okapiHeaders.get(OKAPI_HEADER_TENANT);
    token = okapiHeaders.get(OKAPI_HEADER_TOKEN);
  }

  HttpRequest<Buffer> okapiGetAbs(String path) {
    return webClient.getAbs(okapiUrl + path)
      .putHeader(OKAPI_HEADER_TENANT, tenant)
      .putHeader(OKAPI_URL_HEADER, okapiUrl)
      .putHeader(OKAPI_HEADER_TOKEN, token)
      .putHeader(ACCEPT, APPLICATION_JSON);
  }

  HttpRequest<Buffer> okapiPostAbs(String path) {
    return webClient.postAbs(okapiUrl + path)
      .putHeader(ACCEPT, APPLICATION_JSON)
      .putHeader(OKAPI_HEADER_TENANT, tenant)
      .putHeader(OKAPI_URL_HEADER, okapiUrl)
      .putHeader(OKAPI_HEADER_TOKEN, token);
  }

  public <T> Future<T> getById(String resourcePath, String id, Class<T> objectType) {
    Optional<String> validationError = validateGetByIdArguments(resourcePath, id, objectType);
    if (validationError.isPresent()) {
      String errorMessage = validationError.get();
      log.error(errorMessage);
      return failedFuture(new IllegalArgumentException(errorMessage));
    }

    final String url = resourcePath + "/" + id;
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    okapiGetAbs(url).send(promise);

    return promise.future().compose(response -> {
      int responseStatus = response.statusCode();
      if (responseStatus != 200) {
        final String errorMessage = format("Failed to get %s by ID %s. Response status code: %s",
          objectType.getSimpleName(), id, responseStatus);
        log.error(errorMessage);
        return failedFuture(new HttpGetByIdException(url, response, objectType, id));
      }
      try {
        T object = objectMapper.readValue(response.bodyAsString(), objectType);
        return succeededFuture(object);
      } catch (IOException exception) {
        final String errorMessage = format("Failed to parse response from %s. Response body: %s",
          url, response.bodyAsString());
        log.error(errorMessage);
        return failedFuture(errorMessage);
      }
    });
  }

  private static <T> Optional<String> validateGetByIdArguments(String path, String id,
    Class<T> objectType) {

    String errorMessage = null;

    if (objectType == null) {
      errorMessage = "Requested object type is null";
    } else if (isBlank(path)) {
      errorMessage = "Invalid resource path for " + objectType.getSimpleName();
    } else if (!isUuid(id)) {
      errorMessage = "Invalid UUID: " + id;
    }

    return Optional.ofNullable(errorMessage);
  }
}
