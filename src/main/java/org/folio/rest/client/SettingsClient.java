package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static io.vertx.core.http.HttpMethod.GET;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.LocaleSettings;
import org.folio.rest.exception.http.HttpException;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class SettingsClient extends OkapiClient {
  private static final Logger log = LogManager.getLogger(SettingsClient.class);

  public SettingsClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<LocaleSettings> getLocaleSettings() {
    String url = "/locale";
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
            String localSettings = response.bodyAsString();
            if (localSettings == null) {
              return failedFuture("Failed to find locale settings");
            }

            JsonObject localeSettingsJsonObject = new JsonObject(localSettings);

            String locale = localeSettingsJsonObject.getString("locale");
            String timezone = localeSettingsJsonObject.getString("timezone");
            String currency = localeSettingsJsonObject.getString("currency");
            String numberingSystem = localeSettingsJsonObject.getString("numberingSystem");

            return succeededFuture(new LocaleSettings(locale, timezone, currency, numberingSystem));
          } catch (Exception e) {
            log.error("Failed to parse response: {}", response.bodyAsString(), e);
            return failedFuture(e);
          }
        }
      });
  }
}
