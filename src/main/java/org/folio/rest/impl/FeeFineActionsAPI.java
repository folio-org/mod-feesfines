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
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.FeefineactiondataCollection;
import org.folio.rest.jaxrs.resource.FeefineactionsResource;
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
public class FeeFineActionsAPI implements FeefineactionsResource {

    public static final String TABLE_NAME_FEEFINEACTIONS = "feefineactions";
    public static final String VIEW_NAME_FEEFINEACTION_JOIN = "feefineactions";

    private final Messages messages = Messages.getInstance();
    private static final String FEEFINEACTION_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(FeeFineActionsAPI.class);
    String lang = "eng";

    public FeeFineActionsAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    /**
     *
     * @param cql
     * @return
     */
    private String getTableName(String cql) {
        if (cql != null) {
            return TABLE_NAME_FEEFINEACTIONS;
        }
        return TABLE_NAME_FEEFINEACTIONS;
    }

    /**
     *
     * @param cql
     * @return
     */
    private String convertQuery(String cql) {
        if (cql != null) {
            return cql.replaceAll("(?i)feefineactions\\.", VIEW_NAME_FEEFINEACTION_JOIN + ".jsonb");
        }
        return cql;
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(TABLE_NAME_FEEFINEACTIONS + ".jsonb");
        return new CQLWrapper(cql2pgJson, convertQuery(query)).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Override
    public void getFeefineactions(String query, String orderBy, Order order, int offset, int limit, String lang, Map<String,
 String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        System.out.print("Test feefineaction");
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

                    postgresClient.get(TABLE_NAME_FEEFINEACTIONS, Feefineaction.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        FeefineactiondataCollection FeefineactionsCollection = new FeefineactiondataCollection();
                                        List<Feefineaction> feefineaction = (List<Feefineaction>) reply.result()[0];
                                        FeefineactionsCollection.setFeefineactions(feefineaction);
                                        FeefineactionsCollection.setTotalRecords((Integer) reply.result()[1]);
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetFeefineactionsResponse.withJsonOK(FeefineactionsCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetFeefineactionsResponse.withPlainInternalServerError(
                                                        reply.cause().getMessage())));
                                    }

                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetFeefineactionsResponse.withPlainInternalServerError(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException"))
                    {
                        logger.debug("BAD CQL UNAM");
                        asyncResultHandler.handle(Future.succeededFuture(GetFeefineactionsResponse.withPlainBadRequest(
                                "CQL UNAM Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetFeefineactionsResponse.withPlainInternalServerError(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD UNAM CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetFeefineactionsResponse.withPlainBadRequest(
                        "CQL UNAM Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetFeefineactionsResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
              }
          }

    }

    @Override
    public void postFeefineactions(String lang, Feefineaction entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
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
                                    final Feefineaction feefineaction = entity;
                                    feefineaction.setId(entity.getId());
                                    OutStream stream = new OutStream();
                                    stream.setData(feefineaction);
                                    postgresClient.endTx(beginTx, done -> {
                                        asyncResultHandler.handle(Future.succeededFuture(PostFeefineactionsResponse.withJsonCreated(
                                                reply.result(), stream)));
                                    });
                                } else {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PostFeefineactionsResponse.withPlainBadRequest(
                                                    messages.getMessage(
                                                            lang, MessageConsts.UnableToProcessRequest))));

                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostFeefineactionsResponse.withPlainInternalServerError(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        asyncResultHandler.handle(Future.succeededFuture(
                                PostFeefineactionsResponse.withPlainInternalServerError(
                                        e.getMessage())));
                    } 
                });

            });

        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostFeefineactionsResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void getFeefineactionsByFeefineactionId(String feefineactionId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
     try {
            vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
      String tableName = getTableName(null);
 
        try {
                Criteria idCrit = new Criteria();
                idCrit.addField(FEEFINEACTION_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(feefineactionId);
                Criterion criterion = new Criterion(idCrit);  
            PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName, Feefineaction.class, criterion,
                     true, false, getReply -> {
               if(getReply.failed()) {
                 logger.info("GET BY ID FEEFINEACTION failed: " + getReply.failed() + getReply.succeeded());
                 asyncResultHandler.handle(Future.succeededFuture(
                         GetFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                 messages.getMessage(lang, MessageConsts.InternalServerError))));
               } else {
               List<Feefineaction> feefineactions = (List<Feefineaction>) getReply.result()[0];
                  logger.debug("*******************feefineactionList: " + feefineactions);
                 if(feefineactions.size() < 1) {
                   asyncResultHandler.handle(Future.succeededFuture(
                          GetFeefineactionsByFeefineactionIdResponse.withPlainNotFound("Feefineaction" +
                                  messages.getMessage(lang,
                                          MessageConsts.ObjectDoesNotExist))));
                 } else if(feefineactions.size() > 1) {
                   logger.debug("Multiple feefineactions found with the same id");
                   asyncResultHandler.handle(Future.succeededFuture(
                        GetFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
                 } else {
                   asyncResultHandler.handle(Future.succeededFuture(
                          GetFeefineactionsByFeefineactionIdResponse.withJsonOK(feefineactions.get(0))));
                 }
               }
             });
           } catch(Exception e) {
             logger.debug("Error occurred: " + e.getMessage());
             asyncResultHandler.handle(Future.succeededFuture(
                    GetFeefineactionsResponse.withPlainInternalServerError(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
           }
     });
  } catch(Exception e) {
    asyncResultHandler.handle(Future.succeededFuture(
            GetFeefineactionsResponse.withPlainInternalServerError(messages.getMessage(
                    lang, MessageConsts.InternalServerError))));
  }
    }

    @Override
    public void deleteFeefineactionsByFeefineactionId(String feefineactionId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            System.out.println("Metodo DELETE" + feefineactionId);
            System.out.println("Metodo DELETE" + lang);
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);
                Criteria idCrit = new Criteria();
                idCrit.setOperation("=");
                idCrit.setValue(feefineactionId);
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
                                                DeleteFeefineactionsByFeefineactionIdResponse.withNoContent()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteFeefineactionsByFeefineactionIdResponse.withPlainNotFound("No se encontro el registro")));
                                    }
                                } else {
                                    logger.info("Fallos? ");

                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteFeefineactionsByFeefineactionIdResponse.withPlainBadRequest(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.debug("Delete failed: " + e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void putFeefineactionsByFeefineactionId(String feefineactionId, String lang, Feefineaction feefineaction, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            System.out.println("Metodo PUT" + feefineactionId);
            System.out.println("Metodo PUT" + lang);
            if (feefineactionId == null) {
                logger.error("No id when PUTting feefineaction " + feefineactionId);
                asyncResultHandler.handle(Future.succeededFuture(PutFeefineactionsByFeefineactionIdResponse.withPlainBadRequest("Error")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);

                Criteria idCrit = new Criteria();
                idCrit.addField(FEEFINEACTION_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(feefineactionId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName,
                            Feefineaction.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.debug("Error querying existing username: " + getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
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
                                                    tableName, feefineaction, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    PutFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
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
                                                                        DeleteFeefineactionsByFeefineactionIdResponse.withNoContent()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        DeleteFeefineactionsByFeefineactionIdResponse.withPlainNotFound("No se encontro el registro")));
                                                            }
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            asyncResultHandler.handle(Future.succeededFuture(
                                                    PutFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                        }
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    } 

}

