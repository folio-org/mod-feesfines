package org.folio.rest.service;

@FunctionalInterface
public interface RemainingCalculator {

  double calculate(double remainingAmount, double amount);
}
