package org.folio.rest.service.report.utils;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.util.UuidUtil.isUuid;

import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.client.ConfigurationClient;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.UserGroupsClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.repository.LostItemFeePolicyRepository;
import org.folio.rest.repository.OverdueFinePolicyRepository;
import org.folio.rest.service.LocationService;
import org.folio.rest.service.report.FinancialTransactionsDetailReportService;
import org.folio.rest.service.report.RefundReportService;
import org.folio.rest.service.report.context.HasUserInfo;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class LookupHelper {
  private static final Logger log = LogManager.getLogger(LookupHelper.class);

  private final LocationService locationService;

  private final ConfigurationClient configurationClient;
  private final InventoryClient inventoryClient;
  private final UsersClient usersClient;
  private final UserGroupsClient userGroupsClient;
  private final AccountRepository accountRepository;

  private final FeeFineActionRepository feeFineActionRepository;
  private final CirculationStorageClient circulationStorageClient;
  private final LostItemFeePolicyRepository lostItemFeePolicyRepository;
  private final OverdueFinePolicyRepository overdueFinePolicyRepository;

  public LookupHelper(Map<String, String> headers, Context context) {
    locationService = new LocationService(context.owner(), headers);

    configurationClient = new ConfigurationClient(context.owner(), headers);
    inventoryClient = new InventoryClient(context.owner(), headers);
    usersClient = new UsersClient(context.owner(), headers);
    userGroupsClient = new UserGroupsClient(context.owner(), headers);
    circulationStorageClient = new CirculationStorageClient(context.owner(), headers);

    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    accountRepository = new AccountRepository(context, headers);
    lostItemFeePolicyRepository = new LostItemFeePolicyRepository(context, headers);
    overdueFinePolicyRepository = new OverdueFinePolicyRepository(context, headers);
  }

  public <T extends HasUserInfo> Future<T> lookupUserForAccount(T ctx, Account account) {
    if (account == null) {
      return succeededFuture(ctx);
    }

    String userId = account.getUserId();
    if (!isUuid(userId)) {
      log.error("User ID {} is not a valid UUID - account {}", userId, account.getId());
      return succeededFuture(ctx);
    }

    if (ctx.getUsers().containsKey(userId)) {
      return succeededFuture(ctx);
    } else {
      return usersClient.fetchUserById(userId)
        .compose(user -> addUserToContext(ctx, user, account.getId(), userId))
        .otherwise(ctx);
    }
  }

  private <T extends HasUserInfo> Future<T> addUserToContext(T ctx, User user, String accountId,
    String userId) {

    if (user == null) {
      log.error("User not found - account {}, user {}", accountId, userId);
    } else {
      ctx.getUsers().put(user.getId(), user);
    }

    return succeededFuture(ctx);
  }

  public <T extends HasUserInfo> Future<T> lookupUserGroupForUser(T ctx, String accountId) {
    User user = ctx.getUserByAccountId(accountId);

    if (user == null || ctx.getUserGroupByAccountId(accountId) != null) {
      return succeededFuture(ctx);
    }

    return userGroupsClient.fetchUserGroupById(user.getPatronGroup())
      .map(userGroup -> ctx.getUserGroups().put(userGroup.getId(), userGroup))
      .map(ctx)
      .otherwise(ctx);
  }
}