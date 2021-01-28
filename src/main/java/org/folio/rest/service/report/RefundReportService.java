package org.folio.rest.service.report;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.utils.AccountHelper.PATRON_COMMENTS_KEY;
import static org.folio.rest.utils.AccountHelper.STAFF_COMMENTS_KEY;
import static org.folio.rest.utils.AccountHelper.parseFeeFineComments;
import static org.folio.util.UuidUtil.isUuid;
import static org.joda.time.DateTimeZone.UTC;
import static org.apache.commons.lang.StringUtils.defaultString;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.folio.rest.client.ConfigurationClient;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.UserGroupsClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.LocaleSettings;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.RefundReport;
import org.folio.rest.jaxrs.model.RefundReportEntry;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

public class RefundReportService {
  private static final Logger log = LoggerFactory.getLogger(RefundReportService.class);

  private static final int REPORT_ROWS_LIMIT = 1_000_000;
  private static final String MULTIPLE_MESSAGE = "Multiple";
  private static final String SEE_FEE_FINE_DETAILS_PAGE_MESSAGE = "See Fee/fine details page";
  private static final String REFUNDED_TO_PATRON = "Refunded to patron";
  private static final String REFUNDED_TO_BURSAR = "Refunded to Bursar";
  private static final LocaleSettings FALLBACK_LOCALE_SETTINGS =
    new LocaleSettings(Locale.US.toLanguageTag(), UTC.getID(),
      Currency.getInstance(Locale.US).getCurrencyCode());

  private final ConfigurationClient configurationClient;
  private final InventoryClient inventoryClient;
  private final UsersClient usersClient;
  private final UserGroupsClient userGroupsClient;
  private final FeeFineActionRepository feeFineActionRepository;
  private final AccountRepository accountRepository;

  private DateTimeZone timeZone;
  private DateTimeFormatter dateTimeFormatter;
  private Currency currency;

  public RefundReportService(Map<String, String> headers, Context context) {
    configurationClient = new ConfigurationClient(context.owner(), headers);
    inventoryClient = new InventoryClient(context.owner(), headers);
    usersClient = new UsersClient(context.owner(), headers);
    userGroupsClient = new UserGroupsClient(context.owner(), headers);
    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    accountRepository = new AccountRepository(context, headers);
  }

  public Future<RefundReport> buildReport(DateTime startDate, DateTime endDate,
    List<String> ownerIds) {

    return configurationClient.getLocaleSettings()
      .recover(throwable -> succeededFuture(FALLBACK_LOCALE_SETTINGS))
      .compose(localeSettings -> buildReportWithLocale(startDate, endDate, ownerIds,
        localeSettings));
  }

  private Future<RefundReport> buildReportWithLocale(DateTime startDate, DateTime endDate,
    List<String> ownerIds, LocaleSettings localeSettings) {

    setUpLocale(localeSettings);

    String startDateTimeFormatted = startDate
      .withTimeAtStartOfDay()
      .withZoneRetainFields(timeZone)
      .withZone(UTC)
      .toString(ISODateTimeFormat.dateTime());

    String endDateTimeFormatted = endDate
      .withTimeAtStartOfDay()
      .plusDays(1)
      .withZoneRetainFields(timeZone)
      .withZone(UTC)
      .toString(ISODateTimeFormat.dateTime());

    log.info("Building refund report with parameters: startDate={}, endDate={}, ownerIds={}, tz={}",
      startDate.toDateTimeISO(), endDate.toDateTimeISO(), ownerIds, timeZone);

    RefundReportContext ctx = new RefundReportContext().withTimeZone(timeZone);

    return feeFineActionRepository
      .findActionsByTypeForPeriodAndOwners(REFUND, startDateTimeFormatted, endDateTimeFormatted,
        ownerIds, REPORT_ROWS_LIMIT)
      .map(RefundReportService::toRefundDataMap)
      .map(ctx::withRefunds)
      .compose(this::processAllRefundActions)
      .map(this::buildReportFromContext);
  }

  private RefundReport buildReportFromContext(RefundReportContext ctx) {
    return new RefundReport().withReportData(ctx.refunds.values().stream()
      .map(refundData -> refundData.reportEntry)
      .collect(Collectors.toList()));
  }

  private Future<RefundReportContext> processAllRefundActions(RefundReportContext ctx) {
    return ctx.refunds.values().stream().reduce(succeededFuture(ctx),
        (f, r) -> f.compose(result -> processSingleRefundAction(ctx, r.refundAction)),
        (a, b) -> succeededFuture(ctx));
  }

  private Future<RefundReportContext> processSingleRefundAction(RefundReportContext ctx,
    Feefineaction refundAction) {

    String accountId = refundAction.getAccountId();

    if (ctx.processedAccounts.contains(accountId)) {
      return succeededFuture(ctx);
    }

    return lookupAccount(ctx, refundAction)
      .compose(r -> lookupItemForAccount(ctx, accountId))
      .compose(r -> lookupInstanceForAccount(ctx, accountId))
      .compose(r -> lookupUserForAccount(ctx, accountId))
      .compose(r -> lookupUserGroupForUser(ctx, accountId))
      .compose(r -> lookupFeeFineActionsForAccount(ctx, accountId))
      .map(actions -> processAccount(ctx, accountId, actions));
  }

  private RefundReportContext processAccount(RefundReportContext ctx,
    String accountId, List<Feefineaction> accountFeeFineActions) {

    accountFeeFineActions.forEach(action -> processAccountAction(ctx, accountId, action));
    ctx.markAccountProcessed(accountId);

    return ctx;
  }

  private void processAccountAction(RefundReportContext ctx,
    String accountId, Feefineaction feeFineAction) {

    log.info("Processing fee/fine action {}, account {}", feeFineAction.getId(), accountId);

    AccountContextData accountData = ctx.accounts.get(accountId);
    AccountProcessingContext accountCtx = accountData.processingContext;

    if (actionIsOfType(feeFineAction, PAY)) {
      if (feeFineAction.getAmountAction() != null) {
        accountCtx.setPaidAmount(accountCtx.paidAmount.add(
          new MonetaryValue(feeFineAction.getAmountAction())));
        accountCtx.paymentMethods.add(feeFineAction.getPaymentMethod());
        accountCtx.paymentTransactionInfo.add(feeFineAction.getTransactionInformation());
      } else {
        log.error("Payment amount is null - fee/fine action {}, account {}", feeFineAction.getId(),
          accountId);
      }
    }
    else if (actionIsOfType(feeFineAction, TRANSFER)) {
      if (feeFineAction.getAmountAction() != null) {
        accountCtx.setTransferredAmount(accountCtx.transferredAmount.add(
          new MonetaryValue(feeFineAction.getAmountAction())));
        accountCtx.transferAccounts.add(feeFineAction.getPaymentMethod());
      } else {
        log.error("Transfer amount is null - fee/fine action {}, account {}", feeFineAction.getId(),
          accountId);
      }
    }
    else if (actionIsOfType(feeFineAction, REFUND)) {
      RefundData refundData = ctx.refunds.get(feeFineAction.getId());
      RefundReportEntry reportEntry = refundData.reportEntry;

      Account account = ctx.getAccountById(accountId);
      User user = ctx.getUserByAccountId(accountId);
      UserGroup userGroup = ctx.getUserGroupByAccountId(accountId);
      Item item = ctx.getItemByAccountId(accountId);

      reportEntry
        .withFeeFineId(accountId)
        .withRefundDate(formatDate(feeFineAction.getDateAction(), ctx.timeZone))
        .withRefundAmount(formatMonetaryValue(feeFineAction.getAmountAction()))
        .withRefundAction(feeFineAction.getTypeAction())
        .withRefundReason(feeFineAction.getPaymentMethod())
        .withStaffInfo(getStaffInfo(feeFineAction.getComments()))
        .withPatronInfo(getPatronInfo(feeFineAction.getComments()));

      if (user != null) {
        reportEntry
          .withPatronName(formatName(user))
          .withPatronBarcode(user.getBarcode())
          .withPatronId(user.getId());
      } else {
        log.error("Refund report - user is null, refund action {}", feeFineAction.getId());
      }

      if (userGroup != null) {
        reportEntry.withPatronGroup(userGroup.getGroup());
      } else {
        log.error("Refund report - user group is null, refund action {}", feeFineAction.getId());
      }

      if (account != null) {
        reportEntry
          .withFeeFineType(account.getFeeFineType())
          .withBilledAmount(formatMonetaryValue(account.getAmount()))
          .withDateBilled(formatDate(account.getMetadata().getCreatedDate(), ctx.timeZone))
          .withFeeFineOwner(account.getFeeFineOwner());
      } else {
        log.error("Refund report - account is null, refund action {}", feeFineAction.getId());
      }

      if (accountCtx != null) {
        if (isRefundedToPatron(feeFineAction)){
          withPaymentInfo(reportEntry, accountCtx);
        } else if (isRefundedToBursar(feeFineAction)) {
          withTransferInfo(reportEntry, accountCtx);
        } else {
          withPaymentAndTransferInfo(reportEntry, accountCtx);
        }
      } else {
        log.error("Refund report - account ctx is null, refund action {}", feeFineAction.getId());
      }

      if (item != null) {
        reportEntry
          .withItemBarcode(getItemBarcode(ctx, accountId))
          .withInstance(accountData.instance);
      } else {
        log.info("Refund report - item is null, refund action {}", feeFineAction.getId());
      }

      ctx.refunds.put(feeFineAction.getId(), refundData.withReportEntry(reportEntry));
    }
  }

  private Future<RefundReportContext> lookupAccount(RefundReportContext ctx,
    Feefineaction refundAction) {

    String accountId = refundAction.getAccountId();

    if (!isUuid(accountId)) {
      String message = format("Account ID is not a valid UUID in fee/fine action %s",
        refundAction.getId());
      log.error(message);
      return succeededFuture(ctx);
    }

    return accountRepository.getAccountById(accountId)
      .map(account -> addAccountContextData(ctx, account, accountId))
      .map(ctx)
      .otherwise(ctx);
  }

  private AccountContextData addAccountContextData(RefundReportContext ctx,
    Account account, String accountId) {

    if (account == null) {
      String message = format("Account %s not found", accountId);
      log.error(message);
      return null;
    }

    return ctx.accounts.computeIfAbsent(accountId,
      key -> new AccountContextData().withAccount(account));
  }

  private Future<RefundReportContext> lookupItemForAccount(RefundReportContext ctx,
    String accountId) {

    Account account = ctx.getAccountById(accountId);
    if (account == null) {
      return succeededFuture(ctx);
    }

    String itemId = account.getItemId();
    if (!isUuid(itemId)) {
      log.info("Item ID is not a valid UUID - account {}", accountId);
      return succeededFuture(ctx);
    }
    else {
      if (ctx.items.containsKey(itemId)) {
        return succeededFuture(ctx);
      }
      else {
        return inventoryClient.getItemById(itemId)
          .map(item -> addItemToContext(ctx, item, accountId, itemId))
          .map(ctx)
          .otherwise(ctx);
      }
    }
  }

  private RefundReportContext addItemToContext(RefundReportContext ctx, Item item,
    String accountId, String itemId) {

    if (item == null) {
      log.info("Item not found - account {}, item {}", accountId, itemId);
    } else {
      ctx.items.put(itemId, item);
    }

    return ctx;
  }

  private Future<RefundReportContext> lookupInstanceForAccount(RefundReportContext ctx,
    String accountId) {

    Account account = ctx.getAccountById(accountId);
    if (account == null) {
      return succeededFuture(ctx);
    }

    Item item = ctx.getItemByAccountId(accountId);
    if (item == null) {
      return succeededFuture(ctx);
    }

    String holdingsRecordId = item.getHoldingsRecordId();
    if (!isUuid(holdingsRecordId)) {
      log.info("Holdings record ID {} is not a valid UUID - account {}", holdingsRecordId,
        accountId);
      return succeededFuture(ctx);
    }

    return inventoryClient.getHoldingById(holdingsRecordId)
      .map(HoldingsRecord::getInstanceId)
      .compose(inventoryClient::getInstanceById)
      .map(instance -> ctx.accounts.put(accountId, ctx.accounts.get(accountId)
        .withInstance(instance.getTitle())))
      .map(ctx)
      .otherwise(throwable -> {
        log.error("Failed to find instance for account {}, holdingsRecord is {}", accountId,
          holdingsRecordId);
        return ctx;
      });
  }

  private Future<RefundReportContext> lookupUserForAccount(RefundReportContext ctx,
    String accountId) {

    Account account = ctx.getAccountById(accountId);
    if (account == null) {
      return succeededFuture(ctx);
    }

    String userId = account.getUserId();
    if (!isUuid(userId)) {
      log.error("User ID {} is not a valid UUID - account {}", userId, accountId);
      return succeededFuture(ctx);
    }

    if (ctx.users.containsKey(userId)) {
      return succeededFuture(ctx);
    }
    else {
      return usersClient.fetchUserById(userId)
        .compose(user -> addUserToContext(ctx, user, accountId, userId))
        .otherwise(ctx);
    }
  }

  private Future<RefundReportContext> addUserToContext(RefundReportContext ctx, User user,
    String accountId, String userId) {

    if (user == null) {
      log.error("User not found - account {}, user {}", accountId, userId);
      return succeededFuture(ctx);
    } else {
      ctx.users.put(userId, user);
    }

    return succeededFuture(ctx);
  }

  private Future<RefundReportContext> lookupUserGroupForUser(RefundReportContext ctx,
    String accountId) {

    User user = ctx.getUserByAccountId(accountId);

    if (user == null || ctx.getUserGroupByAccountId(accountId) != null) {
      return succeededFuture(ctx);
    }

    return userGroupsClient.fetchUserGroupById(user.getPatronGroup())
      .map(userGroup -> ctx.userGroups.put(userGroup.getId(), userGroup))
      .map(ctx)
      .otherwise(ctx);
  }

  private Future<List<Feefineaction>> lookupFeeFineActionsForAccount(RefundReportContext ctx,
    String accountId) {

    AccountContextData accountContextData = ctx.getAccountContextById(accountId);
    if (accountContextData == null) {
      return succeededFuture(new ArrayList<>());
    }

    return feeFineActionRepository.findActionsOfTypesForAccount(accountId,
      List.of(REFUND, PAY, TRANSFER))
      .map(ctx.accounts.get(accountId)::withActions)
      .map(AccountContextData::getActions);
  }

  private void setUpLocale(LocaleSettings localeSettings) {
    timeZone = localeSettings.getDateTimeZone();
    dateTimeFormatter = DateTimeFormat.forPattern(DateTimeFormat.patternForStyle("SS",
      Locale.forLanguageTag(localeSettings.getLocale())));
    currency = Currency.getInstance(localeSettings.getCurrency());
  }

  private static boolean actionIsOfType(Feefineaction feeFineAction, Action action) {
    return List.of(action.getFullResult(), action.getPartialResult())
      .contains(feeFineAction.getTypeAction());
  }

  private static Map<String, RefundData> toRefundDataMap(List<Feefineaction> refundActions) {
    return refundActions.stream()
      .collect(Collectors.toMap(Feefineaction::getId, RefundData::new, (a, b) -> b,
        LinkedHashMap::new));
  }

  private static String singleOrDefaultMessage(List<String> values, String message) {
    List<String> uniqueValues = new ArrayList<>(new HashSet<>(values));

    if (uniqueValues.size() == 0) {
      return "";
    }

    if (uniqueValues.size() == 1) {
      return uniqueValues.get(0);
    }

    return message;
  }

  private static String getStaffInfo(String comments) {
    return defaultString(parseFeeFineComments(comments).get(STAFF_COMMENTS_KEY));
  }

  private static String getPatronInfo(String comments) {
    return defaultString(parseFeeFineComments(comments).get(PATRON_COMMENTS_KEY));
  }

  private static String getItemBarcode(RefundReportContext ctx, String accountId) {
    Item item = ctx.getItemByAccountId(accountId);

    if (item == null) {
      return "";
    }

    return item.getBarcode();
  }

  private String formatMonetaryValue(Double value) {
    return new MonetaryValue(value, currency).toString();
  }

  private String formatDate(Date date, DateTimeZone timeZone) {
    return new DateTime(date).withZone(timeZone).toString(dateTimeFormatter);
  }

  private boolean isRefundedToPatron(Feefineaction feeFineAction) {
    return REFUNDED_TO_PATRON.equals(feeFineAction.getTransactionInformation());
  }

  private boolean isRefundedToBursar(Feefineaction feeFineAction) {
    return REFUNDED_TO_BURSAR.equals(feeFineAction.getTransactionInformation());
  }

  private void withPaymentInfo(RefundReportEntry reportEntry, AccountProcessingContext accountCtx) {
    reportEntry.withPaidAmount(accountCtx.paidAmount.toString())
      .withPaymentMethod(singleOrDefaultMessage(accountCtx.paymentMethods, MULTIPLE_MESSAGE))
      .withTransactionInfo(singleOrDefaultMessage(accountCtx.paymentTransactionInfo,
        SEE_FEE_FINE_DETAILS_PAGE_MESSAGE));;
  }

  private void withTransferInfo(RefundReportEntry reportEntry, AccountProcessingContext accountCtx) {
    reportEntry.withTransferredAmount(accountCtx.transferredAmount.toString())
      .withTransferAccount(singleOrDefaultMessage(accountCtx.transferAccounts, MULTIPLE_MESSAGE));
  }

  private void withPaymentAndTransferInfo(RefundReportEntry reportEntry, AccountProcessingContext accountCtx) {
    withPaymentInfo(reportEntry, accountCtx);
    withTransferInfo(reportEntry, accountCtx);
  }

  private static String formatName(User user) {
    StringBuilder builder = new StringBuilder();
    Personal personal = user.getPersonal();

    if (personal == null) {
      log.info("Personal info not found - user {}", user.getId());
    }
    else {
      builder.append(personal.getLastName());

      String firstName = personal.getFirstName();
      if (firstName != null && !firstName.isBlank()) {
        builder.append(format(", %s", firstName));
      }

      String middleName = personal.getMiddleName();
      if (middleName != null && !middleName.isBlank()) {
        builder.append(format(" %s", middleName));
      }
    }

    return builder.toString();
  }

  @With
  @AllArgsConstructor
  private static class RefundReportContext {
    final DateTimeZone timeZone;
    final Map<String, RefundData> refunds;
    final Map<String, AccountContextData> accounts;
    final Set<String> processedAccounts;
    final Map<String, User> users;
    final Map<String, UserGroup> userGroups;
    final Map<String, Item> items;

    public RefundReportContext() {
      timeZone = UTC;
      refunds = new HashMap<>();
      accounts = new HashMap<>();
      processedAccounts = new HashSet<>();
      users = new HashMap<>();
      userGroups = new HashMap<>();
      items = new HashMap<>();
    }

    AccountContextData getAccountContextById(String accountId) {
      return accounts.computeIfAbsent(accountId, key -> null);
    }

    Account getAccountById(String accountId) {
      AccountContextData accountContextData = getAccountContextById(accountId);
      return accountContextData == null ? null : accountContextData.account;
    }

    Item getItemByAccountId(String accountId) {
      Account account = getAccountById(accountId);

      if (account != null) {
        String itemId = account.getItemId();
        if (itemId != null && items.containsKey(itemId)) {
          return items.get(itemId);
        }
      }

      return null;
    }

    User getUserByAccountId(String accountId) {
      Account account = getAccountById(accountId);

      if (account != null && isUuid(account.getUserId())) {
        return users.get(account.getUserId());
      }

      return null;
    }

    UserGroup getUserGroupByAccountId(String accountId) {
      User user = getUserByAccountId(accountId);

      if (user != null && isUuid(user.getPatronGroup())) {
        return userGroups.get(user.getPatronGroup());
      }

      return null;
    }

    void markAccountProcessed(String accountId) {
      processedAccounts.add(accountId);
      accounts.remove(accountId);
    }
  }

  @With
  @AllArgsConstructor
  @Getter
  private static class AccountContextData {
    final Account account;
    final List<Feefineaction> actions;
    final AccountProcessingContext processingContext;
    final String instance;

    public AccountContextData() {
      account = null;
      actions = new ArrayList<>();
      processingContext = new AccountProcessingContext();
      instance = "";
    }
  }

  @With
  @AllArgsConstructor
  @NoArgsConstructor(force = true)
  private static class RefundData {
    final Feefineaction refundAction;
    final RefundReportEntry reportEntry;

    public RefundData(Feefineaction refundAction) {
      this.refundAction = refundAction;

      reportEntry = new RefundReportEntry()
        .withPatronId(refundAction.getUserId())
        .withFeeFineId(refundAction.getAccountId());
    }
  }

  @Setter
  @AllArgsConstructor
  private static class AccountProcessingContext {
    MonetaryValue paidAmount;
    List<String> paymentMethods;
    List<String> paymentTransactionInfo;
    MonetaryValue transferredAmount;
    List<String> transferAccounts;

    public AccountProcessingContext() {
      paidAmount = new MonetaryValue(ZERO);
      paymentMethods = new ArrayList<>();
      paymentTransactionInfo = new ArrayList<>();
      transferredAmount = new MonetaryValue(ZERO);
      transferAccounts = new ArrayList<>();
    }
  }
}
