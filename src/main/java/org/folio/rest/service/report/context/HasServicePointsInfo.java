package org.folio.rest.service.report.context;

import java.util.Map;

import org.folio.rest.jaxrs.model.ServicePoint;

public interface HasServicePointsInfo {
  Map<String, ServicePoint> getServicePoints();
}
