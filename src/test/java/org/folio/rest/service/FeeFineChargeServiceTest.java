package org.folio.rest.service;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TENANT;
import static org.folio.rest.RestVerticle.OKAPI_HEADER_TOKEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;

import java.util.Map;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.test.support.ApiTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;

@ExtendWith(VertxExtension.class)
class FeeFineChargeServiceTest extends ApiTests {

  private static final Map<String, String> HEADERS = Map.of(
    ACCEPT, APPLICATION_JSON,
    OKAPI_HEADER_TENANT, TENANT_NAME,
    OKAPI_HEADER_TOKEN, OKAPI_TOKEN,
    OKAPI_URL_HEADER, getOkapiUrl());

  @BeforeEach
  void beforeEach() {
    removeAllFromTable(ACCOUNTS_TABLE);
    removeAllFromTable(FEE_FINE_ACTIONS_TABLE);
  }

  @SneakyThrows
  @Test
  void feeFineActionIsNotSavedWhenAccountSavingFailed(VertxTestContext testContext) {
    Account account = buildAccount();
    Feefineaction action = buildAction();

    // create account beforehand to cause error later when charging
    accountsClient.post(account)
      .then()
      .statusCode(201);

    new FeeFineChargeService(vertx.getOrCreateContext(), HEADERS)
      .chargeFeeFine(account, action)
      .onComplete(testContext.failing(failure -> testContext.verify(() -> {
        assertThat(failure.getMessage(), containsString(
          "duplicate key value violates unique constraint \"accounts_pkey\""));

        feeFineActionsClient.getAll()
          .then()
          .body("feefineactions", empty());

        testContext.completeNow();
      })));
  }

  @SneakyThrows
  @Test
  void accountIsNotSavedWhenFeeFineActionSavingFailed(VertxTestContext testContext) {
    Account account = buildAccount();
    Feefineaction action = buildAction();

    // create action beforehand to cause error later when charging
    feeFineActionsClient.post(action)
      .then()
      .statusCode(201);

    new FeeFineChargeService(vertx.getOrCreateContext(), HEADERS)
      .chargeFeeFine(account, action)
      .onComplete(testContext.failing(failure -> testContext.verify(() -> {
        assertThat(failure.getMessage(), containsString(
          "duplicate key value violates unique constraint \"feefineactions_pkey\""));

        accountsClient.getAll()
          .then()
          .body("accounts", empty());

        testContext.completeNow();
      })));
  }

  private static Feefineaction buildAction() {
    return new Feefineaction()
      .withId(randomId())
      .withAccountId(randomId())
      .withUserId(randomId());
  }

  private static Account buildAccount() {
    return new Account()
      .withAmount(new MonetaryValue(9.99))
      .withRemaining(new MonetaryValue(9.99))
      .withStatus(new Status().withName("Open"))
      .withPaymentStatus(new PaymentStatus().withName(PaymentStatus.Name.OUTSTANDING))
      .withUserId(randomId())
      .withFeeFineId(randomId())
      .withOwnerId(randomId())
      .withId(randomId());
  }

}
