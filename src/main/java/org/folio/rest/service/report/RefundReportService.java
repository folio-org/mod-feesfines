package org.folio.rest.service.report;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.math.BigDecimal.ZERO;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.utils.FeeFineActionHelper.getPatronInfoFromComment;
import static org.folio.rest.utils.FeeFineActionHelper.getStaffInfoFromComment;
import static org.folio.rest.utils.PatronHelper.buildFormattedName;
import static org.folio.util.UuidUtil.isUuid;
import static org.joda.time.DateTimeZone.UTC;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.SettingsClient;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.LocaleSettings;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.RefundReport;
import org.folio.rest.jaxrs.model.RefundReportEntry;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.jaxrs.model.UserGroup;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.service.report.context.HasItemInfo;
import org.folio.rest.service.report.context.HasUserInfo;
import org.folio.rest.service.report.utils.LookupHelper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import io.vertx.core.Context;
import io.vertx.core.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

// TODO: inherit from DateBasedReportService<RefundReport>
public class RefundReportService {
  private static final Logger log = LogManager.getLogger(RefundReportService.class);

  private static final int REPORT_ROWS_LIMIT = 1_000_000;
  private static final String MULTIPLE_MESSAGE = "Multiple";
  private static final String SEE_FEE_FINE_DETAILS_PAGE_MESSAGE = "See Fee/fine details page";
  private static final String REFUNDED_TO_PATRON = "Refunded to patron";
  private static final String REFUNDED_TO_BURSAR = "Refunded to Bursar";
  private static final LocaleSettings FALLBACK_LOCALE_SETTINGS =
    new LocaleSettings(Locale.US.toLanguageTag(), UTC.getID(),
      Currency.getInstance(Locale.US).getCurrencyCode());

  private final SettingsClient settingsClient;
  private final FeeFineActionRepository feeFineActionRepository;
  private final AccountRepository accountRepository;

  private final LookupHelper lookupHelper;

  private DateTimeZone timeZone;
  private DateTimeFormatter dateTimeFormatter;
  private Currency currency;

  public RefundReportService(Map<String, String> headers, Context context) {
    settingsClient = new SettingsClient(context.owner(), headers);
    feeFineActionRepository = new FeeFineActionRepository(headers, context);
    accountRepository = new AccountRepository(context, headers);

    lookupHelper = new LookupHelper(headers, context);
  }

  public Future<RefundReport> buildReport(DateTime startDate, DateTime endDate,
    List<String> ownerIds) {

    return settingsClient.getLocaleSettings()
      .recover(throwable -> succeededFuture(FALLBACK_LOCALE_SETTINGS))
      .compose(localeSettings -> buildReportWithLocale(startDate, endDate, ownerIds,
        localeSettings));
  }

  private Future<RefundReport> buildReportWithLocale(DateTime startDate, DateTime endDate,
    List<String> ownerIds, LocaleSettings localeSettings) {

    setUpLocale(localeSettings);

    String startDateTimeFormatted = null;
    String endDateTimeFormatted = null;
    String logStartDate = "null";
    String logEndDate = "null";

    if (startDate != null) {
      startDateTimeFormatted = startDate
        .withTimeAtStartOfDay()
        .withZoneRetainFields(timeZone)
        .withZone(UTC)
        .toString(ISODateTimeFormat.dateTime());
      logStartDate = startDate.toDateTimeISO().toString();
    }

    if (endDate != null) {
      endDateTimeFormatted = endDate
        .withTimeAtStartOfDay()
        .plusDays(1)
        .withZoneRetainFields(timeZone)
        .withZone(UTC)
        .toString(ISODateTimeFormat.dateTime());
      logEndDate = endDate.toDateTimeISO().toString();
    }

    log.info("Building refund report with parameters: startDate={}, endDate={}, ownerIds={}, tz={}",
      logStartDate, logEndDate, ownerIds, timeZone);

    RefundReportContext ctx = new RefundReportContext().withTimeZone(timeZone);

    return feeFineActionRepository
      .find(REFUND, startDateTimeFormatted, endDateTimeFormatted,
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
      .compose(r -> lookupHelper.lookupItemForAccount(ctx, accountId))
      .compose(r -> lookupHelper.lookupInstanceForAccount(ctx, accountId))
      .compose(r -> lookupHelper.lookupUserForAccount(ctx, ctx.getAccountById(accountId)))
      .compose(r -> lookupHelper.lookupUserGroupForUser(ctx, accountId))
      .compose(r -> lookupHelper.lookupRefundPayTransferFeeFineActionsForAccount(ctx, accountId))
      .map(actions -> processAccount(ctx, accountId));
  }

  private RefundReportContext processAccount(RefundReportContext ctx, String accountId) {
    AccountContextData accountData = ctx.accounts.get(accountId);

    if (accountData != null) {
      accountData.actions.forEach(action -> processAccountAction(ctx, accountId, action));
    }

    ctx.markAccountProcessed(accountId);

    return ctx;
  }

  private void processAccountAction(RefundReportContext ctx,
    String accountId, Feefineaction feeFineAction) {

    log.info("Processing fee/fine action {}, account {}", feeFineAction.getId(), accountId);

    AccountContextData accountData = ctx.accounts.get(accountId);
    if (accountData == null) {
      log.error("processAccountAction:: accountData is null");
      return;
    }
    AccountProcessingContext accountCtx = accountData.processingContext;

    if (actionIsOfType(feeFineAction, PAY)) {
      if (feeFineAction.getAmountAction() != null) {
        accountCtx.setPaidAmount(accountCtx.paidAmount.add(
          feeFineAction.getAmountAction()));
        accountCtx.paymentMethods.add(feeFineAction.getPaymentMethod());
        accountCtx.paymentTransactionInfo.add(feeFineAction.getTransactionInformation());
      } else {
        log.error("Payment amount is null - fee/fine action {}, account {}", feeFineAction.getId(),
          accountId);
      }
    } else if (actionIsOfType(feeFineAction, TRANSFER)) {
      if (feeFineAction.getAmountAction() != null) {
        accountCtx.setTransferredAmount(accountCtx.transferredAmount.add(
          feeFineAction.getAmountAction()));
        accountCtx.transferAccounts.add(feeFineAction.getPaymentMethod());
      } else {
        log.error("Transfer amount is null - fee/fine action {}, account {}", feeFineAction.getId(),
          accountId);
      }
    } else if (actionIsOfType(feeFineAction, REFUND)
      && ctx.refunds.containsKey(feeFineAction.getId())) {

      RefundData refundData = ctx.refunds.get(feeFineAction.getId());
      RefundReportEntry reportEntry = refundData.reportEntry;

      Account account = ctx.getAccountById(accountId);
      User user = ctx.getUserByAccountId(accountId);
      UserGroup userGroup = ctx.getUserGroupByAccountId(accountId);
      Item item = ctx.getItemByAccountId(accountId);

      reportEntry
        .withFeeFineId(accountId)
        .withRefundDate(formatDate(feeFineAction.getDateAction(), ctx.timeZone))
        .withRefundAction(feeFineAction.getTypeAction())
        .withRefundReason(feeFineAction.getPaymentMethod())
        .withStaffInfo(getStaffInfoFromComment(feeFineAction))
        .withPatronInfo(getPatronInfoFromComment(feeFineAction));

      var amountAction = feeFineAction.getAmountAction();
      if (amountAction != null) {
        reportEntry = reportEntry.withRefundAmount(amountAction.toString());
      }

      if (user != null) {
        reportEntry
          .withPatronName(buildFormattedName(user))
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
          .withBilledAmount(account.getAmount().toString())
          .withDateBilled(formatDate(account.getMetadata().getCreatedDate(), ctx.timeZone))
          .withFeeFineOwner(account.getFeeFineOwner());
      } else {
        log.error("Refund report - account is null, refund action {}", feeFineAction.getId());
      }

      if (accountCtx != null) {
        if (isRefundedToPatron(feeFineAction)) {
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

  private static String getItemBarcode(RefundReportContext ctx, String accountId) {
    Item item = ctx.getItemByAccountId(accountId);

    if (item == null) {
      return "";
    }

    return item.getBarcode();
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
        SEE_FEE_FINE_DETAILS_PAGE_MESSAGE));
  }

  private void withTransferInfo(RefundReportEntry reportEntry, AccountProcessingContext accountCtx) {
    reportEntry.withTransferredAmount(accountCtx.transferredAmount.toString())
      .withTransferAccount(singleOrDefaultMessage(accountCtx.transferAccounts, MULTIPLE_MESSAGE));
  }

  private void withPaymentAndTransferInfo(RefundReportEntry reportEntry, AccountProcessingContext accountCtx) {
    withPaymentInfo(reportEntry, accountCtx);
    withTransferInfo(reportEntry, accountCtx);
  }

  @With
  @AllArgsConstructor
  @Getter
  private static class RefundReportContext implements HasUserInfo, HasItemInfo {
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

    public Account getAccountById(String accountId) {
      AccountContextData accountContextData = getAccountContextById(accountId);
      return accountContextData == null ? null : accountContextData.account;
    }

    public Item getItemByAccountId(String accountId) {
      Account account = getAccountById(accountId);

      if (account != null) {
        String itemId = account.getItemId();
        if (itemId != null && items.containsKey(itemId)) {
          return items.get(itemId);
        }
      }

      return null;
    }

    public User getUserByAccountId(String accountId) {
      Account account = getAccountById(accountId);

      if (account != null && isUuid(account.getUserId())) {
        return users.get(account.getUserId());
      }

      return null;
    }

    public UserGroup getUserGroupByAccountId(String accountId) {
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

    public Future<Void> updateAccountContextWithInstance(String accountId, Instance instance) {
      accounts.put(accountId, getAccountContextById(accountId).withInstance(instance.getTitle()));
      return succeededFuture();
    }

    public Future<Void> updateAccountContextWithEffectiveLocation(String accountId,
      Location location) {

      return succeededFuture();
    }

    public Future<Void> updateAccountContextWithActions(String accountId, List<Feefineaction> actions) {
      AccountContextData accountContextData = getAccountContextById(accountId);
      if (accountContextData == null) {
        return succeededFuture();
      }

      accounts.put(accountId, accountContextData.withActions(actions));
      return succeededFuture();
    }

    public boolean isAccountContextCreated(String accountId) {
      return getAccountContextById(accountId) != null;
    }

    public List<Feefineaction> getAccountFeeFineActions(String accountId) {
      AccountContextData accountCtx = getAccountContextById(accountId);
      if (accountCtx == null) {
        return null;
      }

      return accountCtx.getActions();
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
