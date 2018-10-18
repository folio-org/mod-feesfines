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
import org.folio.rest.jaxrs.model.Transfertype;
import org.folio.rest.jaxrs.model.TransferTypedataCollection;
import org.folio.rest.jaxrs.resource.Transfertypes;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;
import org.folio.rest.jaxrs.model.TransfertypesGetOrder;

public class TransferTypeAPI implements Transfertypes {

    public static final String TRANSFERTYPES_TABLE  = "transfer_type";

    private final Messages messages = Messages.getInstance();
    private static final String TRANSFERTYPE_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(TransferTypeAPI.class);

    public TransferTypeAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(TRANSFERTYPES_TABLE + ".jsonb"
        );
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Override
    public void getTransfertypes(String query, String orderBy, TransfertypesGetOrder order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        try {
        CQLWrapper cql = getCQL(query, limit, offset);
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
                    String[] fieldList = {"*"};

                    postgresClient.get(TRANSFERTYPES_TABLE, Transfertype.class, fieldList, cql,
                            true, false, reply -> {
                        try {
                            if (reply.succeeded()) {
                                TransferTypedataCollection transfertypeCollection = new TransferTypedataCollection();
                                List<Transfertype> transfertypes = (List<Transfertype>) reply.result().getResults();
                                transfertypeCollection.setTransfertypes(transfertypes);
                                transfertypeCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                asyncResultHandler.handle(Future.succeededFuture(
                                        GetTransfertypesResponse.respond200WithApplicationJson(transfertypeCollection)));
                            } else {
                                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                        GetTransfertypesResponse.respond500WithTextPlain(
                                                reply.cause().getMessage())));
                            }
                        } catch (Exception e) {
                            logger.debug(e.getLocalizedMessage());
                            asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                    GetTransfertypesResponse.respond500WithTextPlain(
                                            reply.cause().getMessage())));
                        }
                    }
                 );
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetTransfertypesResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetTransfertypesResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetTransfertypesResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetTransfertypesResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Override
    public void postTransfertypes(String lang, Transfertype entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {
                        postgresClient.save(beginTx, TRANSFERTYPES_TABLE, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final Transfertype transfertype = entity;
                                    transfertype.setId(entity.getId());
                                    postgresClient.endTx(beginTx, done -> {
                                        asyncResultHandler.handle(Future.succeededFuture(PostTransfertypesResponse.respond201WithApplicationJson(transfertype,
                                                PostTransfertypesResponse.headersFor201().withLocation(reply.result()))));
                                    });
                                } else {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PostTransfertypesResponse.respond400WithTextPlain(
                                                    messages.getMessage(
                                                            lang, MessageConsts.UnableToProcessRequest))));

                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostTransfertypesResponse.respond500WithTextPlain(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        asyncResultHandler.handle(Future.succeededFuture(
                                PostTransfertypesResponse.respond500WithTextPlain(
                                        e.getMessage())));
                    }
                });

            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostTransfertypesResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void getTransfertypesByTransfertypeId(String transfertypeId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(TRANSFERTYPE_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setValue(transfertypeId);
                    Criterion criterion = new Criterion(idCrit);
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(TRANSFERTYPES_TABLE, Transfertype.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Transfertype> transfertypeList = (List<Transfertype>) getReply.result().getResults();
                                    if (transfertypeList.size() < 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetTransfertypesByTransfertypeIdResponse.respond404WithTextPlain("Transfertype"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (transfertypeList.size() > 1) {
                                        logger.error("Multiple transfertypes found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetTransfertypesByTransfertypeIdResponse.respond200WithApplicationJson(transfertypeList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetTransfertypesResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetTransfertypesResponse.respond500WithTextPlain(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void deleteTransfertypesByTransfertypeId(String transfertypeId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.setOperation("=");
                idCrit.setValue(transfertypeId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            TRANSFERTYPES_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteTransfertypesByTransfertypeIdResponse.respond204()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteTransfertypesByTransfertypeIdResponse.respond404WithTextPlain("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteTransfertypesByTransfertypeIdResponse.respond400WithTextPlain(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void putTransfertypesByTransfertypeId(String transfertypeId, String lang, Transfertype entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext)  {
        try {
            if (transfertypeId == null) {
                logger.error("transfertypeId is missing");
                asyncResultHandler.handle(Future.succeededFuture(PutTransfertypesByTransfertypeIdResponse.respond400WithTextPlain("transfertypeId is missing")));
            }

            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(TRANSFERTYPE_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(transfertypeId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(TRANSFERTYPES_TABLE,
                            Transfertype.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else {
                                    if (!getReply.succeeded()) {
                                        logger.error(getReply.result());
                                    } else {
                                        try {
                                            PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                    TRANSFERTYPES_TABLE, entity, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    PutTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                                                        } else {
                                                            if (putReply.result().getUpdated() == 1) {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        DeleteTransfertypesByTransfertypeIdResponse.respond204()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        DeleteTransfertypesByTransfertypeIdResponse.respond404WithTextPlain("Record Not Found")));
                                                            }
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            asyncResultHandler.handle(Future.succeededFuture(
                                                    PutTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                        }
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutTransfertypesByTransfertypeIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    }
}
