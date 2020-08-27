package org.folio.rest.service.action;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.domain.FeeFineStatus.OPEN;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.service.AccountUpdateService;
import org.folio.rest.service.PatronNoticeService;
import org.folio.rest.service.action.validation.ActionValidationService;
import org.folio.rest.service.action.validation.DefaultActionValidationService;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class DefaultActionService {
  private final AccountRepository accountRepository;
  private final FeeFineActionRepository feeFineActionRepository;
  private final AccountUpdateService accountUpdateService;
  private final ActionValidationService validationService;
  private final PatronNoticeService patronNoticeService;

  public DefaultActionService(Map<String, String> okapiHeaders, Context vertxContext) {
    PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(),
      TenantTool.tenantId(okapiHeaders));

    this.accountRepository = new AccountRepository(postgresClient);
    this.feeFineActionRepository = new FeeFineActionRepository(postgresClient);
    this.accountUpdateService = new AccountUpdateService(okapiHeaders, vertxContext);
    this.patronNoticeService = new PatronNoticeService(vertxContext.owner(), okapiHeaders);
    this.validationService = new DefaultActionValidationService(accountRepository);
  }

  public Future<ActionContext> pay(String accountId, ActionRequest request) {
    return performAction(Action.PAY, accountId, request);
  }

  public Future<ActionContext> waive(String accountId, ActionRequest request) {
    return performAction(Action.WAIVE, accountId, request);
  }

  public Future<ActionContext> transfer(String accountId, ActionRequest request) {
    return performAction(Action.TRANSFER, accountId, request);
  }

  private Future<ActionContext> performAction(Action action, String accountId,
    ActionRequest request) {

    return succeededFuture(new ActionContext(action, accountId, request))
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
      .map(result -> context.withRequestedAmount(new MonetaryValue(amount)));
  }

  private Future<ActionContext> createFeeFineAction(ActionContext context) {
    final ActionRequest request = context.getRequest();
    final Account account = context.getAccount();
    final Action action = context.getAction();
    final MonetaryValue requestedAmount = context.getRequestedAmount();

    MonetaryValue remainingAmountAfterAction = new MonetaryValue(account.getRemaining())
         .subtract(requestedAmount);

    boolean shouldCloseAccount = remainingAmountAfterAction.isZero();
    String actionType = shouldCloseAccount ? action.getFullResult() : action.getPartialResult();

    Feefineaction feeFineAction = new Feefineaction()
      .withAmountAction(requestedAmount.toDouble())
      .withComments(request.getComments())
      .withNotify(request.getNotifyPatron())
      .withTransactionInformation(request.getTransactionInfo())
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withPaymentMethod(request.getPaymentMethod())
      .withAccountId(context.getAccountId())
      .withUserId(account.getUserId())
      .withBalance(remainingAmountAfterAction.toDouble())
      .withTypeAction(actionType)
      .withId(UUID.randomUUID().toString())
      .withDateAction(new Date())
      .withAccountId(context.getAccountId());

    return feeFineActionRepository.save(feeFineAction)
      .map(context.withFeeFineAction(feeFineAction)
        .withShouldCloseAccount(shouldCloseAccount)
      );
  }

  private Future<ActionContext> updateAccount(ActionContext context) {
    final Feefineaction feeFineAction = context.getFeeFineAction();
    final Account account = context.getAccount();
    final Status accountStatus = account.getStatus();

    account.getPaymentStatus().setName(feeFineAction.getTypeAction());

    if (context.getShouldCloseAccount()) {
      accountStatus.setName(CLOSED.getValue());
      account.setRemaining(0.0);
    } else {
      accountStatus.setName(OPEN.getValue());
      account.setRemaining(feeFineAction.getBalance());
    }

    return accountUpdateService.updateAccount(account)
      .map(context);
  }

  private Future<ActionContext> sendPatronNotice(ActionContext context) {
    if (isTrue(context.getRequest().getNotifyPatron())) {
      patronNoticeService.sendPatronNotice(context.getFeeFineAction());
    }
    return succeededFuture(context);
  }

}