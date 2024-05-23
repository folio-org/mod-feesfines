package org.folio.rest.service.action;

import static io.vertx.core.CompositeFuture.all;
import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.fromValue;
import static org.folio.rest.persist.PostgresClient.getInstance;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.FEE_FINE;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.ActionRequest;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.service.AccountUpdateService;
import org.folio.rest.service.LogEventPublisher;
import org.folio.rest.service.LogEventService;
import org.folio.rest.service.PatronNoticeService;
import org.folio.rest.service.action.context.ActionContext;
import org.folio.rest.service.action.validation.ActionValidationService;
import org.folio.rest.utils.amountsplitter.BulkActionAmountSplitterStrategy;
import org.folio.rest.utils.amountsplitter.SplitEvenlyRecursively;

import io.vertx.core.Context;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ActionService {
  protected final Action action;
  protected final AccountRepository accountRepository;
  protected final FeeFineActionRepository feeFineActionRepository;
  protected final AccountUpdateService accountUpdateService;
  protected final ActionValidationService validationService;
  protected final PatronNoticeService patronNoticeService;
  protected final BulkActionAmountSplitterStrategy amountSplitterStrategy;
  private final LogEventService logEventService;
  private final LogEventPublisher logEventPublisher;
  private final Map<String, String> headers;

  protected ActionService(Action action, ActionValidationService validationService,
    Map<String, String> headers, Context context) {

    PostgresClient postgresClient = getInstance(context.owner(), tenantId(headers));

    this.action = action;
    this.accountRepository = new AccountRepository(postgresClient);
    this.feeFineActionRepository = new FeeFineActionRepository(headers, context);
    this.accountUpdateService = new AccountUpdateService(headers, context);
    this.patronNoticeService = new PatronNoticeService(context.owner(), headers);
    this.validationService = validationService;
    this.amountSplitterStrategy = new SplitEvenlyRecursively();
    this.logEventService = new LogEventService(context.owner(), headers);
    this.logEventPublisher = new LogEventPublisher(context.owner(), headers);
    this.headers = headers;
  }

  protected ActionService(Action action, ActionValidationService validationService,
    BulkActionAmountSplitterStrategy bulkActionAmountSplitterStrategy,
    Map<String, String> headers, Context context) {

    PostgresClient postgresClient = getInstance(context.owner(), tenantId(headers));

    this.action = action;
    this.accountRepository = new AccountRepository(postgresClient);
    this.feeFineActionRepository = new FeeFineActionRepository(headers, context);
    this.accountUpdateService = new AccountUpdateService(headers, context);
    this.patronNoticeService = new PatronNoticeService(context.owner(), headers);
    this.validationService = validationService;
    this.amountSplitterStrategy = bulkActionAmountSplitterStrategy;
    this.logEventService = new LogEventService(context.owner(), headers);
    this.logEventPublisher = new LogEventPublisher(context.owner(), headers);
    this.headers = headers;
  }

  public Future<ActionContext> performAction(ActionRequest request) {
    return succeededFuture(new ActionContext(request))
      .compose(this::findAccounts)
      .compose(this::validateAction)
      .compose(this::createFeeFineActions)
      .compose(this::publishLogEvents)
      .compose(this::updateAccounts)
      .compose(this::sendPatronNotice)
      .onSuccess(accountUpdateService::publishLoanRelatedFeeFineClosedEvent);
  }

  private Future<ActionContext> findAccounts(ActionContext context) {
    return accountRepository.getAccountsByIdWithNulls(context.getRequest().getAccountIds())
      .map(context::withAccounts);
  }

  protected Future<ActionContext> validateAction(ActionContext context) {
    String requestedAmount = context.getRequest().getAmount();

    return validationService.validate(context.getAccounts(), requestedAmount)
      .map(ignored -> context.withRequestedAmount(new MonetaryValue(requestedAmount)));
  }

  protected Future<ActionContext> createFeeFineActions(ActionContext context) {
    final ActionRequest request = context.getRequest();
    final List<Account> accounts = new ArrayList<>(context.getAccounts().values());
    final MonetaryValue requestedAmount = context.getRequestedAmount();

    Map<String, MonetaryValue> actionableAmounts = accounts.stream()
      .collect(toMap(Account::getId, Account::getRemaining));

    Map<String, MonetaryValue> distributedAmounts = amountSplitterStrategy.split(
      requestedAmount, actionableAmounts);

    List<Feefineaction> feeFineActions = accounts.stream()
      .map(account -> createFeeFineActionAndUpdateAccount(
        account, distributedAmounts.get(account.getId()), request))
      .collect(toList());

    return all(feeFineActions.stream().map(feeFineActionRepository::save).collect(toList()))
      .map(context.withFeeFineActions(feeFineActions));
  }

  protected Feefineaction createFeeFineActionAndUpdateAccount(Account account, MonetaryValue amount,
    ActionRequest request) {

    final MonetaryValue remainingAmountAfterAction = account.getRemaining()
      .subtract(amount);
    boolean isFullAction = remainingAmountAfterAction.isZero();
    String actionType = isFullAction ? action.getFullResult() : action.getPartialResult();

    final Feefineaction feeFineAction = new Feefineaction()
      .withAmountAction(amount)
      .withComments(request.getComments())
      .withNotify(request.getNotifyPatron())
      .withTransactionInformation(request.getTransactionInfo())
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withPaymentMethod(request.getPaymentMethod())
      .withAccountId(account.getId())
      .withUserId(account.getUserId())
      .withBalance(remainingAmountAfterAction)
      .withTypeAction(actionType)
      .withId(UUID.randomUUID().toString())
      .withDateAction(new Date());

    account.getPaymentStatus().setName(fromValue(actionType));

    if (isFullAction) {
      account.getStatus().setName(CLOSED.getValue());
      account.setRemaining(MonetaryValue.ZERO);
    } else {
      account.setRemaining(feeFineAction.getBalance());
    }

    return feeFineAction;
  }

  private Future<ActionContext> updateAccounts(ActionContext context) {
    return all(
      context.getAccounts()
        .values()
        .stream()
        .map(account -> accountUpdateService.updateAccount(account, headers))
        .collect(toList())
    ).map(context);
  }

  private Future<ActionContext> sendPatronNotice(ActionContext context) {
    if (isTrue(context.getRequest().getNotifyPatron())) {
      context.getFeeFineActions().stream()
        // do not send notices for CREDIT actions
        .filter(ffa -> !CREDIT.isActionForResult(ffa.getTypeAction()))
        .forEach(patronNoticeService::sendPatronNotice);
    }
    return succeededFuture(context);
  }

  private Future<ActionContext> publishLogEvents(ActionContext actionContext) {
    return all(actionContext.getFeeFineActions().stream()
      .map(ffa -> logEventService.createFeeFineLogEventPayload(ffa,
          actionContext.getAccounts().get(ffa.getAccountId()))
        .compose(eventPayload -> {
          logEventPublisher.publishLogEvent(eventPayload, FEE_FINE);
          return succeededFuture();
        }))
      .collect(toList()))
      .map(actionContext);
  }
}
