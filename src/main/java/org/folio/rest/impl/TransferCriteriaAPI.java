package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.CQL2PgJSONException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.TransferCriteria;
import org.folio.rest.jaxrs.model.TransferCriteriaCollection;
import org.folio.rest.jaxrs.model.TransferCriteriasGetOrder;
import org.folio.rest.jaxrs.resource.TransferCriterias;
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

/**
 *
 * @author Lluvia
 */
public class TransferCriteriaAPI implements TransferCriterias {

    public static final String TRANSFER_CRITERIA_TABLE = "transfer_criteria";

    private final Messages messages = Messages.getInstance();
    private static final String TRANSFER_CRITERIA_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(TransferCriteriaAPI.class);

    private CQLWrapper getCQL(String query, int limit, int offset) throws CQL2PgJSONException, IOException  {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(TRANSFER_CRITERIA_TABLE + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Validate
    @Override
    public void getTransferCriterias(String query, String orderBy, TransferCriteriasGetOrder order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        try {
            CQLWrapper cql = getCQL(query, limit, offset);
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
                    String[] fieldList = {"*"};

                    postgresClient.get(TRANSFER_CRITERIA_TABLE, TransferCriteria.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        TransferCriteriaCollection transferCriteriasCollection = new TransferCriteriaCollection();
                                        List<TransferCriteria> transferCriteriaList = reply.result().getResults();
                                        transferCriteriasCollection.setTransferCriterias(transferCriteriaList);
                                        transferCriteriasCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetTransferCriteriasResponse.respond200WithApplicationJson(transferCriteriasCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetTransferCriteriasResponse.respond500WithTextPlain(
                                                        reply.cause().getMessage())));
                                    }

                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetTransferCriteriasResponse.respond500WithTextPlain(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetTransferCriteriasResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetTransferCriteriasResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetTransferCriteriasResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetTransferCriteriasResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void postTransferCriterias(String lang, TransferCriteria entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {

                        postgresClient.save(beginTx, TRANSFER_CRITERIA_TABLE, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final TransferCriteria transferCriteria = entity;
                                    transferCriteria.setId(entity.getId());
                                    logger.debug("ID API" + entity.getId());
                                    postgresClient.endTx(beginTx, done
                                            -> asyncResultHandler.handle(Future.succeededFuture(PostTransferCriteriasResponse.respond201WithApplicationJson(transferCriteria,
                                                    PostTransferCriteriasResponse.headersFor201().withLocation(reply.result())))));

                                } else {
                                    postgresClient.rollbackTx(beginTx, rollback -> {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PostTransferCriteriasResponse.respond400WithTextPlain(messages.getMessage(lang, MessageConsts.UnableToProcessRequest))));
                                    });
                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostTransferCriteriasResponse.respond500WithTextPlain(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        postgresClient.rollbackTx(beginTx, rollback -> {
                            asyncResultHandler.handle(Future.succeededFuture(
                                    PostTransferCriteriasResponse.respond500WithTextPlain(
                                            e.getMessage())));
                        });
                    }
                });

            });

        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostTransferCriteriasResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void getTransferCriteriasByTransferCriteriaId(String transferCriteriaId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(TRANSFER_CRITERIA_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setVal(transferCriteriaId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(TRANSFER_CRITERIA_TABLE, TransferCriteria.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<TransferCriteria> transferCriteriaList = getReply.result().getResults();
                                    if (transferCriteriaList.isEmpty()) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetTransferCriteriasByTransferCriteriaIdResponse.respond404WithTextPlain("TransferCriteria"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (transferCriteriaList.size() > 1) {
                                        logger.error("Multiple transferCriterias found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetTransferCriteriasByTransferCriteriaIdResponse.respond200WithApplicationJson(transferCriteriaList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetTransferCriteriasResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetTransferCriteriasResponse.respond500WithTextPlain(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void deleteTransferCriteriasByTransferCriteriaId(String transferCriteriaId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(TRANSFER_CRITERIA_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(transferCriteriaId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            TRANSFER_CRITERIA_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteTransferCriteriasByTransferCriteriaIdResponse.respond204()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteTransferCriteriasByTransferCriteriaIdResponse.respond404WithTextPlain("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))
                                        ));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteTransferCriteriasByTransferCriteriaIdResponse.respond400WithTextPlain(error)
                                        )
                                        );
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void putTransferCriteriasByTransferCriteriaId(String transferCriteriaId, String lang, TransferCriteria entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            if (transferCriteriaId == null) {
                logger.error("transferCriteriaId is missing ");
                asyncResultHandler.handle(Future.succeededFuture(PutTransferCriteriasByTransferCriteriaIdResponse.respond400WithTextPlain("transferCriteriaId is missing")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(TRANSFER_CRITERIA_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(transferCriteriaId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(TRANSFER_CRITERIA_TABLE,
                            TransferCriteria.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else if (getReply.result().getResults().size() == 1) {
                                    try {
                                        PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                TRANSFER_CRITERIA_TABLE, entity, criterion, true, putReply -> {
                                                    if (putReply.failed()) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                                                    } else if (putReply.result().getUpdated() == 1) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutTransferCriteriasByTransferCriteriaIdResponse.respond204()));
                                                    }
                                                });
                                    } catch (Exception e) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PutTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(messages.getMessage(lang,
                                                        MessageConsts.InternalServerError))));
                                    }
                                } else if (getReply.result().getResults().isEmpty()) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutTransferCriteriasByTransferCriteriaIdResponse.respond404WithTextPlain("Record Not Found")));
                                } else if (getReply.result().getResults().size() > 1) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutTransferCriteriasByTransferCriteriaIdResponse.respond404WithTextPlain("Multiple account records")));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutTransferCriteriasByTransferCriteriaIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }

}
