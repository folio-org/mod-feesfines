package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.AccountdataCollection;
import org.folio.rest.jaxrs.resource.Accounts;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.facets.FacetField;
import org.folio.rest.persist.facets.FacetManager;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;
import org.folio.rest.jaxrs.model.AccountsGetOrder;

public class AccountsAPI implements Accounts {

    private static final String ACCOUNTS_TABLE = "accounts";
    private static final String ACCOUNT_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Messages messages = Messages.getInstance();
    private final Logger logger = LoggerFactory.getLogger(AccountsAPI.class);

    public AccountsAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(ACCOUNTS_TABLE + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
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
                                        AccountdataCollection accountCollection = new AccountdataCollection();
                                        List<Account> accounts = reply.result().getResults();
                                        accountCollection.setAccounts(accounts);
                                        accountCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                        accountCollection.setResultInfo(reply.result().getResultInfo());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetAccountsResponse.respond200WithApplicationJson(accountCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetAccountsResponse.respond500WithTextPlain(
                                                        reply.cause().getMessage())));
                                    }
                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetAccountsResponse.respond500WithTextPlain(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetAccountsResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetAccountsResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetAccountsResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetAccountsResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void postAccounts(String lang, Account entity, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {
                        postgresClient.save(beginTx, ACCOUNTS_TABLE, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final Account account = entity;
                                    account.setId(entity.getId());
                                    postgresClient.endTx(beginTx, done
                                            -> asyncResultHandler.handle(
                                                    Future.succeededFuture(
                                                            PostAccountsResponse
                                                                    .respond201WithApplicationJson(account,
                                                                            PostAccountsResponse.headersFor201().withLocation(reply.result())))));

                                } else {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PostAccountsResponse.respond400WithTextPlain(
                                                    messages.getMessage(
                                                            lang, MessageConsts.UnableToProcessRequest))));
                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostAccountsResponse.respond500WithTextPlain(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        asyncResultHandler.handle(Future.succeededFuture(
                                PostAccountsResponse.respond500WithTextPlain(
                                        e.getMessage())));
                    }
                });
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostAccountsResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
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
                    idCrit.setValue(accountId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(ACCOUNTS_TABLE, Account.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetAccountsByAccountIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Account> accountList = getReply.result().getResults();
                                    if (accountList.isEmpty()) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetAccountsByAccountIdResponse.respond404WithTextPlain("Account"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (accountList.size() > 1) {
                                        logger.error("Multiple accounts found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetAccountsByAccountIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetAccountsByAccountIdResponse.respond200WithApplicationJson(accountList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetAccountsResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
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
                idCrit.setValue(accountId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            ACCOUNTS_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteAccountsByAccountIdResponse.respond204()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteAccountsByAccountIdResponse.respond404WithTextPlain("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteAccountsByAccountIdResponse.respond500WithTextPlain(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteAccountsByAccountIdResponse.respond400WithTextPlain(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteAccountsByAccountIdResponse.respond500WithTextPlain(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteAccountsByAccountIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void putAccountsByAccountId(String accountId, String lang, Account entity,
            Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            if (accountId == null) {
                logger.error("accountId is missing");
                asyncResultHandler.handle(Future.succeededFuture(PutAccountsByAccountIdResponse.respond400WithTextPlain("accountId is missing")));
            }

            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(ACCOUNT_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(accountId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(ACCOUNTS_TABLE,
                            Account.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutAccountsByAccountIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else if (getReply.result().getResults().size() == 1) {
                                    try {
                                        PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                ACCOUNTS_TABLE, entity, criterion, true, putReply -> {
                                                    if (putReply.failed()) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutAccountsByAccountIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                                                    } else if (putReply.result().getUpdated() == 1) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutAccountsByAccountIdResponse.respond204()));
                                                    }
                                                });
                                    } catch (Exception e) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PutAccountsByAccountIdResponse.respond500WithTextPlain(messages.getMessage(lang,
                                                        MessageConsts.InternalServerError))));
                                    }
                                } else if (getReply.result().getResults().isEmpty()) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutAccountsByAccountIdResponse.respond404WithTextPlain("Record Not Found")));
                                } else if (getReply.result().getResults().size() > 1) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutAccountsByAccountIdResponse.respond404WithTextPlain("Multiple account records")));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutAccountsByAccountIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutAccountsByAccountIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }
}
