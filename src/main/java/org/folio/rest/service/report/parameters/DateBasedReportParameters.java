package org.folio.rest.service.report.parameters;

import org.joda.time.DateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DateBasedReportParameters {
  private final DateTime rawStartDate;
  private final DateTime rawEndDate;
  private String startDate;
  private String endDate;

  public DateBasedReportParameters(DateTime rawStartDate, DateTime rawEndDate) {
    this.rawStartDate = rawStartDate;
    this.rawEndDate = rawEndDate;
  }
}
