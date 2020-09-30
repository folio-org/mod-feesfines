package org.folio.rest.service.action;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.persist.PostgresClient.getInstance;
import static org.folio.rest.tools.utils.TenantTool.tenantId;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.ActionRequest;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.DefaultActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.service.AccountUpdateService;
import org.folio.rest.service.PatronNoticeService;
import org.folio.rest.service.action.validation.ActionValidationService;

import io.vertx.core.Context;
import io.vertx.core.Future;

public abstract class ActionService {
  protected final Action action;
  protected final AccountRepository accountRepository;
  protected final FeeFineActionRepository feeFineActionRepository;
  protected final AccountUpdateService accountUpdateService;
  protected final ActionValidationService validationService;
  protected final PatronNoticeService patronNoticeService;

  public ActionService(Action action, ActionValidationService validationService,
    Map<String, String> headers, Context context) {

    PostgresClient postgresClient = getInstance(context.owner(), tenantId(headers));

    this.action = action;
    this.accountRepository = new AccountRepository(postgresClient);
    this.feeFineActionRepository = new FeeFineActionRepository(postgresClient);
    this.accountUpdateService = new AccountUpdateService(headers, context);
    this.patronNoticeService = new PatronNoticeService(context.owner(), headers);
    this.validationService = validationService;
  }

  public Future<ActionContext> performAction(String accountId, ActionRequest request) {
    return succeededFuture(new ActionContext(accountId, request))
      .compose(this::findAccount)
      .compose(this::validateAction)
      .compose(this::createFeeFineActions)
      .compose(this::updateAccount)
      .compose(this::sendPatronNotice);
  }

  private Future<ActionContext> findAccount(ActionContext context) {
    return accountRepository.getAccountById(context.getAccountId())
      .map(context::withAccount);
  }

  protected Future<ActionContext> validateAction(ActionContext context) {
    DefaultActionRequest request = (DefaultActionRequest) context.getRequest();
    final String amount = request.getAmount();

    return validationService.validate(context.getAccountId(), context.getAccount(), amount)
      .map(result -> context.withRequestedAmount(new MonetaryValue(amount)));
  }

  protected Future<ActionContext> createFeeFineActions(ActionContext context) {
    final DefaultActionRequest request = (DefaultActionRequest) context.getRequest();
    final Account account = context.getAccount();
    final MonetaryValue requestedAmount = context.getRequestedAmount();

    MonetaryValue remainingAmountAfterAction = new MonetaryValue(account.getRemaining())
      .subtract(requestedAmount);

    boolean isFullAction = remainingAmountAfterAction.isZero();
    String actionType = isFullAction ? action.getFullResult() : action.getPartialResult();

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
      .map(context
        .withFeeFineAction(feeFineAction)
        .withShouldCloseAccount(isFullAction)
      );
  }

  private Future<ActionContext> updateAccount(ActionContext context) {
    final List<Feefineaction> feeFineActions = context.getFeeFineActions();

    if (feeFineActions.isEmpty()) {
      return succeededFuture(context);
    }

    final Feefineaction lastFeeFineAction = feeFineActions.get(feeFineActions.size() - 1);
    final Account account = context.getAccount();
    final Status accountStatus = account.getStatus();

    account.getPaymentStatus().setName(lastFeeFineAction.getTypeAction());

    if (context.isShouldCloseAccount()) {
      accountStatus.setName(CLOSED.getValue());
      account.setRemaining(0.0);
    } else {
      account.setRemaining(lastFeeFineAction.getBalance());
    }

    return accountUpdateService.updateAccount(account)
      .map(context);
  }

  private Future<ActionContext> sendPatronNotice(ActionContext context) {
    if (isTrue(context.getRequest().getNotifyPatron())) {
      context.getFeeFineActions()
        .forEach(patronNoticeService::sendPatronNotice);
    }
    return succeededFuture(context);
  }

}
