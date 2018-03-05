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

public class FeeFineActionsAPI implements FeefineactionsResource {

    public static final String FEEFINEACTIONS_TABLE  = "feefineactions";

    private final Messages messages = Messages.getInstance();
    private static final String FEEFINEACTION_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(FeeFineActionsAPI.class);

    public FeeFineActionsAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }
    
    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(FEEFINEACTIONS_TABLE + ".jsonb"
        );
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Override
    public void getFeefineactions(String query, String orderBy, Order order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        CQLWrapper cql = getCQL(query, limit, offset);

        try {
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
                    String[] fieldList = {"*"};

                    postgresClient.get(FEEFINEACTIONS_TABLE, Feefineaction.class, fieldList, cql,
                            true, false, reply -> {
                        try {
                            if (reply.succeeded()) {
                                FeefineactiondataCollection feefineactionCollection = new FeefineactiondataCollection();
                                List<Feefineaction> feefineactions = (List<Feefineaction>) reply.result()[0];
                                feefineactionCollection.setFeefineactions(feefineactions);
                                feefineactionCollection.setTotalRecords((Integer) reply.result()[1]);
                                asyncResultHandler.handle(Future.succeededFuture(
                                        GetFeefineactionsResponse.withJsonOK(feefineactionCollection)));
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
                    }
                 );
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetFeefineactionsResponse.withPlainBadRequest(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
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
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetFeefineactionsResponse.withPlainBadRequest(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
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
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {
                        postgresClient.save(beginTx, FEEFINEACTIONS_TABLE, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
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

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(FEEFINEACTION_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setValue(feefineactionId);
                    Criterion criterion = new Criterion(idCrit);
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(FEEFINEACTIONS_TABLE, Feefineaction.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Feefineaction> feefineactionList = (List<Feefineaction>) getReply.result()[0];
                                    if (feefineactionList.size() < 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetFeefineactionsByFeefineactionIdResponse.withPlainNotFound("Feefineaction"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (feefineactionList.size() > 1) {
                                        logger.error("Multiple feefineactions found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetFeefineactionsByFeefineactionIdResponse.withJsonOK(feefineactionList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetFeefineactionsResponse.withPlainInternalServerError(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetFeefineactionsResponse.withPlainInternalServerError(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void deleteFeefineactionsByFeefineactionId(String feefineactionId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                
                Criteria idCrit = new Criteria();
                idCrit.setOperation("=");
                idCrit.setValue(feefineactionId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            FEEFINEACTIONS_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteFeefineactionsByFeefineactionIdResponse.withNoContent()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteFeefineactionsByFeefineactionIdResponse.withPlainNotFound("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
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
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            logger.error(e.getMessage());
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
            if (feefineactionId == null) {
                logger.error("feefineactionId is missing");
                asyncResultHandler.handle(Future.succeededFuture(PutFeefineactionsByFeefineactionIdResponse.withPlainBadRequest("feefineactionId is missing")));
            }

            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(FEEFINEACTION_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(feefineactionId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(FEEFINEACTIONS_TABLE,
                            Feefineaction.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else {
                                    if (!getReply.succeeded()) {
                                        logger.error(getReply.result());
                                    } else {
                                        try {
                                            PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                    FEEFINEACTIONS_TABLE, feefineaction, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    PutFeefineactionsByFeefineactionIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
                                                        } else {
                                                            if (putReply.result().getUpdated() == 1) {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        DeleteFeefineactionsByFeefineactionIdResponse.withNoContent()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        DeleteFeefineactionsByFeefineactionIdResponse.withPlainNotFound("Record Not Found")));
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