package org.folio.rest.utils;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.tools.utils.MetadataUtil;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MetadataHelper {

  private static final Logger log = LogManager.getLogger(MetadataHelper.class);

  public static <T> void populateMetadata(T entity, Map<String, String> headers) {
    try {
      MetadataUtil.populateMetadata(entity, headers);
    } catch (Exception e) {
      log.error("Failed to populate Metadata for {}: {}",
        entity.getClass().getSimpleName(), e.getMessage());
    }
  }
}
