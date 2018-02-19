/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Feefinehistory;
import org.folio.rest.jaxrs.model.FeefinehistorydataCollection;
import org.folio.rest.jaxrs.resource.FeefinehistoryResource;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.OutStream;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.utils.ValidationHelper;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 *
 * @author Lluvia
 */
public class FeeFineHistoryAPI implements FeefinehistoryResource {

    public static final String TABLE_NAME_FEEFINEHISTORY = "feefine_history_view";
    public static final String VIEW_NAME_FEEFINE_GROUPS_JOIN = "feefine_history_view";

    private final Messages messages = Messages.getInstance();
    private static final String FEEFINE_ID_FIELD = "'id'";
    private static final String TYPEFEEFINEHISTORY_FIELD = "'feeFineType'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(FeeFineHistoryAPI.class);
    String lang = "eng";

    public FeeFineHistoryAPI(Vertx vertx, String tenantId) {
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
            return TABLE_NAME_FEEFINEHISTORY;
        }
        return TABLE_NAME_FEEFINEHISTORY;
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
            return cql.replaceAll("(?i)feefinehistory\\.", VIEW_NAME_FEEFINE_GROUPS_JOIN + ".jsonb");
        }
        return cql;
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(TABLE_NAME_FEEFINEHISTORY + ".jsonb");
        return new CQLWrapper(cql2pgJson, convertQuery(query)).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Override
    public void getFeefinehistory(String query, String orderBy, FeefinehistoryResource.Order order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        System.out.print("Test");
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

                    postgresClient.get(TABLE_NAME_FEEFINEHISTORY, Feefinehistory.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        FeefinehistorydataCollection FeefinehistoryCollection = new FeefinehistorydataCollection();
                                        List<Feefinehistory> feefinehistorys = (List<Feefinehistory>) reply.result()[0];
                                        FeefinehistoryCollection.setFeefinehistory(feefinehistorys);
            logger.info("Estoy en try 2 " );
                                        FeefinehistoryCollection.setTotalRecords((Integer) reply.result()[1]);
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                FeefinehistoryResource.GetFeefinehistoryResponse.withJsonOK(FeefinehistoryCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                FeefinehistoryResource.GetFeefinehistoryResponse.withPlainInternalServerError(
                                                        reply.cause().getMessage())));
                                    }

                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            FeefinehistoryResource.GetFeefinehistoryResponse.withPlainInternalServerError(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL UNAM AUNAM");
                        asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.GetFeefinehistoryResponse.withPlainBadRequest(
                                "CQL UNAM Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                FeefinehistoryResource.GetFeefinehistoryResponse.withPlainInternalServerError(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD UNAM CQL");
                asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.GetFeefinehistoryResponse.withPlainBadRequest(
                        "CQL UNAM Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        FeefinehistoryResource.GetFeefinehistoryResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }

    }

    @Override
    public void postFeefinehistory(String lang, Feefinehistory entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

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
                                    final Feefinehistory feefine = entity;
                                    feefine.setId(entity.getId());
                                    OutStream stream = new OutStream();
                                    stream.setData(feefine);
                                    postgresClient.endTx(beginTx, done -> {
                                        asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.PostFeefinehistoryResponse.withJsonCreated(
                                                reply.result(), stream)));
                                    });
                                } else {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            FeefinehistoryResource.PostFeefinehistoryResponse.withPlainBadRequest(
                                                    messages.getMessage(
                                                            lang, MessageConsts.UnableToProcessRequest))));

                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        FeefinehistoryResource.PostFeefinehistoryResponse.withPlainInternalServerError(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        asyncResultHandler.handle(Future.succeededFuture(
                                FeefinehistoryResource.PostFeefinehistoryResponse.withPlainInternalServerError(
                                        e.getMessage())));
                    } 
                });

            });

        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    FeefinehistoryResource.PostFeefinehistoryResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    }

    @Override
    public void getFeefinehistoryByFeefinehistoryId(String feefineId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
          try {
            logger.info("Estoy en try 1 " );
    vertxContext.runOnContext(v -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
      String tableName = getTableName(null);

          try {
            logger.info("Estoy en try 2 " );
                Criteria idCrit = new Criteria();
                idCrit.addField(FEEFINE_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(feefineId);
                Criterion criterion = new Criterion(idCrit);            

             PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName, Feefinehistory.class, criterion,
                     true, false, getReply -> {
               if(getReply.failed()) {
                 logger.info("GET BY ID failed: " + getReply.failed() + getReply.succeeded());
                 asyncResultHandler.handle(Future.succeededFuture(
                         FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                 messages.getMessage(lang, MessageConsts.InternalServerError))));
               } else {
               List<Feefinehistory> feefinehistory = (List<Feefinehistory>) getReply.result()[0];
                  logger.debug("*******************feefineList: " + feefinehistory);
                 if(feefinehistory.size() < 1) {
                   asyncResultHandler.handle(Future.succeededFuture(
                          FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse.withPlainNotFound("Feefinehistory" +
                                  messages.getMessage(lang,
                                          MessageConsts.ObjectDoesNotExist))));
                 } else if(feefinehistory.size() > 1) {
                   logger.debug("Multiple feefinehistory found with the same id");
                   asyncResultHandler.handle(Future.succeededFuture(
                        FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
                 } else {
                   asyncResultHandler.handle(Future.succeededFuture(
                          FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse.withJsonOK(feefinehistory.get(0))));
                 }
               }
             });
           } catch(Exception e) {
             logger.debug("Error occurred: " + e.getMessage());
             asyncResultHandler.handle(Future.succeededFuture(
                    FeefinehistoryResource.GetFeefinehistoryResponse.withPlainInternalServerError(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
           }

     });
  } catch(Exception e) {
    asyncResultHandler.handle(Future.succeededFuture(
            FeefinehistoryResource.GetFeefinehistoryResponse.withPlainInternalServerError(messages.getMessage(
                    lang, MessageConsts.InternalServerError))));
  }

}
    @Override
    public void deleteFeefinehistoryByFeefinehistoryId(String feefineId,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) throws Exception {
        try {
            System.out.println("Metodo DELETE" + feefineId);
            System.out.println("Metodo DELETE" + lang);
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);
                Criteria idCrit = new Criteria();
                idCrit.addField(FEEFINE_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(feefineId);
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
                                                FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse.withNoContent()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse.withPlainNotFound("No se encontro el registro")));
                                    }
                                } else {
                                    logger.info("Fallos? ");

                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse.withPlainBadRequest(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.debug("Delete failed: " + e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    public void putFeefinehistoryByFeefinehistoryId(String feefineId,
            String lang, Feefinehistory feefine,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) throws Exception {

        try {
            System.out.println("Metodo PUT" + feefineId);
            System.out.println("Metodo PUT" + lang);
            if (feefineId == null) {
                logger.error("No id when PUTting feefine " + feefineId);
                asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainBadRequest("Error")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);

                Criteria idCrit = new Criteria();
                idCrit.addField(FEEFINE_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(feefineId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName,
                            Feefinehistory.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.debug("Error querying existing username: " + getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
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
                                                    tableName, feefine, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
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
                                                                        FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withNoContent()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainNotFound("No se encontro el registro")));
                                                            }
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            asyncResultHandler.handle(Future.succeededFuture(
                                                    FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                        }
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    } 

}


