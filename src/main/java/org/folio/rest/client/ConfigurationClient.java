package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.LocaleSettings;
import org.folio.rest.exception.EntityNotFoundException;
import org.folio.rest.jaxrs.model.Config;
import org.folio.rest.jaxrs.model.KvConfigurations;
import org.folio.util.StringUtil;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;

public class ConfigurationClient extends OkapiClient {
  private static final Logger log = LogManager.getLogger(ConfigurationClient.class);

  private static final DateTimeZone DEFAULT_DATE_TIME_ZONE = DateTimeZone.UTC;
  private static final String TIMEZONE_KEY = "timezone";

  public ConfigurationClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<LocaleSettings> getLocaleSettings() {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();

    String query = cqlAnd(cqlExactMatch("module", "ORG"),
      cqlExactMatch("configName", "localeSettings"));

    String url = format("/configurations/entries?query=%s", StringUtil.urlEncode(query));
    okapiGetAbs(url).send(promise);

    return promise.future().compose(response -> {
      int responseStatus = response.statusCode();
      if (responseStatus != 200) {
        String errorMessage = String.format(
          "Failed to find locale configuration. Response: %d %s", responseStatus,
          response.bodyAsString());
        log.error(errorMessage);
        return failedFuture(new EntityNotFoundException(errorMessage));
      } else {
        try {
          KvConfigurations kvConfigurations = objectMapper.readValue(response.bodyAsString(),
            KvConfigurations.class);

          JsonObject localeSettingsJsonObject = kvConfigurations.getConfigs().stream()
            .findFirst()
            .map(Config::getValue)
            .map(JsonObject::new)
            .orElse(null);

          if (localeSettingsJsonObject == null) {
            return failedFuture("Failed to find locale configuration");
          } else {
            return succeededFuture(new LocaleSettings(
              localeSettingsJsonObject.getString("locale"),
              localeSettingsJsonObject.getString("timezone"),
              localeSettingsJsonObject.getString("currency")
            ));
          }
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
}
