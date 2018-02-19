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
import org.folio.rest.jaxrs.model.Chargeitem;
import org.folio.rest.jaxrs.model.ChargeitemdataCollection;
import org.folio.rest.jaxrs.resource.ChargeitemResource;
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
public class ChargeItemAPI implements ChargeitemResource {

    public static final String TABLE_NAME_CHARGEITEM = "item_information_view";
    public static final String VIEW_NAME_CHARGEITEM_GROUPS_JOIN = "item_information_view";

    private final Messages messages = Messages.getInstance();
    private static final String CHARGEITEM_ID_FIELD = "'id'";
    private static final String TYPECHARGEITEM_FIELD = "'title'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(ChargeItemAPI.class);
    String lang = "eng";

    public ChargeItemAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    /**
     * @param cql
     * @return
     */
    private String getTableName(String cql) {
        if (cql != null) {
            return TABLE_NAME_CHARGEITEM;
        }
        return TABLE_NAME_CHARGEITEM;
    }

    /**
     * @param cql
     * @return
     */
    private String convertQuery(String cql) {
        if (cql != null) {
            return cql.replaceAll("(?i)chargeitem\\.", VIEW_NAME_CHARGEITEM_GROUPS_JOIN + ".jsonb");
        }
        return cql;
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(TABLE_NAME_CHARGEITEM + ".jsonb");
        return new CQLWrapper(cql2pgJson, convertQuery(query)).setLimit(new Limit(limit)).setOffset(new Offset(offset)
);
    }

    @Override
    public void getChargeitem(String query, String orderBy, ChargeitemResource.Order order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
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

                    postgresClient.get(TABLE_NAME_CHARGEITEM, Chargeitem.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        ChargeitemdataCollection ChargeitemCollection = new ChargeitemdataCollection();
                                        List<Chargeitem> chargeitems = (List<Chargeitem>) reply.result()[0];
                                        ChargeitemCollection.setChargeitem(chargeitems);
            logger.info("Estoy en try 2 " );
                                        ChargeitemCollection.setTotalRecords((Integer) reply.result()[1]);
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                ChargeitemResource.GetChargeitemResponse.withJsonOK(ChargeitemCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                ChargeitemResource.GetChargeitemResponse.withPlainInternalServerError(
                                                        reply.cause().getMessage())));
                                    }

                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            ChargeitemResource.GetChargeitemResponse.withPlainInternalServerError(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException"))
 {
                        logger.debug("BAD CQL UNAM AUNAM");
                        asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.GetChargeitemResponse.withPlainBadRequest(
                                "CQL UNAM Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                ChargeitemResource.GetChargeitemResponse.withPlainInternalServerError(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD UNAM CQL");
                asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.GetChargeitemResponse.withPlainBadRequest(
                        "CQL UNAM Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        ChargeitemResource.GetChargeitemResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }

    }

    @Override
    public void postChargeitem(String lang, Chargeitem entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

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
                                    final Chargeitem chargeitem = entity;
                                    chargeitem.setId(entity.getId());
                                    OutStream stream = new OutStream();
                                    stream.setData(chargeitem);
                                    postgresClient.endTx(beginTx, done -> {
                                        asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.PostChargeitemResponse.withJsonCreated(
                                                reply.result(), stream)));
                                    });
                                } else {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            ChargeitemResource.PostChargeitemResponse.withPlainBadRequest(
                                                    messages.getMessage(
                                                            lang, MessageConsts.UnableToProcessRequest))));

                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        ChargeitemResource.PostChargeitemResponse.withPlainInternalServerError(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        asyncResultHandler.handle(Future.succeededFuture(
                                ChargeitemResource.PostChargeitemResponse.withPlainInternalServerError(
                                        e.getMessage())));
                    } 
                });

            });

        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    ChargeitemResource.PostChargeitemResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    }

    @Override
    public void getChargeitemByChargeitemId(String chargeitemId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
          try {
    vertxContext.runOnContext(v -> {
      String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
      String tableName = getTableName(null);

          try {
                Criteria idCrit = new Criteria();
                idCrit.addField(CHARGEITEM_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(chargeitemId);
                Criterion criterion = new Criterion(idCrit);            

             PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName, Chargeitem.class, criterion,
                     true, false, getReply -> {
               if(getReply.failed()) {
                 logger.info("GET BY ID failed: " + getReply.failed() + getReply.succeeded());
                 asyncResultHandler.handle(Future.succeededFuture(
                         ChargeitemResource.GetChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                 messages.getMessage(lang, MessageConsts.InternalServerError))));
               } else {
               List<Chargeitem> chargeitem = (List<Chargeitem>) getReply.result()[0];
                  logger.debug("*******************chargeitemList: " + chargeitem);
                 if(chargeitem.size() < 1) {
                   asyncResultHandler.handle(Future.succeededFuture(
                          ChargeitemResource.GetChargeitemByChargeitemIdResponse.withPlainNotFound("Chargeitem" +
                                  messages.getMessage(lang,
                                          MessageConsts.ObjectDoesNotExist))));
                 } else if(chargeitem.size() > 1) {
                   logger.debug("Multiple chargeitem found with the same id");
                   asyncResultHandler.handle(Future.succeededFuture(
                        ChargeitemResource.GetChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
                 } else {
                   asyncResultHandler.handle(Future.succeededFuture(
                          ChargeitemResource.GetChargeitemByChargeitemIdResponse.withJsonOK(chargeitem.get(0))));
                 }
               }
             });
           } catch(Exception e) {
             logger.debug("Error occurred: " + e.getMessage());
             asyncResultHandler.handle(Future.succeededFuture(
                    ChargeitemResource.GetChargeitemResponse.withPlainInternalServerError(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
           }

     });
  } catch(Exception e) {
    asyncResultHandler.handle(Future.succeededFuture(
            ChargeitemResource.GetChargeitemResponse.withPlainInternalServerError(messages.getMessage(
                    lang, MessageConsts.InternalServerError))));
  }

}
    @Override
    public void deleteChargeitemByChargeitemId(String chargeitemId,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) throws Exception {
        try {
            System.out.println("Metodo DELETE" + chargeitemId);
            System.out.println("Metodo DELETE" + lang);
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);
                Criteria idCrit = new Criteria();
                idCrit.addField(CHARGEITEM_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(chargeitemId);
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
                                                ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withNoContent()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withPlainNotFound("No se encontro el registro")));
                                    }
                                } else {
                                    logger.info("Fallos? ");

                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withPlainBadRequest(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.debug("Delete failed: " + e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    public void putChargeitemByChargeitemId(String chargeitemId,
            String lang, Chargeitem chargeitem,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) throws Exception {

        try {
            if (chargeitemId == null) {
                logger.error("No id when PUTting chargeitem " + chargeitemId);
                asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainBadRequest("Error")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                String tableName = getTableName(null);

                Criteria idCrit = new Criteria();
                idCrit.addField(CHARGEITEM_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(chargeitemId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(tableName,
                            Chargeitem.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.debug("Error querying existing username: " + getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainInternalServerError(
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
                                                    tableName, chargeitem, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
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
                                                                        ChargeitemResource.PutChargeitemByChargeitemIdResponse.withNoContent()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainNotFound("No se encontro el registro")));
                                                            }
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            asyncResultHandler.handle(Future.succeededFuture(
                                                    ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainInternalServerError(messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                        }
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    } 

}

