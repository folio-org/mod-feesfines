package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.CQL2PgJSONException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.client.InventoryClient;
import org.folio.rest.domain.Action;
import org.folio.rest.domain.ActionRequest;
import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.AccountdataCollection;
import org.folio.rest.jaxrs.model.AccountsGetOrder;
import org.folio.rest.jaxrs.model.ActionSuccessResponse;
import org.folio.rest.jaxrs.model.ActionFailureResponse;
import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.jaxrs.model.CheckActionRequest;
import org.folio.rest.jaxrs.model.CheckActionResponse;
import org.folio.rest.jaxrs.model.DefaultActionRequest;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.HoldingsRecords;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Items;
import org.folio.rest.jaxrs.resource.Accounts;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.facets.FacetField;
import org.folio.rest.persist.facets.FacetManager;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.service.AccountEventPublisher;
import org.folio.rest.service.AccountUpdateService;
import org.folio.rest.service.action.ActionContext;
import org.folio.rest.service.action.CancelActionService;
import org.folio.rest.service.action.PayActionService;
import org.folio.rest.service.action.RefundActionService;
import org.folio.rest.service.action.TransferActionService;
import org.folio.rest.service.action.WaiveActionService;
import org.folio.rest.service.action.validation.ActionValidationService;
import org.folio.rest.service.action.validation.DefaultActionValidationService;
import org.folio.rest.service.action.validation.RefundActionValidationService;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.utils.ActionResultAdapter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class AccountsAPI implements Accounts {
  private final static Logger logger = LoggerFactory.getLogger(AccountsAPI.class);
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String ACCOUNT_ID_FIELD = "'id'";
  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
  private final Messages messages = Messages.getInstance();

  private CQLWrapper getCQL(String query, int limit, int offset) throws CQL2PgJSONException, IOException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(ACCOUNTS_TABLE + ".jsonb");
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
  }

  private Future<Void> setAdditionalFields(Vertx vertx, Map<String, String> okapiHeaders,
    List<Account> accounts) {

    if (accounts == null) {
      return succeededFuture(null);
    }

    InventoryClient inventoryClient = new InventoryClient(WebClient.create(vertx), okapiHeaders);

    List<String> itemIds = accounts.stream()
      .map(Account::getItemId)
      .collect(Collectors.toList());

    return succeededFuture(new AdditionalFieldsContext(null, null))
      .compose(ctx -> inventoryClient.getItemsById(itemIds)
          .map(ctx::withItems))
      .compose(ctx -> inventoryClient.getHoldingsById(ctx.items.getItems().stream()
        .map(Item::getHoldingsRecordId)
        .collect(Collectors.toList()))
        .map(ctx::withHoldings))
      .compose(ctx -> {
        accounts.forEach(account -> {
          Optional<Item> item = ctx.items.getItems().stream()
            .filter(i -> account.getItemId().equals(i.getId()))
            .findAny();

          Optional<HoldingsRecord> holding = Optional.empty();
          if (item.isPresent() && item.get().getHoldingsRecordId() != null) {
            holding = ctx.holdings.getHoldingsRecords().stream()
              .filter(h -> item.get().getHoldingsRecordId().equals(h.getId()))
              .findAny();
          }

          String holdingRecordsId = item.map(Item::getHoldingsRecordId).orElse("");
          String instanceId = holding.map(HoldingsRecord::getInstanceId).orElse("");

          account.setHoldingsRecordId(holdingRecordsId);
          account.setInstanceId(instanceId);
        });

        return succeededFuture(null);
      });
  }

    @Validate
    @Override
    public void getAccounts(String query, String orderBy, AccountsGetOrder order, int offset, int limit, List<String> facets, String lang,
            Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        List<FacetField> facetList = FacetManager.convertFacetStrings2FacetFields(facets, "jsonb");
        try {
            CQLWrapper cql = getCQL(query, limit, offset);
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
                    String[] fieldList = {"*"};

                    postgresClient.get(ACCOUNTS_TABLE, Account.class, fieldList, cql,
                            true, false, facetList, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                      List<Account> accounts = reply.result().getResults();

                                      setAdditionalFields(vertxContext.owner(), okapiHeaders, accounts)
                                        .onComplete(accountsResult -> {
                                          AccountdataCollection accountCollection = new AccountdataCollection();
                                          accountCollection.setAccounts(accounts);
                                          accountCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                          accountCollection.setResultInfo(reply.result().getResultInfo());
                                          asyncResultHandler.handle(succeededFuture(
                                            GetAccountsResponse.respond200WithApplicationJson(accountCollection)));
                                        });
                                    } else {
                                        asyncResultHandler.handle(succeededFuture(
                                                GetAccountsResponse.respond500WithTextPlain(
                                                        reply.cause().getMessage())));
                                    }
                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(succeededFuture(
                                            GetAccountsResponse.respond500WithTextPlain(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(succeededFuture(GetAccountsResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(succeededFuture(
                                GetAccountsResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (IOException | CQL2PgJSONException e) {
            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(succeededFuture(GetAccountsResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(succeededFuture(
                        GetAccountsResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void postAccounts(String lang,
                             Account entity,
                             Map<String, String> okapiHeaders,
                             Handler<AsyncResult<Response>> asyncResultHandler,
                             Context vertxContext) {

      PgUtil.post(ACCOUNTS_TABLE, entity, okapiHeaders, vertxContext,
        PostAccountsResponse.class, post -> {
          if (post.succeeded()) {
            new AccountEventPublisher(vertxContext, okapiHeaders)
              .publishAccountBalanceChangeEvent(entity);
          }
          asyncResultHandler.handle(post);
        });
    }

    @Validate
    @Override
    public void getAccountsByAccountId(String accountId, String lang, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(ACCOUNT_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setVal(accountId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(ACCOUNTS_TABLE, Account.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(succeededFuture(
                                            GetAccountsByAccountIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Account> accountList = getReply.result().getResults();
                                    if (accountList.isEmpty()) {
                                        asyncResultHandler.handle(succeededFuture(
                                                GetAccountsByAccountIdResponse.respond404WithTextPlain("Account"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (accountList.size() > 1) {
                                        logger.error("Multiple accounts found with the same id");
                                        asyncResultHandler.handle(succeededFuture(
                                                GetAccountsByAccountIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                          setAdditionalFields(vertxContext.owner(), okapiHeaders, accountList)
                                            .onComplete(accountsResult -> asyncResultHandler.handle(
                                              succeededFuture(
                                                GetAccountsByAccountIdResponse.respond200WithApplicationJson(
                                                  accountList.get(0)))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(succeededFuture(
                            GetAccountsResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            asyncResultHandler.handle(succeededFuture(
                    GetAccountsResponse.respond500WithTextPlain(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void deleteAccountsByAccountId(String accountId, String lang, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                Criteria idCrit = new Criteria();
                idCrit.addField(ACCOUNT_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(accountId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            ACCOUNTS_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().rowCount() == 1) {
                                        new AccountEventPublisher(vertxContext, okapiHeaders)
                                          .publishDeletedAccountBalanceChangeEvent(accountId);
                                        asyncResultHandler.handle(succeededFuture(
                                                DeleteAccountsByAccountIdResponse.respond204()));
                                    } else {
                                        asyncResultHandler.handle(succeededFuture(
                                                DeleteAccountsByAccountIdResponse.respond404WithTextPlain("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(succeededFuture(DeleteAccountsByAccountIdResponse.respond500WithTextPlain(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(succeededFuture(DeleteAccountsByAccountIdResponse.respond400WithTextPlain(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            succeededFuture(
                                    DeleteAccountsByAccountIdResponse.respond500WithTextPlain(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            asyncResultHandler.handle(
                    succeededFuture(
                            DeleteAccountsByAccountIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void putAccountsByAccountId(String accountId, String lang,
      Account entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

      new AccountUpdateService(okapiHeaders, vertxContext)
        .updateAccount(accountId, entity)
        .thenAccept(asyncResultHandler::handle);
    }

  @Override
  public void postAccountsCheckPayByAccountId(String accountId, CheckActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkAction(accountId, request, asyncResultHandler,
      new DefaultActionValidationService(new AccountRepository(vertxContext, okapiHeaders)));
  }

  @Override
  public void postAccountsCheckWaiveByAccountId(String accountId, CheckActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkAction(accountId, request, asyncResultHandler,
      new DefaultActionValidationService(new AccountRepository(vertxContext, okapiHeaders)));
  }

  @Override
  public void postAccountsCheckTransferByAccountId(String accountId, CheckActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkAction(accountId, request, asyncResultHandler,
      new DefaultActionValidationService(new AccountRepository(vertxContext, okapiHeaders)));
  }

  @Override
  public void postAccountsCheckRefundByAccountId(String accountId, CheckActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    checkAction(accountId, request, asyncResultHandler,
      new RefundActionValidationService(okapiHeaders, vertxContext));
  }

  private void checkAction(String accountId, CheckActionRequest request,
    Handler<AsyncResult<Response>> asyncResultHandler,
    ActionValidationService validationService) {

    String rawAmount = request.getAmount();

    validationService.validate(accountId, rawAmount)
      .onSuccess(result -> {
        CheckActionResponse response = new CheckActionResponse()
          .withAccountId(accountId)
          .withAmount(result.getFormattedAmount())
          .withAllowed(true)
          .withRemainingAmount(result.getRemainingAmount());
        asyncResultHandler.handle(Future.succeededFuture(
          PostAccountsCheckPayByAccountIdResponse
            .respond200WithApplicationJson(response)));
      }).onFailure(throwable -> {
      String errorMessage = throwable.getLocalizedMessage();
      if (throwable instanceof FailedValidationException) {
        CheckActionResponse response = new CheckActionResponse()
          .withAccountId(accountId)
          .withAmount(request.getAmount())
          .withAllowed(false)
          .withErrorMessage(errorMessage);
        asyncResultHandler.handle(Future.succeededFuture(
          PostAccountsCheckPayByAccountIdResponse
            .respond422WithApplicationJson(response)));
      } else if (throwable instanceof AccountNotFoundValidationException) {
        asyncResultHandler.handle(Future.succeededFuture(
          PostAccountsCheckPayByAccountIdResponse
            .respond404WithTextPlain(errorMessage)));
      } else {
        asyncResultHandler.handle(Future.succeededFuture(
          PostAccountsCheckPayByAccountIdResponse
            .respond500WithTextPlain(errorMessage)));
      }
    });
  }

  @Override
  public void postAccountsPayByAccountId(String accountId, DefaultActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new PayActionService(okapiHeaders, vertxContext)
      .performAction(accountId, request)
      .onComplete(result -> handleActionResult(accountId, request, result, asyncResultHandler,
        Action.PAY));
  }

  @Override
  public void postAccountsWaiveByAccountId(String accountId, DefaultActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new WaiveActionService(okapiHeaders, vertxContext)
      .performAction(accountId, request)
      .onComplete(result -> handleActionResult(accountId, request, result, asyncResultHandler,
        Action.WAIVE));
  }

  @Override
  public void postAccountsTransferByAccountId(String accountId, DefaultActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new TransferActionService(okapiHeaders, vertxContext)
      .performAction(accountId, request)
      .onComplete(result -> handleActionResult(accountId, request, result, asyncResultHandler,
        Action.TRANSFER));
  }

  @Override
  public void postAccountsRefundByAccountId(String accountId, DefaultActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new RefundActionService(okapiHeaders, vertxContext)
      .performAction(accountId, request)
      .onComplete(result -> handleActionResult(accountId, request, result, asyncResultHandler,
        Action.REFUND));
  }

  @Override
  public void postAccountsCancelByAccountId(String accountId, CancelActionRequest request,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    new CancelActionService(okapiHeaders, vertxContext)
      .performAction(accountId, request)
      .onComplete(result -> handleActionResult(accountId, request, result, asyncResultHandler,
        Action.CANCELLED));
  }

  private void handleActionResult(String accountId, ActionRequest request,
    AsyncResult<ActionContext> asyncResult, Handler<AsyncResult<Response>> asyncResultHandler,
    Action action) {

    ActionResultAdapter resultAdapter = action.getActionResultAdapter();
    if (resultAdapter == null) {
      logger.error("Unprocessable action: " + action.name());
      return;
    }

    if (asyncResult.succeeded()) {
      final ActionContext actionContext = asyncResult.result();
      ActionSuccessResponse response = new ActionSuccessResponse()
        .withAccountId(accountId);
      if (actionContext.getRequestedAmount() != null) {
        response.withAmount(actionContext.getRequestedAmount().toString());
      }
      asyncResultHandler.handle(succeededFuture(resultAdapter.to201(response)));
    }
    else if (asyncResult.failed()) {
      final Throwable cause = asyncResult.cause();
      String errorMessage = cause.getLocalizedMessage();
      if (cause instanceof FailedValidationException) {
        ActionFailureResponse response = new ActionFailureResponse()
          .withAccountId(accountId)
          .withErrorMessage(errorMessage);
        if (Action.CANCELLED != action) {
          DefaultActionRequest defaultActionRequest = (DefaultActionRequest) request;
          response.withAmount(defaultActionRequest.getAmount());
        }
        asyncResultHandler.handle(succeededFuture(resultAdapter.to422(response)));
      } else if (cause instanceof AccountNotFoundValidationException) {
        asyncResultHandler.handle(succeededFuture(resultAdapter.to404(errorMessage)));
      } else {
        asyncResultHandler.handle(succeededFuture(resultAdapter.to500(errorMessage)));
      }
    }
  }

  private static class AdditionalFieldsContext {
    final Items items;
    final HoldingsRecords holdings;

    private AdditionalFieldsContext(Items items, HoldingsRecords holdings) {
      this.items = items;
      this.holdings = holdings;
    }

    private AdditionalFieldsContext withItems(Items items) {
      return new AdditionalFieldsContext(items, this.holdings);
    }

    private AdditionalFieldsContext withHoldings(HoldingsRecords holdings) {
      return new AdditionalFieldsContext(this.items, holdings);
    }
  }
}
