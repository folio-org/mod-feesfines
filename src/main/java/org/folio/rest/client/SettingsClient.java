package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static io.vertx.core.http.HttpMethod.GET;
import static java.lang.String.format;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.LocaleSettings;
import org.folio.rest.exception.http.HttpException;
import org.folio.rest.jaxrs.model.Setting;
import org.folio.rest.jaxrs.model.Settings;
import org.folio.util.StringUtil;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class SettingsClient extends OkapiClient {
  private static final Logger log = LogManager.getLogger(SettingsClient.class);

  public SettingsClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<LocaleSettings> getLocaleSettings() {
    String query = cqlAnd(cqlExactMatch("scope", "stripes-core.prefs.manage"),
      cqlExactMatch("key", "tenantLocaleSettings"));

    String url = format("/settings/entries?query=%s", StringUtil.urlEncode(query));
    return okapiGetAbs(url).send()
      .compose(response -> {
        int responseStatus = response.statusCode();
        if (responseStatus != 200) {
          String errorMessage = String.format(
            "Failed to find locale settings. Response: %d %s", responseStatus,
            response.bodyAsString());
          log.error(errorMessage);
          return failedFuture(new HttpException(GET, url, response));
        } else {
          try {
            Settings settings = objectMapper.readValue(response.bodyAsString(), Settings.class);

            JsonObject localeSettingsJsonObject = settings.getItems()
              .stream()
              .findFirst()
              .map(Setting::getValue)
              .filter(Map.class::isInstance)
              .map(Map.class::cast)
              .map(JsonObject::new)
              .orElse(null);

            if (localeSettingsJsonObject == null) {
              return failedFuture("Failed to find locale settings");
            } else {
              return succeededFuture(new LocaleSettings(
                localeSettingsJsonObject.getString("locale"),
                localeSettingsJsonObject.getString("timezone"),
                localeSettingsJsonObject.getString("currency")
              ));
            }
          } catch (Exception e) {
            log.error("Failed to parse response: {}", response.bodyAsString(), e);
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
