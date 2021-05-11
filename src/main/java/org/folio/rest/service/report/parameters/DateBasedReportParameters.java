package org.folio.rest.service.report.parameters;

import org.joda.time.DateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class DateBasedReportParameters {
  private final DateTime rawStartDate;
  private final DateTime rawEndDate;
  private String startDate;
  private String endDate;
}
