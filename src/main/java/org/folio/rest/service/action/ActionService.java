package org.folio.rest.service.action;

import static io.vertx.core.Future.succeededFuture;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.domain.FeeFineStatus.CLOSED;

import java.util.List;
import java.util.Map;

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
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;

public abstract class ActionService {
  final AccountRepository accountRepository;
  final FeeFineActionRepository feeFineActionRepository;
  final AccountUpdateService accountUpdateService;
  final ActionValidationService validationService;
  final PatronNoticeService patronNoticeService;

  public ActionService(ActionValidationService validationService,
    Map<String, String> headers, Context context) {

    PostgresClient postgresClient = PostgresClient.getInstance(context.owner(),
      TenantTool.tenantId(headers));

    this.accountRepository = new AccountRepository(postgresClient);
    this.feeFineActionRepository = new FeeFineActionRepository(postgresClient);
    this.accountUpdateService = new AccountUpdateService(headers, context);
    this.patronNoticeService = new PatronNoticeService(context.owner(), headers);
    this.validationService = validationService;
  }

  Future<ActionContext> performAction(Action action, String accountId,
    ActionRequest request) {

    return succeededFuture(new ActionContext(action, accountId, request))
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

  private Future<ActionContext> validateAction(ActionContext context) {
    final String amount = context.getRequest().getAmount();

    return validationService.validate(context.getAccount(), amount)
      .map(result -> context.withRequestedAmount(new MonetaryValue(amount)));
  }

  abstract Future<ActionContext> createFeeFineActions(ActionContext context);

  private Future<ActionContext> updateAccount(ActionContext context) {
    final List<Feefineaction> feeFineActions = context.getFeeFineActions();
    final Feefineaction feeFineAction = feeFineActions.get(feeFineActions.size() - 1);
    final Account account = context.getAccount();
    final Status accountStatus = account.getStatus();

    account.getPaymentStatus().setName(feeFineAction.getTypeAction());

    if (context.getShouldCloseAccount()) {
      accountStatus.setName(CLOSED.getValue());
      account.setRemaining(0.0);
    } else {
      account.setRemaining(feeFineAction.getBalance());
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