package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.CQL2PgJSONException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Refund;
import org.folio.rest.jaxrs.model.RefunddataCollection;
import org.folio.rest.jaxrs.model.RefundsGetOrder;
import org.folio.rest.jaxrs.resource.Refunds;
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

public class RefundsAPI implements Refunds {

    private static final String REFUNDS_TABLE = "refunds";
    private static final String REFUND_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Messages messages = Messages.getInstance();
    private final Logger logger = LoggerFactory.getLogger(RefundsAPI.class);

    private CQLWrapper getCQL(String query, int limit, int offset) throws CQL2PgJSONException, IOException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(REFUNDS_TABLE + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Validate
    @Override
    public void getRefunds(String query, String orderBy, RefundsGetOrder order, int offset, int limit, List<String> facets, String lang,
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

                    postgresClient.get(REFUNDS_TABLE, Refund.class, fieldList, cql,
                            true, false, facetList, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        RefunddataCollection refundCollection = new RefunddataCollection();
                                        List<Refund> refunds = reply.result().getResults();
                                        refundCollection.setRefunds(refunds);
                                        refundCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                        refundCollection.setResultInfo(reply.result().getResultInfo());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetRefundsResponse.respond200WithApplicationJson(refundCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetRefundsResponse.respond500WithTextPlain(
                                                        reply.cause().getMessage())));
                                    }
                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetRefundsResponse.respond500WithTextPlain(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetRefundsResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetRefundsResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (IOException | CQL2PgJSONException e) {
            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetRefundsResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetRefundsResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void postRefunds(String lang, Refund entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {
                        postgresClient.save(beginTx, REFUNDS_TABLE, entity.getId(), entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final Refund refund = entity;
                                    refund.setId(entity.getId());
                                    postgresClient.endTx(beginTx, done
                                            -> asyncResultHandler.handle(Future.succeededFuture(PostRefundsResponse.respond201WithApplicationJson(refund,
                                                    PostRefundsResponse.headersFor201().withLocation(reply.result())))));

                                } else {
                                    postgresClient.rollbackTx(beginTx, rollback -> {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PostRefundsResponse.respond400WithTextPlain(messages.getMessage(lang, MessageConsts.UnableToProcessRequest))));
                                    });
                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostRefundsResponse.respond500WithTextPlain(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        postgresClient.rollbackTx(beginTx, rollback -> {
                            asyncResultHandler.handle(Future.succeededFuture(
                                    PostRefundsResponse.respond500WithTextPlain(
                                            e.getMessage())));
                        });
                    }
                });
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostRefundsResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void getRefundsByRefundId(String refundId, String lang, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(REFUND_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setVal(refundId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(REFUNDS_TABLE, Refund.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetRefundsByRefundIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Refund> refundList = getReply.result().getResults();
                                    if (refundList.isEmpty()) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetRefundsByRefundIdResponse.respond404WithTextPlain("Refund"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (refundList.size() > 1) {
                                        logger.error("Multiple refunds found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetRefundsByRefundIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetRefundsByRefundIdResponse.respond200WithApplicationJson(refundList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetRefundsResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetRefundsResponse.respond500WithTextPlain(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void deleteRefundsByRefundId(String refundId, String lang, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                Criteria idCrit = new Criteria();
                idCrit.addField(REFUND_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(refundId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            REFUNDS_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().rowCount() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteRefundsByRefundIdResponse.respond204()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteRefundsByRefundIdResponse.respond404WithTextPlain("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteRefundsByRefundIdResponse.respond500WithTextPlain(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteRefundsByRefundIdResponse.respond400WithTextPlain(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteRefundsByRefundIdResponse.respond500WithTextPlain(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteRefundsByRefundIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void putRefundsByRefundId(String refundId, String lang, Refund entity,
            Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            if (refundId == null) {
                logger.error("refundId is missing");
                asyncResultHandler.handle(Future.succeededFuture(PutRefundsByRefundIdResponse.respond400WithTextPlain("refundId is missing")));
            }

            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(REFUND_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(refundId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(REFUNDS_TABLE,
                            Refund.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutRefundsByRefundIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else if (getReply.result().getResults().size() == 1) {
                                    try {
                                        PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                REFUNDS_TABLE, entity, criterion, true, putReply -> {
                                                    if (putReply.failed()) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutRefundsByRefundIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                                                    } else if (putReply.result().rowCount() == 1) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutRefundsByRefundIdResponse.respond204()));
                                                    }
                                                });
                                    } catch (Exception e) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PutRefundsByRefundIdResponse.respond500WithTextPlain(messages.getMessage(lang,
                                                        MessageConsts.InternalServerError))));
                                    }
                                } else if (getReply.result().getResults().isEmpty()) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutRefundsByRefundIdResponse.respond404WithTextPlain("Record Not Found")));
                                } else if (getReply.result().getResults().size() > 1) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutRefundsByRefundIdResponse.respond404WithTextPlain("Multiple account records")));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutRefundsByRefundIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutRefundsByRefundIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }
}
