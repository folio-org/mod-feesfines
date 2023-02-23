package org.folio.migration;

import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.rest.utils.ResourceClients.buildFeeFineActionsClient;
import static org.folio.test.support.EntityBuilder.buildServicePoint;
import static org.folio.test.support.matcher.constant.ServicePath.SERVICE_POINTS_PATH;
import static org.folio.test.support.matcher.constant.ServicePath.SERVICE_POINTS_USERS_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.ServicePoint;
import org.folio.rest.jaxrs.model.ServicePointsUser;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.utils.ResourceClient;
import org.folio.test.support.ApiTests;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class FeeFineActionMigrationTest extends ApiTests {

  private static final String CREATED_AT_KEY = "createdAt";
  private static final String FEE_FINE_ACTIONS_TABLE = "feefineactions";
  private static final String FALLBACK_SERVICE_POINT_ID = "00000000-0000-4000-8000-000000000000";
  private static final String DEFAULT_SERVICE_POINT_ID = "11111111-1111-4111-8111-111111111111";
  private static final String ANOTHER_SERVICE_POINT_ID = "22222222-2222-4222-8222-222222222222";
  private static final String FALLBACK_SERVICE_POINT_NAME = "Fallback service point name";
  private static final String DEFAULT_SERVICE_POINT_NAME = "Default service point name";
  private static final String ANOTHER_SERVICE_POINT_NAME = "Another service point name";
  private static final String USER_ID_WITH_DEFAULT_SERVICE_POINT = "fdb8ad73-1627-4cbe-977d-6de0155ad564";
  private static final String USER_ID_WITHOUT_DEFAULT_SERVICE_POINT = "48faabd1-0275-47a9-92d7-abee27b0471f";
  private static final String OWNER_NAME = "Test, Owner";

  private static final String MODULE_FROM = "mod-feesfines-18.0.0";
  private static final String MODULE_TO = "mod-feesfines-18.3.0";
  private static final String FALLBACK_SERVICE_POINT_ID_KEY = "fallbackServicePointId";
  private static final Parameter FALLBACK_SERVICE_POINT_ID_PARAMETER =
    new Parameter().withKey(FALLBACK_SERVICE_POINT_ID_KEY).withValue(FALLBACK_SERVICE_POINT_ID);

  private final ResourceClient feeFineActionsClient = buildFeeFineActionsClient();
  private StubMapping servicePointsStub;
  private StubMapping servicePointsUsersStub;

  @BeforeEach
  void beforeEach() {
    servicePointsStub = stubForServicePoints();
    servicePointsUsersStub = stubForServicePointUsers();
  }

  @AfterEach
  void afterEach() {
    removeAllFromTable(FEE_FINE_ACTIONS_TABLE);
  }

  @Test
  void createdAtIsNotCreatedWhenActionDidNotHaveItInitially() {
    String actionId = createAction(null, randomId());
    Response response = postTenant(MODULE_FROM, MODULE_TO, FALLBACK_SERVICE_POINT_ID_PARAMETER);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
    verifyCreatedAt(actionId, nullValue());
  }

  @Test
  void createdAtIsNotChangedWhenItAlreadyContainsFallbackServicePointId() {
    String actionId = createAction(FALLBACK_SERVICE_POINT_ID, randomId());
    Response response = postTenant(MODULE_FROM, MODULE_TO, FALLBACK_SERVICE_POINT_ID_PARAMETER);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
    verifyCreatedAt(actionId, FALLBACK_SERVICE_POINT_ID);
  }

  @Test
  void createdAtIsChangedToServicePointIdWhenItContainsServicePointName() {
    String actionId = createAction(ANOTHER_SERVICE_POINT_NAME, randomId());
    Response response = postTenant(MODULE_FROM, MODULE_TO, FALLBACK_SERVICE_POINT_ID_PARAMETER);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
    verifyCreatedAt(actionId, ANOTHER_SERVICE_POINT_ID);
  }

  @Test
  void createdAtIsChangedToDefaultServicePointIdOfAssociatedUser() {
    String actionId = createAction(OWNER_NAME, USER_ID_WITH_DEFAULT_SERVICE_POINT);
    Response response = postTenant(MODULE_FROM, MODULE_TO, FALLBACK_SERVICE_POINT_ID_PARAMETER);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
    verifyCreatedAt(actionId, DEFAULT_SERVICE_POINT_ID);
  }

  @Test
  void createdAtIsChangedToFallbackServicePointIdWhenAssociatedUserHasNoDefaultServicePoint() {
    String actionId = createAction(OWNER_NAME, USER_ID_WITHOUT_DEFAULT_SERVICE_POINT);
    Response response = postTenant(MODULE_FROM, MODULE_TO, FALLBACK_SERVICE_POINT_ID_PARAMETER);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
    verifyCreatedAt(actionId, FALLBACK_SERVICE_POINT_ID);
  }

  @Test
  void createdAtIsNotUpdatedWhenItAlreadyContainsValidUuid() {
    String actionId = createAction(ANOTHER_SERVICE_POINT_ID, randomId());
    Response response = postTenant(MODULE_FROM, MODULE_TO, FALLBACK_SERVICE_POINT_ID_PARAMETER);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
    verifyCreatedAt(actionId, ANOTHER_SERVICE_POINT_ID);
  }

  @Test
  void migrationFailsWhenFallbackServicePointIdIsMissing() {
    Response response = postTenant(MODULE_FROM, MODULE_TO, null);
    assertThat(response.getStatus(), is(SC_INTERNAL_SERVER_ERROR));
    verifyResponseMessage(response, "fallbackServicePointId was not found among tenantParameters");
  }

  @Test
  void migrationFailsWhenFallbackServicePointIdIsInvalid() {
    Parameter parameter = new Parameter()
      .withKey(FALLBACK_SERVICE_POINT_ID_KEY)
      .withValue("not a UUID");

    Response response = postTenant(MODULE_FROM, MODULE_TO, parameter);;
    assertThat(response.getStatus(), is(SC_INTERNAL_SERVER_ERROR));
    verifyResponseMessage(response, "fallbackServicePointId is not a valid UUID: not a UUID");
  }

  @Test
  void migrationFailsWhenFallbackServicePointDoesNotExist() {
    String servicePointId = randomId();
    Parameter parameter = new Parameter()
      .withKey(FALLBACK_SERVICE_POINT_ID_KEY)
      .withValue(servicePointId);

    Response response = postTenant(MODULE_FROM, MODULE_TO, parameter);;
    assertThat(response.getStatus(), is(SC_INTERNAL_SERVER_ERROR));
    verifyResponseMessage(response, "Fallback service point was not found by ID: " + servicePointId);
  }

  // We know that migration is skipped because we get 201 in response. Had it not been skipped, we
  // would have received a 500 due to a missing fallbackServicePointId.
  @Test
  void migrationIsSkippedWhenModuleFromIsNull() {
    Response response = postTenant(null, MODULE_TO, null);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
  }

  @Test
  void migrationIsSkippedWhenModuleToIsNull() {
    Response response = postTenant(MODULE_FROM, null, null);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
  }

  @Test
  void migrationIsSkippedWhenModuleToVersionIsTooLow() {
    Response response = postTenant(MODULE_FROM, "mod-feesfines-18.2.99", null);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
  }

  @Test
  void migrationIsSkippedWhenModuleFromVersionIsTooHigh() {
    Response response = postTenant("mod-feesfines-18.3.0", "mod-feesfines-18.4.0", null);
    assertThat(response.getStatus(), is(SC_NO_CONTENT));
  }

  @Test
  void migrationFailsWhenServicePointsApiIsUnreachable() {
    getOkapi().removeStub(servicePointsStub);
    Response response = postTenant(MODULE_FROM, MODULE_TO, FALLBACK_SERVICE_POINT_ID_PARAMETER);
    assertThat(response.getStatus(), is(SC_INTERNAL_SERVER_ERROR));
    verifyResponseMessage(response, "Resource not found");
  }

  @Test
  void migrationFailsWhenServicePointsUsersApiIsUnreachable() {
    getOkapi().removeStub(servicePointsUsersStub);
    createAction(OWNER_NAME, USER_ID_WITH_DEFAULT_SERVICE_POINT);
    Response response = postTenant(MODULE_FROM, MODULE_TO, FALLBACK_SERVICE_POINT_ID_PARAMETER);
    assertThat(response.getStatus(), is(SC_INTERNAL_SERVER_ERROR));
    verifyResponseMessage(response, "Resource not found");
  }

  private StubMapping stubForServicePoints() {
    ServicePoint fallbackServicePoint = buildServicePoint(FALLBACK_SERVICE_POINT_ID, FALLBACK_SERVICE_POINT_NAME);
    ServicePoint defaultServicePoint = buildServicePoint(DEFAULT_SERVICE_POINT_ID, DEFAULT_SERVICE_POINT_NAME);
    ServicePoint anotherServicePoint = buildServicePoint(ANOTHER_SERVICE_POINT_ID, ANOTHER_SERVICE_POINT_NAME);

    return stubForGet(SERVICE_POINTS_PATH,
      List.of(fallbackServicePoint, defaultServicePoint, anotherServicePoint),
      "servicepoints");
  }

  private StubMapping stubForServicePointUsers() {
    ServicePointsUser userWithDefaultServicePoint = buildServicePointUser(
      USER_ID_WITH_DEFAULT_SERVICE_POINT, DEFAULT_SERVICE_POINT_ID);

    ServicePointsUser userWithoutDefaultServicePoint = buildServicePointUser(
      USER_ID_WITHOUT_DEFAULT_SERVICE_POINT, null);

    return stubForGet(SERVICE_POINTS_USERS_PATH,
      List.of(userWithDefaultServicePoint, userWithoutDefaultServicePoint),
      "servicePointsUsers");
  }

  private StubMapping stubForGet(String url, Collection<Object> returnObjects, String collectionName) {
    JsonArray results = returnObjects.stream()
      .map(JsonObject::mapFrom)
      .collect(collectingAndThen(toList(), JsonArray::new));

    JsonObject response = new JsonObject()
      .put(collectionName, results)
      .put("totalRecords", results.size());

    return getOkapi().stubFor(WireMock.get(urlPathMatching(url))
      .willReturn(jsonResponse(response.encodePrettily(), SC_OK)));
  }

  private static Feefineaction buildAction(String createdAt, String userId) {
    return new Feefineaction()
      .withId(randomId())
      .withTypeAction("Test action type")
      .withAmountAction(new MonetaryValue(1.0))
      .withUserId(userId)
      .withDateAction(new Date())
      .withAccountId(randomId())
      .withBalance(new MonetaryValue(1.0))
      .withPaymentMethod("Cash")
      .withSource("test")
      .withNotify(false)
      .withTransactionInformation("-")
      .withCreatedAt(createdAt);
  }

  private static ServicePointsUser buildServicePointUser(String userId, String defaultServicePointId) {
    return new ServicePointsUser()
      .withId(randomId())
      .withUserId(userId)
      .withDefaultServicePointId(defaultServicePointId)
      .withServicePointsIds(defaultServicePointId == null ? null : singletonList(defaultServicePointId));
  }

  private static Response postTenant(String moduleFrom, String moduleTo,
    Parameter parameter) {

    TenantAttributes tenantAttributes = getTenantAttributes()
      .withModuleFrom(moduleFrom)
      .withModuleTo(moduleTo);

    Optional.ofNullable(parameter)
      .ifPresent(tenantAttributes.getParameters()::add);

    CompletableFuture<Response> future = new CompletableFuture<>();
    createTenant(tenantAttributes, future);

    return get(future);
  }
  
  private String createAction(String createdAt, String userId) {
    Feefineaction action = buildAction(createdAt, userId);
    get(pgClient.save(FEE_FINE_ACTIONS_TABLE, action.getId(), action));
    
    return action.getId();
  }

  private void verifyCreatedAt(String actionId, String createdAt) {
    verifyCreatedAt(actionId, is(createdAt));
  }

  private void verifyCreatedAt(String actionId, Matcher<Object> createdAtMatcher) {
    feeFineActionsClient.getById(actionId)
      .then()
      .body(CREATED_AT_KEY, createdAtMatcher);
  }

  private static void verifyResponseMessage(Response response, String message) {
    assertThat((String) response.getEntity(), containsString(message));
  }
}
