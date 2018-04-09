package org.folio.rest.impl;

import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
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
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ChargeItemAPI implements ChargeitemResource {

    public static final String CHARGEITEM_VIEW = "item_information_view";

    private final Messages messages = Messages.getInstance();
    private static final String CHARGEITEM_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(ChargeItemAPI.class);

    public ChargeItemAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(CHARGEITEM_VIEW + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset)
        );
    }

    @Override
    public void getChargeitem(String query, String orderBy, ChargeitemResource.Order order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        CQLWrapper cql = getCQL(query, limit, offset);
        try {
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));

                    String[] fieldList = {"*"};

                    postgresClient.get(CHARGEITEM_VIEW, Chargeitem.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        ChargeitemdataCollection chargeitemCollection = new ChargeitemdataCollection();
                                        List<Chargeitem> chargeitems = (List<Chargeitem>) reply.result().getResults();
                                        chargeitemCollection.setChargeitem(chargeitems);
                                        chargeitemCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                ChargeitemResource.GetChargeitemResponse.withJsonOK(chargeitemCollection)));
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
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL ");
                        asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.GetChargeitemResponse.withPlainBadRequest(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
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
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.GetChargeitemResponse.withPlainBadRequest(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
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

                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {
                        postgresClient.save(beginTx, CHARGEITEM_VIEW, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
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

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(CHARGEITEM_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setValue(chargeitemId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(CHARGEITEM_VIEW, Chargeitem.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            ChargeitemResource.GetChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Chargeitem> chargeitemList = (List<Chargeitem>) getReply.result().getResults();
                                    if (chargeitemList.size() < 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                ChargeitemResource.GetChargeitemByChargeitemIdResponse.withPlainNotFound("Chargeitem"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (chargeitemList.size() > 1) {
                                        logger.error("Multiple chargeitem found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                ChargeitemResource.GetChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                ChargeitemResource.GetChargeitemByChargeitemIdResponse.withJsonOK(chargeitemList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            ChargeitemResource.GetChargeitemResponse.withPlainInternalServerError(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
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
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(CHARGEITEM_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(chargeitemId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            CHARGEITEM_VIEW, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withNoContent()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withPlainNotFound("Record Not Found")));
                                    }
                                } else {
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
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            ChargeitemResource.DeleteChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void putChargeitemByChargeitemId(String chargeitemId,
            String lang, Chargeitem chargeitem,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) throws Exception {

        try {
            if (chargeitemId == null) {
                logger.error("chargeitemId is missing");
                asyncResultHandler.handle(Future.succeededFuture(ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainBadRequest("chargeitemId is missing")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(CHARGEITEM_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(chargeitemId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(CHARGEITEM_VIEW,
                            Chargeitem.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.debug(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else {
                                    if (!getReply.succeeded()) {
                                        logger.error(getReply.result());
                                    } else {
                                        try {
                                            PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                    CHARGEITEM_VIEW, chargeitem, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
                                                        } else {
                                                            if (putReply.result().getUpdated() == 1) {
                                                                logger.error(putReply.result().getUpdated());
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        ChargeitemResource.PutChargeitemByChargeitemIdResponse.withNoContent()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        ChargeitemResource.PutChargeitemByChargeitemIdResponse.withPlainNotFound("Record Not Found")));
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
