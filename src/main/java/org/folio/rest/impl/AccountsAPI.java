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

public class AccountsAPI implements AccountsResource {

  private static final String TABLE_NAME_ACCOUNTS = "accounts";
  private static final String VIEW_NAME_ACCOUNT_GROUPS_JOIN = "accounts";
  private static final String ACCOUNT_ID_FIELD = "'id'";
  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(FeeFinesAPI.class);

  public AccountsAPI(Vertx vertx, String tenantId) {
    PostgresClient.getInstance(vertx, tenantId).setIdField("id");
  }

  private String getTableName(String cql) {
    if (cql != null) {
      return TABLE_NAME_ACCOUNTS;
    }
    return TABLE_NAME_ACCOUNTS;
  }

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
  public void getAccounts(String query, String orderBy, Order order, int offset, int limit, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) throws Exception {
    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
    CQLWrapper cql = getCQL(query, limit, offset);
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
  public void postAccounts(String lang, Account entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        String tableName = getTableName(null);
        PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);
        postgresClient.startTx(beginTx -> {
          try {
            postgresClient.save(beginTx, tableName, entity, reply -> {
              try {
                if (reply.succeeded()) {
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
  public void getAccountsByAccountId(String accountId, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        String tableName = getTableName(null);
        try {
          Criteria idCrit = new Criteria();
          idCrit.addField(ACCOUNT_ID_FIELD);
          idCrit.setOperation("=");
          idCrit.setValue(accountId);
          Criterion criterion = new Criterion(idCrit);
          PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName, Account.class, criterion,
              true, false, getReply -> {
                if (getReply.failed()) {
                  logger.error(getReply.result());
                  asyncResultHandler.handle(Future.succeededFuture(
                      GetAccountsByAccountIdResponse.withPlainInternalServerError(
                          messages.getMessage(lang, MessageConsts.InternalServerError))));
                } else {
                  List<Account> accounts = (List<Account>) getReply.result()[0];
                  if (accounts.size() < 1) {
                    asyncResultHandler.handle(Future.succeededFuture(
                        GetAccountsByAccountIdResponse.withPlainNotFound("Account"
                            + messages.getMessage(lang,
                                MessageConsts.ObjectDoesNotExist))));
                  } else if (accounts.size() > 1) {
                    logger.error("Multiple accounts found with the same id");
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
        } catch (Exception e) {
          logger.debug(e.getMessage());
          asyncResultHandler.handle(Future.succeededFuture(
              GetAccountsResponse.withPlainInternalServerError(messages.getMessage(
                  lang, MessageConsts.InternalServerError))));
        }
      });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(
          GetAccountsResponse.withPlainInternalServerError(messages.getMessage(
              lang, MessageConsts.InternalServerError))));
    }
  }

  @Override
  public void deleteAccountsByAccountId(String accountId, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    try {
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
                  if (deleteReply.result().getUpdated() == 1) {
                    asyncResultHandler.handle(Future.succeededFuture(
                        DeleteAccountsByAccountIdResponse.withNoContent()));
                  } else {
                    asyncResultHandler.handle(Future.succeededFuture(
                        DeleteAccountsByAccountIdResponse.withPlainNotFound("Record not found")));
                  }
                } else {
                  logger.error(deleteReply.result());
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
          logger.error(e.getMessage());
          asyncResultHandler.handle(
              Future.succeededFuture(
                  DeleteAccountsByAccountIdResponse.withPlainInternalServerError(
                      messages.getMessage(lang,
                          MessageConsts.InternalServerError))));
        }
      });
    } catch (Exception e) {
      logger.error(e.getMessage());
      asyncResultHandler.handle(
          Future.succeededFuture(
              DeleteAccountsByAccountIdResponse.withPlainInternalServerError(
                  messages.getMessage(lang,
                      MessageConsts.InternalServerError))));
    }
  }

  @Override
  public void putAccountsByAccountId(String accountId, String lang, Account account,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
    try {
      if (accountId == null) {
        logger.error("accountId is missing");
        asyncResultHandler.handle(Future.succeededFuture(PutAccountsByAccountIdResponse.withPlainBadRequest("accountId is missing")));
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
                  logger.error(getReply.cause().getLocalizedMessage());
                  asyncResultHandler.handle(Future.succeededFuture(
                      PutAccountsByAccountIdResponse.withPlainInternalServerError(
                          messages.getMessage(lang,
                              MessageConsts.InternalServerError))));
                } else {
                  if (!getReply.succeeded()) {
                    logger.error(getReply.result());
                  } else {
                    try {
                      PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                          tableName, account, criterion, true, putReply -> {
                            if (putReply.failed()) {
                              asyncResultHandler.handle(Future.succeededFuture(
                                  PutAccountsByAccountIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
                            } else {
                              if (putReply.result().getUpdated() == 1) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                    DeleteAccountsByAccountIdResponse.withNoContent()));
                              } else {
                                asyncResultHandler.handle(Future.succeededFuture(
                                    DeleteAccountsByAccountIdResponse.withPlainNotFound("Accounts not found")));
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
