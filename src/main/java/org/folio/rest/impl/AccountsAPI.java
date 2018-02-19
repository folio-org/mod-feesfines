/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.AccountdataCollection;
import org.folio.rest.jaxrs.resource.AccountsResource;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.OutStream;
import org.folio.rest.tools.utils.TenantTool;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

/**
 *
 * @author Lluvia
 */
public class AccountsAPI implements AccountsResource {

    public static final String TABLE_NAME_ACCOUNTS = "accounts";
    public static final String VIEW_NAME_ACCOUNT_GROUPS_JOIN = "accounts";

    private final Messages messages = Messages.getInstance();
    private static final String ACCOUNT_ID_FIELD = "'id'";
    private static final String ACCOUNTSAMOUNT_FIELD = "'amount'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(FeeFinesAPI.class);
    String lang = "eng";

    public AccountsAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    /**
     * right now, just query the join view if a cql was passed in, otherwise
     * work with the master users table. this can be optimized in the future to
     * check if there is really a need to use the join view due to cross table
     * cqling - like returning users sorted by group name
     *
     * @param cql
     * @return
     */
    private String getTableName(String cql) {
        if (cql != null) {
            return TABLE_NAME_ACCOUNTS;
        }
        return TABLE_NAME_ACCOUNTS;
    }

    /**
     * check for entries in the cql which reference the group table, indicating
     * a join is needed and update the cql accordingly - by replacing the
     * patronGroup. prefix with g. which is what the view refers to the groups
     * table
     *
     * @param cql
     * @return
     */
    private String convertQuery(String cql) {
        if (cql != null) {
            return cql.replaceAll("(?i)accounts\\.", VIEW_NAME_ACCOUNT_GROUPS_JOIN + ".jsonb");
        }
        return cql;
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(TABLE_NAME_ACCOUNTS + ".jsonb");
        return new CQLWrapper(cql2pgJson, convertQuery(query)).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Override
    public void getAccounts(String query, String orderBy, Order order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        System.out.print("Test account");
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        System.out.println("cadena del query " + query);
        System.out.println("cadena del orderBy " + orderBy);
        System.out.println("cadena del order " + order);
        System.out.println("cadena del offset " + offset);
        System.out.println("cadena del limit " + limit);
        CQLWrapper cql = getCQL(query, limit, offset);
        System.out.println("cadena cl1 " + cql);
        try {
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));

                    String[] fieldList = {"*"};

                    postgresClient.get(TABLE_NAME_ACCOUNTS, Account.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        AccountdataCollection AccountsCollection = new AccountdataCollection();
                                        List<Account> account = (List<Account>) reply.result()[0];
                                        AccountsCollection.setAccounts(account);
                                        AccountsCollection.setTotalRecords((Integer) reply.result()[1]);
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetAccountsResponse.withJsonOK(AccountsCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetAccountsResponse.withPlainInternalServerError(
                                                        reply.cause().getMessage())));
                                    }

                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetAccountsResponse.withPlainInternalServerError(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL UNAM AUNAM");
                        asyncResultHandler.handle(Future.succeededFuture(GetAccountsResponse.withPlainBadRequest(
                                "CQL UNAM Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetAccountsResponse.withPlainInternalServerError(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD UNAM CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetAccountsResponse.withPlainBadRequest(
                        "CQL UNAM Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetAccountsResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }

    }

    @Override
    public void postAccounts(String lang, Account entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);
                logger.info("TEST INFO 1 " + entity + " " + lang + " " + okapiHeaders);

                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    logger.debug("Attempting to save new record");
                    try { 

                        postgresClient.save(beginTx, tableName, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    logger.debug("Save successful");
                                    final Account account = entity;
                                    account.setId(entity.getId());
                                    OutStream stream = new OutStream();
                                    stream.setData(account);
                                    postgresClient.endTx(beginTx, done -> {
                                        asyncResultHandler.handle(Future.succeededFuture(PostAccountsResponse.withJsonCreated(
                                                reply.result(), stream)));
                                    });
                                } else {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PostAccountsResponse.withPlainBadRequest(
                                                    messages.getMessage(
                                                            lang, MessageConsts.UnableToProcessRequest))));

                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostAccountsResponse.withPlainInternalServerError(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        asyncResultHandler.handle(Future.succeededFuture(
                                PostAccountsResponse.withPlainInternalServerError(
                                        e.getMessage())));
                    } 
                });

            });

        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostAccountsResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void getAccountsByAccountId(String accountId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
     try {
            logger.info("Estoy en try 1 " );
            vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
      String tableName = getTableName(null);
 
        try {
            logger.info("Estoy en try 2 " );
                Criteria idCrit = new Criteria();
                idCrit.addField(ACCOUNT_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(accountId);
                Criterion criterion = new Criterion(idCrit);  
            PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName, Account.class, criterion,
                     true, false, getReply -> {
               if(getReply.failed()) {
                 logger.info("GET BY ID ACCOUNT failed: " + getReply.failed() + getReply.succeeded());
                 asyncResultHandler.handle(Future.succeededFuture(
                         GetAccountsByAccountIdResponse.withPlainInternalServerError(
                                 messages.getMessage(lang, MessageConsts.InternalServerError))));
               } else {
               List<Account> accounts = (List<Account>) getReply.result()[0];
                  logger.debug("*******************accountList: " + accounts);
                 if(accounts.size() < 1) {
                   asyncResultHandler.handle(Future.succeededFuture(
                          GetAccountsByAccountIdResponse.withPlainNotFound("Account" +
                                  messages.getMessage(lang,
                                          MessageConsts.ObjectDoesNotExist))));
                 } else if(accounts.size() > 1) {
                   logger.debug("Multiple accounts found with the same id");
                   asyncResultHandler.handle(Future.succeededFuture(
                        GetAccountsByAccountIdResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
                 } else {
                   asyncResultHandler.handle(Future.succeededFuture(
                          GetAccountsByAccountIdResponse.withJsonOK(accounts.get(0))));
                 }
               }
             });
           } catch(Exception e) {
             logger.debug("Error occurred: " + e.getMessage());
             asyncResultHandler.handle(Future.succeededFuture(
                    GetAccountsResponse.withPlainInternalServerError(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
           }

     });
  } catch(Exception e) {
    asyncResultHandler.handle(Future.succeededFuture(
            GetAccountsResponse.withPlainInternalServerError(messages.getMessage(
                    lang, MessageConsts.InternalServerError))));
  }
    }

    @Override
    public void deleteAccountsByAccountId(String accountId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            System.out.println("Metodo DELETE" + accountId);
            System.out.println("Metodo DELETE" + lang);
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);
                Criteria idCrit = new Criteria();
                idCrit.addField(ACCOUNT_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(accountId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            tableName, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    logger.info("Delete succeeded: " + deleteReply.succeeded());
                                    logger.info("Delete fallido: " + deleteReply.failed());
                                    logger.info("Query Result: " + deleteReply.result());
                                    logger.info("Query Result: " + deleteReply.result());
                                    logger.info("Query Result:getUpdated| " + deleteReply.result().getUpdated());
                                    if (deleteReply.result().getUpdated() == 1) {
                                        logger.info("Query Result: Numero de registros elliminados: " + deleteReply.result().getUpdated());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteAccountsByAccountIdResponse.withNoContent()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteAccountsByAccountIdResponse.withPlainNotFound("No se encontro el registro")));
                                    }
                                } else {
                                    logger.info("Fallos? ");

                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteAccountsByAccountIdResponse.withPlainInternalServerError(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteAccountsByAccountIdResponse.withPlainBadRequest(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.debug("Delete failed: " + e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteAccountsByAccountIdResponse.withPlainInternalServerError(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteAccountsByAccountIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void putAccountsByAccountId(String accountId, String lang, Account account, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            System.out.println("Metodo PUT" + accountId);
            System.out.println("Metodo PUT" + lang);
            if (accountId == null) {
                logger.error("No id when PUTting account " + accountId);
                asyncResultHandler.handle(Future.succeededFuture(PutAccountsByAccountIdResponse.withPlainBadRequest("Error")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);

                Criteria idCrit = new Criteria();
                idCrit.addField(ACCOUNT_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(accountId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName,
                            Account.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.debug("Error querying existing username: " + getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutAccountsByAccountIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else {
                                    if (!getReply.succeeded()) {
                                        logger.info("PUT succeeded: " + getReply.succeeded());
                                        logger.info("PUT fallido: " + getReply.failed());
                                        logger.info("PUT Result: " + getReply.result());

                                        logger.info("NO existe en la base");
                                    } else {
                                        try {
                                            logger.info("Si existe en la base");
                                            logger.info("Si existe en la base");

                                            PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                    tableName, account, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    PutAccountsByAccountIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
                                                        } else {
                                                            logger.info("UPDATE EN LA BASE la base");
                                                            logger.info("Si existe en la base");
                                                            logger.info("Si existe en la base");
                                                            logger.info("PUT succeeded: " + putReply.succeeded());
                                                            logger.info("PUT fallido: " + putReply.failed());
                                                            logger.info("PUT Result: " + putReply.result());
                                                            logger.info("PUT Result:getUpdate " + putReply.result().getUpdated());
                                                            if (putReply.result().getUpdated() == 1) {
                                                                logger.info("Query Result: Numero de registros actualizados: " + putReply.result().getUpdated());
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        DeleteAccountsByAccountIdResponse.withNoContent()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        DeleteAccountsByAccountIdResponse.withPlainNotFound("No se encontro el registro")));
                                                            }
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            asyncResultHandler.handle(Future.succeededFuture(
                                                    PutAccountsByAccountIdResponse.withPlainInternalServerError(messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                        }
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutAccountsByAccountIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutAccountsByAccountIdResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    } 

}

