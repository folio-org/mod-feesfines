package org.folio.rest.service.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.rest.domain.ActionRequest;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Metadata;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.tools.utils.MetadataUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ExtendWith(VertxExtension.class)
class ActionServiceTest {

  @ParameterizedTest(name = "{1}")
  @MethodSource("accountProvider")
  @DisplayName("Verify that metadata populated")
  void metadataUpdated(Account account, String name, Vertx vertx, VertxTestContext testContext) {
    try (MockedStatic<MetadataUtil> mockStatic = Mockito.mockStatic(MetadataUtil.class)) {
      mockStatic
          .when(() -> MetadataUtil.populateMetadata(any(Account.class), anyMap()))
          .thenThrow(new ReflectiveOperationException());

      testContext.verify(
          () -> {
            var headers = createOkapiHeaders();
            PayActionService payActionService =
                new PayActionService(headers, vertx.getOrCreateContext());

            ActionRequest request = createActionRequest();
            MonetaryValue amount = new MonetaryValue(BigDecimal.ZERO);

            payActionService.createFeeFineActionAndUpdateAccount(account, amount, request);

            assertThat(account.getMetadata(), notNullValue());
            assertThat(account.getMetadata().getUpdatedDate(), notNullValue());

            testContext.completeNow();
          });
    }
  }

  static Stream<Arguments> accountProvider() {
    return Stream.of(
      Arguments.of(createAccount(), "Metadata is null"),
      Arguments.of(createAccount().withMetadata(new Metadata()), "Empty metadata")
    );
  }

  private Map<String, String> createOkapiHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put(
        "x-okapi-token",
        "1.eyJ1c2VyX2lkIjoiNjlkOTE2OWQtMDZkYS00NjIyLTljMTgtMjg2OGJkNDZiNjBmIiwidGVuYW50IjoidGVzdF90ZW5hbnQiLCJzdWIiOiJhZG1pbiJ9.3");
    headers.put("x-okapi-tenant", "test_tenant");
    return headers;
  }

  private ActionRequest createActionRequest() {
    return new ActionRequest(
        Collections.singletonList("59e85c1d-0fbb-4e2e-821d-8ed47969f04e"),
        "1.004123456789",
        "STAFF : staff comment \\n PATRON : patron comment",
        "Check #12345",
        "c8c17551-6cf7-4f76-b668-15982e325248",
        "Folio, Tester",
        "Cash",
        false,
        null);
  }

  private static Account createAccount() {
    return new Account()
        .withId(UUID.randomUUID().toString())
        .withOwnerId(UUID.randomUUID().toString())
        .withUserId(UUID.randomUUID().toString())
        .withFeeFineId(UUID.randomUUID().toString())
        .withFeeFineType("book lost")
        .withFeeFineOwner("owner")
        .withAmount(7.77)
        .withRemaining(3.33)
        .withPaymentStatus(new PaymentStatus().withName("Outstanding"))
        .withStatus(new Status().withName("Open"));
  }
}
