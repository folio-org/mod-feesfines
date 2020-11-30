package org.folio.rest.service.report;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.utils.AccountHelper.PATRON_COMMENTS_KEY;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.client.ConfigurationClient;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.client.UserGroupsClient;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.exception.InternalServerErrorException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.RefundReport;
import org.folio.rest.jaxrs.model.RefundReportEntry;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.utils.AccountHelper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Context;
import io.vertx.core.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

public class RefundReportService {
  private static final Logger log = LoggerFactory.getLogger(RefundReportService.class);

  private static final int REPORT_ROWS_LIMIT = 1000000;
  private static final String INVALID_START_DATE_OR_END_DATE_MESSAGE =
    "Invalid startDate or endDate parameter";
  private static final String SEE_FEE_FINE_DETAILS_PAGE_MESSAGE =
    "See Fee/fine details page";
  private static final DateTimeFormatter dateTimeFormatter =
    DateTimeFormat.forPattern("M/d/yyyy K:m a");

  private final ConfigurationClient configurationClient;
  private final InventoryClient inventoryClient;
  private final UsersClient usersClient;
  private final UserGroupsClient userGroupsClient;
  private final FeeFineActionRepository feeFineActionRepository;
  private final AccountRepository accountRepository;

  public RefundReportService(Map<String, String> headers, Context context) {
    configurationClient = new ConfigurationClient(context.owner(), headers);
    inventoryClient = new InventoryClient(context.owner(), headers);
    usersClient = new UsersClient(context.owner(), headers);
    userGroupsClient = new UserGroupsClient(context.owner(), headers);
    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    accountRepository = new AccountRepository(context, headers);
  }

  public Future<RefundReport> buildReport(String startDate, String endDate) {
    return configurationClient.findTimeZone()
      .compose(tz -> buildReport(startDate, endDate, tz));
  }

  private Future<RefundReport> buildReport(String startDate, String endDate,
    DateTimeZone dateTimeZone) {

    DateTime startDateTime = parseDate(startDate);
    DateTime endDateTime = parseDate(endDate);

    if (startDateTime == null || endDateTime == null || dateTimeZone == null) {
      log.error(format("Invalid parameters: startDate=%s, endDate=%s, tz=%s", startDate, endDate,
        dateTimeZone));

      throw new FailedValidationException(INVALID_START_DATE_OR_END_DATE_MESSAGE);
    }

    String startDtFormatted = startDateTime
      .withTimeAtStartOfDay()
      .withZoneRetainFields(dateTimeZone)
      .withZone(DateTimeZone.UTC)
      .toString(ISODateTimeFormat.dateTime());

    String endDtFormatted = endDateTime
      .withTimeAtStartOfDay()
      .plusDays(1)
      .withZoneRetainFields(dateTimeZone)
      .withZone(DateTimeZone.UTC)
      .toString(ISODateTimeFormat.dateTime());

    log.info(format("Building refund report with parameters: startDate=%s, endDate=%s, tz=%s",
      startDateTime, endDateTime, dateTimeZone));

    return feeFineActionRepository
      .findActionsByTypeForPeriod(REFUND, startDtFormatted, endDtFormatted, REPORT_ROWS_LIMIT)
      .map(this::toRefundDataMap)
      .map(refundActions -> new RefundReportContext().withRefunds(refundActions))
      .compose(this::processAllRefundActions)
      .map(null);
  }

  private Future<RefundReportContext> processAllRefundActions(RefundReportContext ctx) {
    return ctx.refunds.values().stream()
      .map(refundData -> processSingleRefundAction(ctx, refundData.refundAction))
      .reduce(succeededFuture(), (left, right) -> left.compose(result -> right));
  }

  private Future<RefundReportContext> processSingleRefundAction(RefundReportContext ctx,
    Feefineaction refundAction) {

    String accountId = refundAction.getAccountId();

    if (ctx.accounts.containsKey(accountId) && ctx.accounts.get(accountId).processed) {
      return succeededFuture();
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

    log.info(format("Processing account ID %s", accountId));

    accountFeeFineActions.forEach(action -> processAccountAction(ctx, accountId, action));

    return ctx;
  }

  private void processAccountAction(RefundReportContext ctx,
    String accountId, Feefineaction feeFineAction) {

    log.info(format("Processing fee/fine action ID %s, account ID %s", feeFineAction.getId(),
      accountId));

    AccountContextData accountData = ctx.accounts.get(accountId);
    AccountProcessingContext accountCtx = accountData.processingContext;

    if (actionIsOfType(feeFineAction, PAY)) {
      if (feeFineAction.getAmountAction() != null) {
        accountCtx.paidAmount.add(new MonetaryValue(feeFineAction.getAmountAction()));
        accountCtx.paymentMethods.add(feeFineAction.getPaymentMethod());
        accountCtx.paymentTransactionInfo.add(feeFineAction.getTransactionInformation());
      } else {
        log.error(format("Amount is null - fee/fine action ID %s, account ID %s",
          feeFineAction.getId(), accountId));
      }
    }

    if (actionIsOfType(feeFineAction, TRANSFER)) {
      accountCtx.transferredAmount.add(new MonetaryValue(feeFineAction.getAmountAction()));
      accountCtx.transferAccounts.add(feeFineAction.getPaymentMethod());
    }

    if (actionIsOfType(feeFineAction, REFUND)) {
      log.error(format("Processing refund action - fee/fine action ID %s, account ID %s",
        feeFineAction.getId(), accountId));

      RefundData refundData = ctx.refunds.get(feeFineAction.getId());

      log.error(format("Processing refund report entry - fee/fine action ID %s, account ID %s",
        feeFineAction.getId(), accountId));

      RefundReportEntry reportEntry = refundData.reportEntry;

      logCtxLookupStep("Looking for account in ctx", feeFineAction);
      Account account = accountData.account;

      logCtxLookupStep("Looking for user in ctx", feeFineAction);
      User user = ctx.users.get(account.getUserId());

      logCtxLookupStep("Looking for user group in ctx", feeFineAction);
      UserGroup userGroup = ctx.userGroups.get(user.getPatronGroup());

      reportEntry.setPatronName(user.getUsername());
      reportEntry.setPatronName(user.getBarcode());
      reportEntry.setPatronName(user.getId());
      reportEntry.setPatronName(userGroup.getGroup());
      reportEntry.setFeeFineType(account.getFeeFineType());
      reportEntry.setBilledAmount(formatMonetaryValue(account.getAmount()));
      reportEntry.setDateBilled(formatDate(account.getDateCreated()));
      reportEntry.setPaidAmount(accountCtx.paidAmount.toString());
      reportEntry.setPaymentMethod(singleOrDefaultMessage(accountCtx.paymentMethods));
      reportEntry.setTransactionInfo(singleOrDefaultMessage(accountCtx.paymentTransactionInfo));
      reportEntry.setTransferredAmount(accountCtx.transferredAmount.toString());
      reportEntry.setTransactionInfo(singleOrDefaultMessage(accountCtx.transferAccounts));
      reportEntry.setFeeFineId(accountId);
      reportEntry.setRefundDate(formatDate(feeFineAction.getDateAction()));
      reportEntry.setRefundAmount(formatMonetaryValue(feeFineAction.getAmountAction()));
      reportEntry.setRefundAction(feeFineAction.getTypeAction());
      reportEntry.setRefundReason(feeFineAction.getPaymentMethod());
      reportEntry.setStaffInfo(getStaffInfo(feeFineAction.getComments()));
      reportEntry.setPatronInfo(getPatronInfo(feeFineAction.getComments()));
      reportEntry.setItemBarcode(getItemBarcode(ctx, accountId));
      reportEntry.setInstance(ctx.accounts.get(accountId).instance);
      reportEntry.setActionCompletionDate("");
      reportEntry.setStaffMemberName("");
      reportEntry.setActionTaken("");
    }
  }

  private void logCtxLookupStep(String message, Feefineaction feeFineAction) {
    log.error(format(message + " - fee/fine action ID %s, account ID %s",
      feeFineAction.getId(), feeFineAction.getAccountId()));
  }

  private Future<RefundReportContext> lookupAccount(RefundReportContext ctx,
    Feefineaction refundAction) {

    String accountId = refundAction.getAccountId();

    if (accountId == null) {
      String message = format("Account ID is null in fee/ine action %s", refundAction.getId());
      log.error(message);
      throw new InternalServerErrorException(message);
    }

    return accountRepository.getAccountById(accountId)
      .map(account -> addAccountContextData(ctx, account))
      .map(acd -> acd.account)
      .map(ctx);
  }

  private AccountContextData addAccountContextData(RefundReportContext ctx,
    Account account) {

    if (account == null) {
      String message = format("Account %s not found", account.getId());
      log.error(message);
      throw new AccountNotFoundValidationException(message);
    }

    if (ctx.accounts.containsKey(account.getId())) {
      return ctx.accounts.get(account.getId());
    }

    AccountContextData accountContextData = new AccountContextData().withAccount(account);

    return ctx.accounts.put(account.getId(), accountContextData);
  }

  private Future<List<Feefineaction>> lookupFeeFineActionsForAccount(RefundReportContext ctx,
    String accountId) {

    log.info(format("Fetching action for account %s", accountId));

    return feeFineActionRepository.findActionsForAccount(accountId)
      .map(ctx.accounts.get(accountId)::withActions)
      .map(AccountContextData::getActions);
  }

  private Future<RefundReportContext> lookupItemForAccount(RefundReportContext ctx,
    String accountId) {

    log.info(format("Fetching item for account %s", accountId));

    String itemId = ctx.getAccountById(accountId).getItemId();

    if (itemId == null) {
      log.info(format("Item ID is null - account ID %s", accountId));
      return succeededFuture(ctx);
    }
    else {
      if (ctx.items.containsKey(itemId)) {
        return succeededFuture(ctx);
      }
      else {
        return inventoryClient.getItemById(itemId)
          .map(item -> addItemToContext(ctx, item, accountId, itemId))
          .map(ctx);
      }
    }
  }

  private Future<RefundReportContext> lookupInstanceForAccount(RefundReportContext ctx,
    String accountId) {

    log.info(format("Fetching instance for account %s", accountId));

    Item item = ctx.getItemByAccountId(accountId);

    if (item == null) {
      return succeededFuture(ctx);
    }

    String holdingsRecordId = item.getHoldingsRecordId();

    if (holdingsRecordId == null) {
      return succeededFuture(ctx);
    }

    return inventoryClient.getHoldingById(holdingsRecordId)
      .map(HoldingsRecord::getInstanceId)
      .compose(inventoryClient::getInstanceById)
      .map(instance -> ctx.accounts.put(accountId, ctx.accounts.get(accountId)
        .withInstance(instance.getTitle())))
      .map(ctx);
  }

  private Future<RefundReportContext> lookupUserForAccount(RefundReportContext ctx,
    String accountId) {

    log.info(format("Fetching user for account %s", accountId));

    String userId = ctx.getAccountById(accountId).getUserId();

    if (userId == null) {
      String message = format("User ID is null - account ID %s", accountId);
      log.error(message);
      throw new InternalServerErrorException(message);
    }

    if (ctx.users.containsKey(userId)) {
      return succeededFuture(ctx);
    }
    else {
      return usersClient.fetchUserById(userId)
        .map(user -> addUserToContext(ctx, user, accountId, userId))
        .map(ctx);
    }
  }

  private Future<RefundReportContext> lookupUserGroupForUser(RefundReportContext ctx,
    String accountId) {

    log.info(format("Fetching user group for account %s", accountId));

    Account account = ctx.accounts.get(accountId).account;
    String userId = account.getUserId();
    User user = ctx.users.get(userId);
    String userGroupId = user.getPatronGroup();

    if (userGroupId == null) {
      log.error(format("User group ID is null - user ID %s", user.getId()));
    }

    if (ctx.userGroups.containsKey(userGroupId)) {
      return succeededFuture(ctx);
    }
    else {
      return userGroupsClient.fetchUserGroupById(userGroupId)
        .map(userGroup -> ctx.userGroups.put(userGroup.getId(), userGroup))
        .map(ctx);
    }
  }

  private RefundReportContext addUserToContext(RefundReportContext ctx, User user,
    String accountId, String userId) {

    if (user == null) {
      String message = format("User not found - account ID %s, user ID %s", accountId, userId);
      log.error(message);
      throw new InternalServerErrorException(message);
    } else {
      ctx.users.put(userId, user);
    }

    return ctx;
  }

  private RefundReportContext addItemToContext(RefundReportContext ctx, Item item,
    String accountId, String itemId) {

    if (item == null) {
      log.info(format("Item not found - account ID %s, item ID %s", accountId, itemId));
    } else {
      ctx.items.put(itemId, item);
    }

    return ctx;
  }

  private boolean actionIsOfType(Feefineaction feeFineAction, Action action) {
    return List.of(action.getFullResult(), action.getPartialResult())
      .contains(feeFineAction.getTypeAction());
  }

  private Map<String, RefundData> toRefundDataMap(List<Feefineaction> refundActions) {
    return refundActions.stream()
      .collect(Collectors.toMap(Feefineaction::getId, RefundData::new));
  }

  private DateTime parseDate(String date) {
    try {
      return DateTime.parse(date, ISODateTimeFormat.basicDate());
    }
    catch (IllegalArgumentException e) {
      return null;
    }
  }

  private String formatMonetaryValue(Double value) {
    return new MonetaryValue(value).toString();
  }

  private String formatDate(Date date) {
    return new DateTime(date).toString(dateTimeFormatter);
  }

  private String singleOrDefaultMessage(List<String> values) {
    if (values.size() == 0) {
      return "";
    }

    if (values.size() == 1) {
      return values.get(0);
    }

    return SEE_FEE_FINE_DETAILS_PAGE_MESSAGE;
  }

  private String getStaffInfo(String comments) {
    return AccountHelper.parseFeeFineComments(comments).get(AccountHelper.STAFF_COMMENTS_KEY);
  }

  private String getPatronInfo(String comments) {
    return AccountHelper.parseFeeFineComments(comments).get(PATRON_COMMENTS_KEY);
  }

  private String getItemBarcode(RefundReportContext ctx, String accountId) {
    Item item = ctx.getItemByAccountId(accountId);

    if (item == null) {
      return "";
    }

    return item.getBarcode();
  }

  @With
  @AllArgsConstructor
  @NoArgsConstructor(force = true)
  private static class RefundReportContext {
    final Map<String, RefundData> refunds;
    final Map<String, AccountContextData> accounts;
    final Map<String, User> users;
    final Map<String, UserGroup> userGroups;
    final Map<String, Item> items;

    Account getAccountById(String accountId) {
      if (accounts != null && accounts.containsKey(accountId)) {
        return accounts.get(accountId).account;
      }

      String message = format("Account %s not found in context", accountId);
      log.error(message);
      throw new AccountNotFoundValidationException(message);
    }

    Item getItemByAccountId(String accountId) {
      String itemId = getAccountById(accountId).getItemId();

      if (itemId != null && items != null && items.containsKey(itemId)) {
        return items.get(itemId);
      }

      return null;
    }
  }

  @With
  @AllArgsConstructor
  @NoArgsConstructor(force = true)
  @Getter
  private static class AccountContextData {
    final Account account;
    final List<Feefineaction> actions;
    final boolean processed;
    final AccountProcessingContext processingContext;
    final String instance;
  }

  @With
  @AllArgsConstructor
  @NoArgsConstructor(force = true)
  private static class RefundData {
    final Feefineaction refundAction;
    final RefundReportEntry reportEntry;

    public RefundData(Feefineaction refundAction) {
      this.refundAction = refundAction;

      reportEntry = new RefundReportEntry();
    }
  }

  @With
  @AllArgsConstructor
  @NoArgsConstructor(force = true)
  private static class AccountProcessingContext {
    final MonetaryValue paidAmount;
    final List<String> paymentMethods;
    final List<String> paymentTransactionInfo;
    final MonetaryValue transferredAmount;
    final List<String> transferAccounts;
  }
}
