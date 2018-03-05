package org.folio.rest.impl;

import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
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
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FeeFineHistoryAPI implements FeefinehistoryResource {

    public static final String FEEFINEHISTORY_VIEW = "feefine_history_view";

    private final Messages messages = Messages.getInstance();
    private static final String FEEFINE_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(FeeFineHistoryAPI.class);
    String lang = "eng";

    public FeeFineHistoryAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(FEEFINEHISTORY_VIEW + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Override
    public void getFeefinehistory(String query, String orderBy, FeefinehistoryResource.Order order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        CQLWrapper cql = getCQL(query, limit, offset);
        try {
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));

                    String[] fieldList = {"*"};

                    postgresClient.get(FEEFINEHISTORY_VIEW, Feefinehistory.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        FeefinehistorydataCollection feefinehistoryCollection = new FeefinehistorydataCollection();
                                        List<Feefinehistory> feefinehistories = (List<Feefinehistory>) reply.result()[0];
                                        feefinehistoryCollection.setFeefinehistory(feefinehistories);
                                        feefinehistoryCollection.setTotalRecords((Integer) reply.result()[1]);
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                FeefinehistoryResource.GetFeefinehistoryResponse.withJsonOK(feefinehistoryCollection)));
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
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.GetFeefinehistoryResponse.withPlainBadRequest(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
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
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.GetFeefinehistoryResponse.withPlainBadRequest(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
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
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {
                        postgresClient.save(beginTx, FEEFINEHISTORY_VIEW, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final Feefinehistory feefinehistory = entity;
                                    feefinehistory.setId(entity.getId());
                                    OutStream stream = new OutStream();
                                    stream.setData(feefinehistory);
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
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(FEEFINE_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setValue(feefineId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(FEEFINEHISTORY_VIEW, Feefinehistory.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Feefinehistory> feefinehistoryList = (List<Feefinehistory>) getReply.result()[0];
                                    if (feefinehistoryList.size() < 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse.withPlainNotFound("Feefinehistory"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (feefinehistoryList.size() > 1) {
                                        logger.error("Multiple feefinehistory found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse.withJsonOK(feefinehistoryList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            FeefinehistoryResource.GetFeefinehistoryResponse.withPlainInternalServerError(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
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
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                
                Criteria idCrit = new Criteria();
                idCrit.addField(FEEFINE_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(feefineId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            FEEFINEHISTORY_VIEW, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse.withNoContent()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse.withPlainNotFound("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
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
                    logger.error(e.getMessage());
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

    @Override
    public void putFeefinehistoryByFeefinehistoryId(String feefinehistoryId,
            String lang, Feefinehistory feefinehistory,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) throws Exception {

        try {
            if (feefinehistoryId == null) {
                logger.error("feefinehistoryId is missing");
                asyncResultHandler.handle(Future.succeededFuture(FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainBadRequest("feefinehistoryId is missing")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(FEEFINE_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(feefinehistoryId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(FEEFINEHISTORY_VIEW,
                            Feefinehistory.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else {
                                    if (!getReply.succeeded()) {
                                        logger.error(getReply.result());
                                    } else {
                                        try {
                                            PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                    FEEFINEHISTORY_VIEW, feefinehistory, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
                                                        } else {
                                                            if (putReply.result().getUpdated() == 1) {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withNoContent()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse.withPlainNotFound("Record Not Found")));
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