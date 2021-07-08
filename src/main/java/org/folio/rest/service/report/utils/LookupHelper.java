package org.folio.rest.service.report.utils;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.client.ConfigurationClient;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.UserGroupsClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.repository.LostItemFeePolicyRepository;
import org.folio.rest.repository.OverdueFinePolicyRepository;
import org.folio.rest.service.LocationService;

import io.vertx.core.Context;

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
}
