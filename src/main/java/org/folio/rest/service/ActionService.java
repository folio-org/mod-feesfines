package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.Double.parseDouble;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.domain.Action.isTerminalStatus;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.ActionRepository;
import org.folio.rest.tools.utils.TenantTool;


import io.vertx.core.Context;
import io.vertx.core.Future;

public class ActionService {
  private final AccountRepository accountRepository;
  private final ActionRepository actionRepository;
  private final AccountUpdateService accountUpdateService;
  private final ActionValidationService validationService;
  private final PatronNoticeService patronNoticeService;

  public ActionService(Map<String, String> okapiHeaders, Context vertxContext) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(),
      TenantTool.tenantId(okapiHeaders));

    this.accountRepository = new AccountRepository(postgresClient);
    this.actionRepository = new ActionRepository(postgresClient);
    this.accountUpdateService = new AccountUpdateService(okapiHeaders, vertxContext);
    this.validationService = new ActionValidationService(accountRepository);
    this.patronNoticeService = new PatronNoticeService(vertxContext.owner(), okapiHeaders);
  }

  public Future<ActionContext> pay(String accountId, ActionRequest request) {
    return performAction(Action.PAY, accountId, request);
  }

  private Future<ActionContext> performAction(Action action, String accountId,
    ActionRequest request) {

    return succeededFuture(new ActionContext(accountId, request, action))
      .compose(this::findAccount)
      .compose(this::validateAction)
      .compose(this::createFeeFineAction)
      .compose(this::updateAccount)
      .compose(this::sendPatronNotice);
  }

  private Future<ActionContext> findAccount(ActionContext context) {
    return accountRepository.getAccountById(context.getAccountId())
      .map(context::withAccount);
  }

  private Future<ActionContext> validateAction(ActionContext context) {
    final String amount = context.getRequest().getAmount();

    return validationService.validate(context.getAccount(), amount)
      .map(result -> context.withRequestedAmount(parseDouble(amount)));
  }

  private Future<ActionContext> createFeeFineAction(ActionContext context) {
    ActionRequest request = context.getRequest();
    Account account = context.getAccount();
    Action action = context.getAction();
    Double requestedAmount = context.getRequestedAmount();
    double remainingAmount = account.getRemaining() - requestedAmount;

    String actionType = remainingAmount == 0
      ? action.getFullResult()
      : action.getPartialResult();

    Feefineaction feeFineAction = new Feefineaction()
      .withAmountAction(requestedAmount)
      .withComments(request.getComments())
      .withNotify(request.getNotifyPatron())
      .withTransactionInformation(request.getTransactionInfo())
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withPaymentMethod(request.getPaymentMethod())
      .withAccountId(context.getAccountId())
      .withUserId(account.getUserId())
      .withBalance(remainingAmount)
      .withTypeAction(actionType)
      .withId(UUID.randomUUID().toString())
      .withDateAction(new Date())
      .withAccountId(context.getAccountId());

    return actionRepository.save(feeFineAction)
      .map(context.withFeeFineAction(feeFineAction));
  }

  private Future<ActionContext> updateAccount(ActionContext context) {
    final Feefineaction feeFineAction = context.getFeeFineAction();
    final Account account = context.getAccount();

    if (isTerminalStatus(feeFineAction.getTypeAction())) {
      account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
    }

    account.setRemaining(feeFineAction.getBalance());
    account.getPaymentStatus().setName(feeFineAction.getTypeAction());

    return accountUpdateService.updateAccount(account)
      .map(context);
  }

  private Future<ActionContext> sendPatronNotice(ActionContext context) {
    if (isTrue(context.getRequest().getNotifyPatron())) {
      patronNoticeService.sendPatronNotice(context.getFeeFineAction());
    }
    return succeededFuture(context);
  }

  public static class ActionContext {
    private final String accountId;
    private final ActionRequest request;
    private final Action action;
    private Double requestedAmount;
    private Account account;
    private Feefineaction feeFineAction;

    public ActionContext(String accountId, ActionRequest request, Action action) {
      this.accountId = accountId;
      this.request = request;
      this.action = action;
    }

    public ActionContext withAccount(Account account) {
      this.account = account;
      return this;
    }

    public ActionContext withFeeFineAction(Feefineaction feefineaction) {
      this.feeFineAction = feefineaction;
      return this;
    }

    public ActionContext withRequestedAmount(Double requestedAmount) {
      this.requestedAmount = requestedAmount;
      return this;
    }

    public String getAccountId() {
      return accountId;
    }

    public ActionRequest getRequest() {
      return request;
    }

    public Action getAction() {
      return action;
    }

    public Account getAccount() {
      return account;
    }

    public Feefineaction getFeeFineAction() {
      return feeFineAction;
    }

    public Double getRequestedAmount() {
      return requestedAmount;
    }
  }

}