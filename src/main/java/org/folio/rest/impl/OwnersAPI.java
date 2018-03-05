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
import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.jaxrs.model.OwnerdataCollection;
import org.folio.rest.jaxrs.resource.OwnersResource;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.OutStream;
import org.folio.rest.tools.utils.TenantTool;
import org.z3950.zing.cql.cql2pgjson.CQL2PgJSON;
import org.z3950.zing.cql.cql2pgjson.FieldException;

public class OwnersAPI implements OwnersResource {

    public static final String OWNERS_TABLE = "owners";

    private final Messages messages = Messages.getInstance();
    private static final String OWNER_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LoggerFactory.getLogger(OwnersAPI.class);

    public OwnersAPI(Vertx vertx, String tenantId) {
        PostgresClient.getInstance(vertx, tenantId).setIdField("id");
    }

    private CQLWrapper getCQL(String query, int limit, int offset) throws FieldException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(OWNERS_TABLE + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Override
    public void getOwners(String query, String orderBy, Order order, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        CQLWrapper cql = getCQL(query, limit, offset);
        try {
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
                    String[] fieldList = {"*"};

                    postgresClient.get(OWNERS_TABLE, Owner.class, fieldList, cql,
                            true, false, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        OwnerdataCollection OwnersCollection = new OwnerdataCollection();
                                        List<Owner> owner = (List<Owner>) reply.result()[0];
                                        OwnersCollection.setOwners(owner);
                                        OwnersCollection.setTotalRecords((Integer) reply.result()[1]);
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetOwnersResponse.withJsonOK(OwnersCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetOwnersResponse.withPlainInternalServerError(
                                                        reply.cause().getMessage())));
                                    }

                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetOwnersResponse.withPlainInternalServerError(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetOwnersResponse.withPlainBadRequest(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetOwnersResponse.withPlainInternalServerError(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (Exception e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetOwnersResponse.withPlainBadRequest(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetOwnersResponse.withPlainInternalServerError(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Override
    public void postOwners(String lang, Owner entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {

        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {

                        postgresClient.save(beginTx, OWNERS_TABLE, entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final Owner owner = entity;
                                    owner.setId(entity.getId());
                                    OutStream stream = new OutStream();
                                    stream.setData(owner);
                                    postgresClient.endTx(beginTx, done -> {
                                        asyncResultHandler.handle(Future.succeededFuture(PostOwnersResponse.withJsonCreated(
                                                reply.result(), stream)));
                                    });
                                } else {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PostOwnersResponse.withPlainBadRequest(
                                                    messages.getMessage(
                                                            lang, MessageConsts.UnableToProcessRequest))));

                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostOwnersResponse.withPlainInternalServerError(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        asyncResultHandler.handle(Future.succeededFuture(
                                PostOwnersResponse.withPlainInternalServerError(
                                        e.getMessage())));
                    }
                });

            });

        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostOwnersResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    }

    @Override
    public void getOwnersByOwnerId(String ownerId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(OWNER_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setValue(ownerId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(OWNERS_TABLE, Owner.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetOwnersByOwnerIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Owner> ownerList = (List<Owner>) getReply.result()[0];
                                    if (ownerList.size() < 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetOwnersByOwnerIdResponse.withPlainNotFound("Owner"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (ownerList.size() > 1) {
                                        logger.error("Multiple owners found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetOwnersByOwnerIdResponse.withPlainInternalServerError(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetOwnersByOwnerIdResponse.withJsonOK(ownerList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetOwnersResponse.withPlainInternalServerError(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetOwnersResponse.withPlainInternalServerError(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }

    }

    @Override
    public void deleteOwnersByOwnerId(String ownerId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(OWNER_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(ownerId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            OWNERS_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteOwnersByOwnerIdResponse.withNoContent()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeleteOwnersByOwnerIdResponse.withPlainNotFound("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteOwnersByOwnerIdResponse.withPlainInternalServerError(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))
                                        ));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeleteOwnersByOwnerIdResponse.withPlainBadRequest(error)
                                        )
                                        );
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeleteOwnersByOwnerIdResponse.withPlainInternalServerError(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeleteOwnersByOwnerIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Override
    public void putOwnersByOwnerId(String ownerId, String lang, Owner owner, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) throws Exception {
        try {
            if (ownerId == null) {
                logger.error("ownerId is missing ");
                asyncResultHandler.handle(Future.succeededFuture(PutOwnersByOwnerIdResponse.withPlainBadRequest("ownerId is missing")));
            }
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(OWNER_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setValue(ownerId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(OWNERS_TABLE,
                            Owner.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutOwnersByOwnerIdResponse.withPlainInternalServerError(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else {
                                    if (!getReply.succeeded()) {
                                        logger.error(getReply.result());
                                    } else {
                                        try {
                                            PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                    OWNERS_TABLE, owner, criterion, true, putReply -> {
                                                        if (putReply.failed()) {
                                                            asyncResultHandler.handle(Future.succeededFuture(
                                                                    PutOwnersByOwnerIdResponse.withPlainInternalServerError(putReply.cause().getMessage())));
                                                        } else {
                                                            if (putReply.result().getUpdated() == 1) {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        PutOwnersByOwnerIdResponse.withNoContent()));
                                                            } else {
                                                                asyncResultHandler.handle(Future.succeededFuture(
                                                                        PutOwnersByOwnerIdResponse.withPlainNotFound("Record Not Found")));
                                                            }
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            asyncResultHandler.handle(Future.succeededFuture(
                                                    PutOwnersByOwnerIdResponse.withPlainInternalServerError(messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                        }
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutOwnersByOwnerIdResponse.withPlainInternalServerError(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutOwnersByOwnerIdResponse.withPlainInternalServerError(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }

    }
}
