package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse.respond500WithTextPlain;
import static org.folio.rest.persist.PgUtil.put;
import static org.folio.rest.utils.AccountHelper.isClosedAndHasZeroRemainingAmount;
import static org.folio.rest.utils.MetadataHelper.populateMetadata;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.service.action.context.ActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class AccountUpdateService {
  private static final Logger log = LoggerFactory.getLogger(AccountUpdateService.class);
  private static final String ACCOUNTS_TABLE = "accounts";

  private final AccountRepository accountRepository;
  private final AccountEventPublisher eventPublisher;
  private final Map<String, String> okapiHeaders;
  private final Context context;

  public AccountUpdateService(Map<String, String> okapiHeaders, Context context) {
    this.okapiHeaders = okapiHeaders;
    this.context = context;
    this.accountRepository = new AccountRepository(context, okapiHeaders);
    this.eventPublisher = new AccountEventPublisher(context, okapiHeaders);
  }

  public Future<Response> updateAccount(String accountId, Account account) {
    return put(ACCOUNTS_TABLE, account, accountId, okapiHeaders, context, PutAccountsByAccountIdResponse.class)
      .compose(putResponse -> {
        if (putResponse.getStatus() != HTTP_NO_CONTENT.toInt()) {
          return succeededFuture(putResponse);
        }

        eventPublisher.publishAccountBalanceChangeEvent(account);

        if (isFeeFineWithLoanClosed(account)) {
          return eventPublisher.publishLoanRelatedFeeFineClosedEvent(account)
            .map(putResponse);
        }

        return succeededFuture(putResponse);
      }).recover(error -> {
        log.error("Cannot publish fee/fine closed event [feeFineId - {}, loanId - {}]",
          account.getId(), account.getLoanId(), error);

        return succeededFuture(respond500WithTextPlain(error.getMessage()));
      });
  }

  public Future<Account> updateAccount(Account account, Map<String, String> headers) {
    populateMetadata(account, headers);

    return accountRepository.update(account)
      .onSuccess(a -> eventPublisher.publishAccountBalanceChangeEvent(account));
  }

  public void publishLoanRelatedFeeFineClosedEvent(ActionContext actionContext) {
    actionContext.getAccounts().values().stream()
      .filter(this::isFeeFineWithLoanClosed)
      .distinct()
      .forEach(eventPublisher::publishLoanRelatedFeeFineClosedEvent);
  }

  private boolean isFeeFineWithLoanClosed(Account feeFine) {
    return isFeeFineAssociatedToLoan(feeFine) && isClosedAndHasZeroRemainingAmount(feeFine);
  }

  private boolean isFeeFineAssociatedToLoan(Account feeFine) {
    return StringUtils.isNotBlank(feeFine.getLoanId());
  }
}
