package org.folio.rest.service.report.parameters;

import java.util.List;

import org.joda.time.DateTime;

import lombok.Getter;

@Getter
public class CashDrawerReconciliationReportParameters extends DateBasedReportParameters {
  private final String createdAt;
  private final List<String> sources;

  public CashDrawerReconciliationReportParameters(DateTime rawStartDate, DateTime rawEndDate,
    String createdAt, List<String> sources) {

    super(rawStartDate, rawEndDate);
    this.createdAt = createdAt;
    this.sources = sources;
  }
}
