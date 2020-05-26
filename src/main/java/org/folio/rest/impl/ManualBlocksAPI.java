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
import org.folio.rest.jaxrs.model.Manualblock;
import org.folio.rest.jaxrs.model.ManualblockdataCollection;
import org.folio.rest.jaxrs.model.ManualblocksGetOrder;
import org.folio.rest.jaxrs.resource.Manualblocks;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

public class ManualBlocksAPI implements Manualblocks {

    public static final String MANUALBLOCKS_TABLE = "manualblocks";

    private final Messages messages = Messages.getInstance();
    private static final String MANUALBLOCK_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(ManualBlocksAPI.class);

    private CQLWrapper getCQL(String query, int limit, int offset) throws CQL2PgJSONException, IOException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(MANUALBLOCKS_TABLE + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Validate
    @Override
    public void getManualblocks(String query, String orderBy, ManualblocksGetOrder order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        try {
            CQLWrapper cql = getCQL(query, limit, offset);
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
                    String[] fieldList = {"*"};

                    postgresClient.get(MANUALBLOCKS_TABLE, Manualblock.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        ManualblockdataCollection manualblocksCollection = new ManualblockdataCollection();
                                        List<Manualblock> manualblockList = reply.result().getResults();
                                        manualblocksCollection.setManualblocks(manualblockList);
                                        manualblocksCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetManualblocksResponse.respond200WithApplicationJson(manualblocksCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetManualblocksResponse.respond500WithTextPlain(
                                                        reply.cause().getMessage())));
                                    }

                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetManualblocksResponse.respond500WithTextPlain(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetManualblocksResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetManualblocksResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (IOException | CQL2PgJSONException e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetManualblocksResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetManualblocksResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void postManualblocks(String lang, Manualblock entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {

                        postgresClient.save(beginTx, MANUALBLOCKS_TABLE, entity.getId(), entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final Manualblock manualblock = entity;
                                    manualblock.setId(entity.getId());
                                    logger.debug("ID API" + entity.getId());
                                    postgresClient.endTx(beginTx, done
                                            -> asyncResultHandler.handle(Future.succeededFuture(PostManualblocksResponse.respond201WithApplicationJson(manualblock,
                                                    PostManualblocksResponse.headersFor201().withLocation(reply.result())))));

                                } else {
                                    postgresClient.rollbackTx(beginTx, rollback -> {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PostManualblocksResponse.respond400WithTextPlain(messages.getMessage(lang, MessageConsts.UnableToProcessRequest))));
                                    });
                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostManualblocksResponse.respond500WithTextPlain(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        postgresClient.rollbackTx(beginTx, rollback -> {
                            asyncResultHandler.handle(Future.succeededFuture(
                                    PostManualblocksResponse.respond500WithTextPlain(
                                            e.getMessage())));
                        });
                    }
                });

            });

        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostManualblocksResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    }

    @Validate
    @Override
    public void getManualblocksByManualblockId(String manualblockId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(MANUALBLOCK_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setVal(manualblockId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(MANUALBLOCKS_TABLE, Manualblock.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetManualblocksByManualblockIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Manualblock> manualblockList = getReply.result().getResults();
                                    if (manualblockList.isEmpty()) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetManualblocksByManualblockIdResponse.respond404WithTextPlain("Manualblock"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (manualblockList.size() > 1) {
                                        logger.error("Multiple manualblocks found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetManualblocksByManualblockIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetManualblocksByManualblockIdResponse.respond200WithApplicationJson(manualblockList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetManualblocksResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetManualblocksResponse.respond500WithTextPlain(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }

    }

    @Validate
    @Override
    public void deleteManualblocksByManualblockId(String manualblockId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(MANUALBLOCK_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(manualblockId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            MANUALBLOCKS_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().rowCount() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteManualblocksByManualblockIdResponse.respond204()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteManualblocksByManualblockIdResponse.respond404WithTextPlain("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteManualblocksByManualblockIdResponse.respond500WithTextPlain(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))
                                        ));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteManualblocksByManualblockIdResponse.respond400WithTextPlain(error)
                                        )
                                        );
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteManualblocksByManualblockIdResponse.respond500WithTextPlain(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteManualblocksByManualblockIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void putManualblocksByManualblockId(String manualblockId, String lang, Manualblock entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            if (manualblockId == null) {
                logger.error("manualblockId is missing ");
                asyncResultHandler.handle(Future.succeededFuture(PutManualblocksByManualblockIdResponse.respond400WithTextPlain("manualblockId is missing")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(MANUALBLOCK_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(manualblockId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(MANUALBLOCKS_TABLE,
                            Manualblock.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutManualblocksByManualblockIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else if (getReply.result().getResults().size() == 1) {
                                    try {
                                        PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                MANUALBLOCKS_TABLE, entity, criterion, true, putReply -> {
                                                    if (putReply.failed()) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutManualblocksByManualblockIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                                                    } else if (putReply.result().rowCount() == 1) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutManualblocksByManualblockIdResponse.respond204()));
                                                    }
                                                });
                                    } catch (Exception e) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PutManualblocksByManualblockIdResponse.respond500WithTextPlain(messages.getMessage(lang,
                                                        MessageConsts.InternalServerError))));
                                    }
                                } else if (getReply.result().getResults().isEmpty()) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutManualblocksByManualblockIdResponse.respond404WithTextPlain("Record Not Found")));
                                } else if (getReply.result().getResults().size() > 1) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutManualblocksByManualblockIdResponse.respond404WithTextPlain("Multiple account records")));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutManualblocksByManualblockIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutManualblocksByManualblockIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    }
}
