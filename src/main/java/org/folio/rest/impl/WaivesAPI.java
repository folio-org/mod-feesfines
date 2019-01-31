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
import org.folio.rest.jaxrs.model.Waiver;
import org.folio.rest.jaxrs.model.WaivedataCollection;
import org.folio.rest.jaxrs.resource.Waives;
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
import org.folio.rest.jaxrs.model.WaivesGetOrder;

public class WaivesAPI implements Waives {

    private static final String WAIVES_TABLE = "waives";
    private static final String WAIVE_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Messages messages = Messages.getInstance();
    private final Logger logger = LoggerFactory.getLogger(WaivesAPI.class);

    public WaivesAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(WAIVES_TABLE + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Validate
    @Override
    public void getWaives(String query, String orderBy, WaivesGetOrder order, int offset, int limit, List<String> facets, String lang,
            Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext)  {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        List<FacetField> facetList = FacetManager.convertFacetStrings2FacetFields(facets, "jsonb");
        try {
        CQLWrapper cql = getCQL(query, limit, offset);
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
                    String[] fieldList = {"*"};

                    postgresClient.get(WAIVES_TABLE, Waiver.class, fieldList, cql,
                            true, false, facetList, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        WaivedataCollection waiveCollection = new WaivedataCollection();
                                        List<Waiver> waives = (List<Waiver>) reply.result().getResults();
                                        waiveCollection.setWaiver(waives);
                                        waiveCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                        waiveCollection.setResultInfo(reply.result().getResultInfo());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetWaivesResponse.respond200WithApplicationJson(waiveCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetWaivesResponse.respond500WithTextPlain(
                                                        reply.cause().getMessage())));
                                    }
                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetWaivesResponse.respond500WithTextPlain(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetWaivesResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetWaivesResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetWaivesResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetWaivesResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void postWaives(String lang, Waiver entity, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {
                        postgresClient.save(beginTx, WAIVES_TABLE, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final Waiver waive = entity;
                                    waive.setId(entity.getId());
                                    postgresClient.endTx(beginTx, done -> {
                                        asyncResultHandler.handle(Future.succeededFuture(PostWaivesResponse.respond201WithApplicationJson(waive,
                                                PostWaivesResponse.headersFor201().withLocation(reply.result()))));
                                    });
                                } else {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PostWaivesResponse.respond400WithTextPlain(
                                                    messages.getMessage(
                                                            lang, MessageConsts.UnableToProcessRequest))));
                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostWaivesResponse.respond500WithTextPlain(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        asyncResultHandler.handle(Future.succeededFuture(
                                PostWaivesResponse.respond500WithTextPlain(
                                        e.getMessage())));
                    }
                });
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostWaivesResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void getWaivesByWaiveId(String waiveId, String lang, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(WAIVE_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setValue(waiveId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(WAIVES_TABLE, Waiver.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetWaivesByWaiveIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Waiver> waiveList = (List<Waiver>) getReply.result().getResults();
                                    if (waiveList.size() < 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetWaivesByWaiveIdResponse.respond404WithTextPlain("Waive"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (waiveList.size() > 1) {
                                        logger.error("Multiple waives found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetWaivesByWaiveIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetWaivesByWaiveIdResponse.respond200WithApplicationJson(waiveList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetWaivesResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetWaivesResponse.respond500WithTextPlain(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void deleteWaivesByWaiveId(String waiveId, String lang, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                Criteria idCrit = new Criteria();
                idCrit.addField(WAIVE_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(waiveId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            WAIVES_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteWaivesByWaiveIdResponse.respond204()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteWaivesByWaiveIdResponse.respond404WithTextPlain("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteWaivesByWaiveIdResponse.respond500WithTextPlain(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteWaivesByWaiveIdResponse.respond400WithTextPlain(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteWaivesByWaiveIdResponse.respond500WithTextPlain(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteWaivesByWaiveIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void putWaivesByWaiveId(String waiveId, String lang, Waiver entity,
            Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        try {
            if (waiveId == null) {
                logger.error("waiveId is missing");
                asyncResultHandler.handle(Future.succeededFuture(PutWaivesByWaiveIdResponse.respond400WithTextPlain("waiveId is missing")));
            }

            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(WAIVE_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(waiveId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(WAIVES_TABLE,
                            Waiver.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutWaivesByWaiveIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else {
                                    if (!getReply.succeeded()) {
                                        logger.error(getReply.result());
                                    } else {
                                        try {
                                            PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                    WAIVES_TABLE, entity, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    PutWaivesByWaiveIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                                                        } else {
                                                            if (putReply.result().getUpdated() == 1) {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        PutWaivesByWaiveIdResponse.respond204()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        PutWaivesByWaiveIdResponse.respond404WithTextPlain("Record Not Found")));
                                                            }
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            asyncResultHandler.handle(Future.succeededFuture(
                                                    PutWaivesByWaiveIdResponse.respond500WithTextPlain(messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                        }
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutWaivesByWaiveIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutWaivesByWaiveIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }
}
