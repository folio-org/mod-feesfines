package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.domain.FeeFineActionResult.shouldCloseAccount;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.FeeFineActionResult;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.FeeFineActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.ActionRepository;
import org.folio.rest.tools.utils.TenantTool;


import io.vertx.core.Context;
import io.vertx.core.Future;

public class FeeFineActionService {
  private final AccountRepository accountRepository;
  private final ActionRepository actionRepository;
  private final AccountUpdateService accountUpdateService;
  private final FeeFineActionValidationService validationService;
  private final PatronNoticeService patronNoticeService;

  public FeeFineActionService(Map<String, String> okapiHeaders, Context vertxContext) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(),
      TenantTool.tenantId(okapiHeaders));

    this.accountRepository = new AccountRepository(postgresClient);
    this.actionRepository = new ActionRepository(postgresClient);
    this.accountUpdateService = new AccountUpdateService(okapiHeaders, vertxContext);
    this.validationService = new FeeFineActionValidationService(accountRepository);
    this.patronNoticeService = new PatronNoticeService(vertxContext.owner(), okapiHeaders);
  }

  public Future<ActionContext> pay(String accountId, FeeFineActionRequest request) {
    return succeededFuture(new ActionContext(accountId, request))
      .compose(this::findAccount)
      .compose(this::validateAction)
      .compose(this::createAction)
      .compose(this::updateAccount)
      .compose(this::sendPatronNotice);
  }

  private Future<ActionContext> findAccount(ActionContext context) {
    return accountRepository.getAccountById(context.getAccountId())
      .map(context::withAccount);
  }

  private Future<ActionContext> validateAction(ActionContext context) {
    return validationService.validate(context.getAccount(), context.getRequest().getAmount())
      .map(context);
  }

  private Future<ActionContext> createAction(ActionContext context) {
    FeeFineActionRequest request = context.getRequest();
    Account account = context.getAccount();
    double remainingAmount = account.getRemaining() - request.getAmount();

    String actionType = remainingAmount == 0
      ? FeeFineActionResult.PAID_FULLY.getName()
      : FeeFineActionResult.PAID_PARTIALLY.getName();

    Feefineaction action = new Feefineaction()
      .withAmountAction(request.getAmount())
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

    return actionRepository.save(action)
      .map(context.withAction(action));
  }

  private Future<ActionContext> updateAccount(ActionContext context) {
    final Feefineaction action = context.getAction();
    final Account account = context.getAccount();

    if (shouldCloseAccount(action.getTypeAction())) {
      account.setRemaining(0.0);
      account.getStatus().setName(FeeFineStatus.CLOSED.getValue());
    } else {
      account.setRemaining(action.getBalance());
    }

    return accountUpdateService.updateAccount(account)
      .map(context);
  }

  private Future<ActionContext> sendPatronNotice(ActionContext context) {
    if (isTrue(context.getRequest().getNotifyPatron())) {
      patronNoticeService.sendPatronNotice(context.getAction());
    }
    return succeededFuture(context);
  }

  public static class ActionContext {
    private final String accountId;
    private final FeeFineActionRequest request;
    private Account account;
    private Feefineaction action;

    public ActionContext(String accountId, FeeFineActionRequest request) {
      this.accountId = accountId;
      this.request = request;
    }

    public ActionContext withAccount(Account account) {
      this.account = account;
      return this;
    }

    public ActionContext withAction(Feefineaction action) {
      this.action = action;
      return this;
    }

    public String getAccountId() {
      return accountId;
    }

    public FeeFineActionRequest getRequest() {
      return request;
    }

    public Account getAccount() {
      return account;
    }

    public Feefineaction getAction() {
      return action;
    }
  }
}
