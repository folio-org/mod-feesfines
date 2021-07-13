package org.folio.rest.service.report.parameters;

import java.util.List;

import org.joda.time.DateTime;

import lombok.Getter;

@Getter
public class FinancialTransactionsDetailReportParameters extends DateBasedReportParameters {
  private final String feeFineOwner;
  private final List<String> createdAt;

  public FinancialTransactionsDetailReportParameters(DateTime rawStartDate, DateTime rawEndDate,
    String feeFineOwner, List<String> createdAt) {

    super(rawStartDate, rawEndDate);
    this.feeFineOwner = feeFineOwner;
    this.createdAt = createdAt;
  }
}
