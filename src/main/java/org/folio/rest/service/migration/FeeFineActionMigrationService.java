package org.folio.rest.service.migration;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.folio.rest.utils.MigrationHelper.shouldSkipMigration;
import static org.folio.util.UuidUtil.isUuid;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.ServicePoint;
import org.folio.rest.jaxrs.model.ServicePointsUser;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.repository.FeeFineActionRepository;

import io.vertx.core.Context;
import io.vertx.core.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

public class FeeFineActionMigrationService {

  private static final Logger log = LogManager.getLogger(FeeFineActionMigrationService.class);
  private static final String MODULE_VERSION_FOR_MIGRATION = "18.3.0";
  private static final String FALLBACK_SERVICE_POINT_ID_KEY = "fallbackServicePointId";
  private static final String FIND_INVALID_ACTIONS_QUERY_TEMPLATE =
    "SELECT ffa.jsonb FROM %s.feefineactions ffa" +
      " WHERE ffa.jsonb->>'createdAt' IS NOT NULL AND ffa.jsonb->>'createdAt' !~ " +
      "'[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[1-5][a-fA-F0-9]{3}-[89abAB][a-fA-F0-9]{3}-[a-fA-F0-9]{12}'";

  private final FeeFineActionRepository actionRepository;
  private final InventoryClient inventoryClient;

  public FeeFineActionMigrationService(Map<String, String> headers, Context context) {
    this.actionRepository = new FeeFineActionRepository(headers, context);
    this.inventoryClient = new InventoryClient(context.owner(), headers);
  }

  public Future<Void> doMigration(TenantAttributes tenantAttributes) {
    if (shouldSkipMigration(tenantAttributes, MODULE_VERSION_FOR_MIGRATION)) {
      return succeededFuture();
    }

    log.info("doMigration:: starting fee/fine action migration");

    return validateFallbackServicePointId(getFallbackServicePointId(tenantAttributes))
      .map(MigrationContext::new)
      .compose(this::fetchServicePoints)
      .compose(this::validateFallbackServicePoint)
      .compose(this::fetchActions)
      .compose(this::processActions)
      .compose(this::updateProcessedActions)
      .onSuccess(r -> log.info("doMigration:: migration is complete"))
      .onFailure(t -> log.warn("doMigration:: migration failed", t))
      .mapEmpty();
  }

  private static String getFallbackServicePointId(TenantAttributes tenantAttributes) {
    return tenantAttributes.getParameters()
      .stream()
      .filter(p -> FALLBACK_SERVICE_POINT_ID_KEY.equals(p.getKey()))
      .findFirst()
      .map(Parameter::getValue)
      .orElse(null);
  }

  private static Future<String> validateFallbackServicePointId(String fallbackServicePointId) {
    log.info("validateFallbackServicePointId:: fallbackServicePointId={}", fallbackServicePointId);

    if (fallbackServicePointId == null) {
      return failedFuture("fallbackServicePointId was not found among tenantParameters");
    }

    if (!isUuid(fallbackServicePointId)) {
      return failedFuture("fallbackServicePointId is not a valid UUID: " + fallbackServicePointId);
    }

    return succeededFuture(fallbackServicePointId);
  }

  private Future<MigrationContext> fetchServicePoints(MigrationContext context) {
    return inventoryClient.getAllServicePoints()
      .map(context::withServicePoints);
  }

  private Future<MigrationContext> validateFallbackServicePoint(MigrationContext context) {
    final String fallbackServicePointId = context.getFallbackServicePointId();

    boolean fallbackServicePointExists = context.getServicePoints()
      .stream()
      .map(ServicePoint::getId)
      .anyMatch(fallbackServicePointId::equals);

    return fallbackServicePointExists
      ? succeededFuture(context)
      : failedFuture("Fallback service point was not found by ID: " + fallbackServicePointId);
  }

  private Future<MigrationContext> fetchActions(MigrationContext context) {
    String query = format(FIND_INVALID_ACTIONS_QUERY_TEMPLATE, actionRepository.getSchemaName());

    return actionRepository.findByQuery(query)
      .onSuccess(r -> log.info("findFeeFineActions:: {} fee/fine action(s) found", r.size()))
      .map(LinkedList::new)
      .map(context::withUnprocessedActions);
  }

  private Future<MigrationContext> processActions(MigrationContext context) {
    if (context.hasNoUnprocessedActions()) {
      log.info("processActions:: found no fee/fine actions to migrate");
      return succeededFuture(context);
    }

    findServicePointIdsByName(context);

    return findDefaultServicePointsForUsers(context);
  }

  private void findServicePointIdsByName(MigrationContext context) {
    Map<String, String> servicePointNamesToIds = context.getServicePoints()
      .stream()
      .collect(toMap(ServicePoint::getName, ServicePoint::getId));

    processActions(context, action -> servicePointNamesToIds.get(action.getCreatedAt()));
  }

  private Future<MigrationContext> findDefaultServicePointsForUsers(MigrationContext context) {
    if (context.hasNoUnprocessedActions()) {
      log.debug("findDefaultServicePointsForUsers:: no fee/fine actions to process");
      return succeededFuture(context);
    }

    Set<String> userIds = context.getUnprocessedActions().stream()
      .map(Feefineaction::getUserId)
      .collect(toSet());

    return inventoryClient.getServicePointsUsers(userIds)
      .map(users -> processServicePointUsers(context, users));
  }

  private MigrationContext processServicePointUsers(MigrationContext context,
    Collection<ServicePointsUser> users) {

    Map<String, String> userIdToServicePointId = users.stream()
      .filter(user -> isUuid(user.getDefaultServicePointId()))
      .collect(toMap(ServicePointsUser::getUserId, ServicePointsUser::getDefaultServicePointId));

    processActions(context, action -> userIdToServicePointId.getOrDefault(action.getUserId(),
      context.getFallbackServicePointId()));

    return context;
  }

  private static void processActions(MigrationContext context,
    Function<Feefineaction, String> actionToServicePointIdMapper) {

    for(var iterator = context.getUnprocessedActions().iterator(); iterator.hasNext();) {
      Feefineaction action = iterator.next();
      String servicePointId = actionToServicePointIdMapper.apply(action);
      if (servicePointId != null) {
        action.setCreatedAt(servicePointId);
        context.getProcessedActions().add(action);
        iterator.remove();
      }
    }
  }

  private Future<MigrationContext> updateProcessedActions(MigrationContext context) {
    return actionRepository.updateBatch(context.getProcessedActions())
      .map(context);
  }

  @With
  @Getter
  @AllArgsConstructor
  @RequiredArgsConstructor
  private static class MigrationContext {
    private final String fallbackServicePointId;
    private Collection<ServicePoint> servicePoints;
    private LinkedList<Feefineaction> unprocessedActions = new LinkedList<>();
    private LinkedList<Feefineaction> processedActions = new LinkedList<>();

    public boolean hasNoUnprocessedActions() {
      return unprocessedActions.isEmpty();
    }
  }
}
