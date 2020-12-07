package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.exception.EntityNotFoundException;
import org.folio.rest.exception.FailedToMakeRequestException;
import org.folio.rest.jaxrs.model.Config;
import org.folio.rest.jaxrs.model.KvConfigurations;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;

public class ConfigurationClient extends OkapiClient {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationClient.class);

  private static final DateTimeZone DEFAULT_DATE_TIME_ZONE = DateTimeZone.UTC;
  private static final String TIMEZONE_KEY = "timezone";

  public ConfigurationClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<DateTimeZone> findTimeZone() {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();

    String query = cqlAnd(cqlExactMatch("module", "ORG"),
      cqlExactMatch("configName", "localeSettings"));

    try {
      query = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
    }
    catch (UnsupportedEncodingException e) {
      String errorMessage = "Failed to encode query: " + query;
      log.error(errorMessage);
      return failedFuture(new FailedToMakeRequestException(errorMessage));
    }

    String url = format("/configurations/entries?query=%s", query);
    okapiGetAbs(url).send(promise);

    return promise.future().compose(response -> {
      int responseStatus = response.statusCode();
      if (responseStatus != 200) {
        String errorMessage = String.format(
          "Failed to find time zone configuration. Response: %d %s", responseStatus,
          response.bodyAsString());
        log.error(errorMessage);
        return failedFuture(new EntityNotFoundException(errorMessage));
      } else {
        try {
          KvConfigurations kvConfigurations = objectMapper.readValue(response.bodyAsString(),
            KvConfigurations.class);

          final DateTimeZone timeZone = kvConfigurations.getConfigs().stream()
            .map(this::applyTimeZone)
            .findFirst()
            .orElse(DEFAULT_DATE_TIME_ZONE);

          return succeededFuture(timeZone);
        } catch (JsonProcessingException e) {
          log.error("Failed to parse response: " + response.bodyAsString());
          return failedFuture(e);
        }
      }
    });
  }

  private String cqlExactMatch(String index, String value) {
    return format("%s==\"%s\"", index, value);
  }

  private String cqlAnd(String left, String right) {
    return format("%s and %s", left, right);
  }

  private DateTimeZone applyTimeZone(Config config) {
    String value = config.getValue();
    return StringUtils.isBlank(value)
      ? DEFAULT_DATE_TIME_ZONE
      : parseDateTimeZone(value);
  }

  private DateTimeZone parseDateTimeZone(String value) {
    String timezone = new JsonObject(value).getString(TIMEZONE_KEY);
    return StringUtils.isBlank(timezone)
      ? DEFAULT_DATE_TIME_ZONE
      : DateTimeZone.forID(timezone);
  }
}
