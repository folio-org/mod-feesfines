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
import org.folio.rest.jaxrs.model.Payment;
import org.folio.rest.jaxrs.model.PaymentdataCollection;
import org.folio.rest.jaxrs.model.PaymentsGetOrder;
import org.folio.rest.jaxrs.resource.Payments;
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

public class PaymentsAPI implements Payments {

    private static final String PAYMENTS_TABLE = "payments";
    private static final String PAYMENT_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Messages messages = Messages.getInstance();
    private final Logger logger = LoggerFactory.getLogger(PaymentsAPI.class);

    private CQLWrapper getCQL(String query, int limit, int offset) throws CQL2PgJSONException, IOException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(PAYMENTS_TABLE + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Validate
    @Override
    public void getPayments(String query, String orderBy, PaymentsGetOrder order, int offset, int limit, List<String> facets, String lang,
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

                    postgresClient.get(PAYMENTS_TABLE, Payment.class, fieldList, cql,
                            true, false, facetList, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        PaymentdataCollection paymentCollection = new PaymentdataCollection();
                                        List<Payment> payments = reply.result().getResults();
                                        paymentCollection.setPayments(payments);
                                        paymentCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                        paymentCollection.setResultInfo(reply.result().getResultInfo());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetPaymentsResponse.respond200WithApplicationJson(paymentCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetPaymentsResponse.respond500WithTextPlain(
                                                        reply.cause().getMessage())));
                                    }
                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetPaymentsResponse.respond500WithTextPlain(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetPaymentsResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetPaymentsResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (IOException | CQL2PgJSONException e) {
            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetPaymentsResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetPaymentsResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void postPayments(String lang, Payment entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                postgresClient.startTx(beginTx -> {
                    try {
                        postgresClient.save(beginTx, PAYMENTS_TABLE, entity.getId(), entity, reply -> {
                            try {
                                if (reply.succeeded()) {
                                    final Payment payment = entity;
                                    payment.setId(entity.getId());
                                    postgresClient.endTx(beginTx, done
                                            -> asyncResultHandler.handle(Future.succeededFuture(PostPaymentsResponse.respond201WithApplicationJson(payment,
                                                    PostPaymentsResponse.headersFor201().withLocation(reply.result())))));

                                } else {
                                    postgresClient.rollbackTx(beginTx, rollback -> {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PostPaymentsResponse.respond400WithTextPlain(messages.getMessage(lang, MessageConsts.UnableToProcessRequest))));
                                    });
                                }
                            } catch (Exception e) {
                                asyncResultHandler.handle(Future.succeededFuture(
                                        PostPaymentsResponse.respond500WithTextPlain(
                                                e.getMessage())));
                            }
                        });
                    } catch (Exception e) {
                        postgresClient.rollbackTx(beginTx, rollback -> {
                            asyncResultHandler.handle(Future.succeededFuture(
                                    PostPaymentsResponse.respond500WithTextPlain(
                                            e.getMessage())));
                        });
                    }
                });
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostPaymentsResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void getPaymentsByPaymentId(String paymentId, String lang, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(PAYMENT_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setVal(paymentId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(PAYMENTS_TABLE, Payment.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.result());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetPaymentsByPaymentIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Payment> paymentList = getReply.result().getResults();
                                    if (paymentList.isEmpty()) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetPaymentsByPaymentIdResponse.respond404WithTextPlain("Payment"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (paymentList.size() > 1) {
                                        logger.error("Multiple payments found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetPaymentsByPaymentIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetPaymentsByPaymentIdResponse.respond200WithApplicationJson(paymentList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetPaymentsResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetPaymentsResponse.respond500WithTextPlain(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void deletePaymentsByPaymentId(String paymentId, String lang, Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                Criteria idCrit = new Criteria();
                idCrit.addField(PAYMENT_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(paymentId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                            PAYMENTS_TABLE, criterion, deleteReply -> {
                                if (deleteReply.succeeded()) {
                                    if (deleteReply.result().getUpdated() == 1) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeletePaymentsByPaymentIdResponse.respond204()));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                DeletePaymentsByPaymentIdResponse.respond404WithTextPlain("Record Not Found")));
                                    }
                                } else {
                                    logger.error(deleteReply.result());
                                    String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                    logger.error(error, deleteReply.cause());
                                    if (error == null) {
                                        asyncResultHandler.handle(Future.succeededFuture(DeletePaymentsByPaymentIdResponse.respond500WithTextPlain(
                                                messages.getMessage(lang, MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(DeletePaymentsByPaymentIdResponse.respond400WithTextPlain(error)));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(
                            Future.succeededFuture(
                                    DeletePaymentsByPaymentIdResponse.respond500WithTextPlain(
                                            messages.getMessage(lang,
                                                    MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage());
            asyncResultHandler.handle(
                    Future.succeededFuture(
                            DeletePaymentsByPaymentIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang,
                                            MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void putPaymentsByPaymentId(String paymentId, String lang, Payment entity,
            Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            if (paymentId == null) {
                logger.error("paymentId is missing");
                asyncResultHandler.handle(Future.succeededFuture(PutPaymentsByPaymentIdResponse.respond400WithTextPlain("paymentId is missing")));
            }

            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                Criteria idCrit = new Criteria();
                idCrit.addField(PAYMENT_ID_FIELD);
                idCrit.setOperation("=");
                idCrit.setVal(paymentId);
                Criterion criterion = new Criterion(idCrit);

                try {
                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(PAYMENTS_TABLE,
                            Payment.class, criterion, true, false, getReply -> {
                                if (getReply.failed()) {
                                    logger.error(getReply.cause().getLocalizedMessage());
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutPaymentsByPaymentIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang,
                                                            MessageConsts.InternalServerError))));
                                } else if (getReply.result().getResults().size() == 1) {
                                    try {
                                        PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                                PAYMENTS_TABLE, entity, criterion, true, putReply -> {
                                                    if (putReply.failed()) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutPaymentsByPaymentIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                                                    } else if (putReply.result().getUpdated() == 1) {
                                                        asyncResultHandler.handle(Future.succeededFuture(
                                                                PutPaymentsByPaymentIdResponse.respond204()));
                                                    }
                                                });
                                    } catch (Exception e) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                PutPaymentsByPaymentIdResponse.respond500WithTextPlain(messages.getMessage(lang,
                                                        MessageConsts.InternalServerError))));
                                    }
                                } else if (getReply.result().getResults().isEmpty()) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutPaymentsByPaymentIdResponse.respond404WithTextPlain("Record Not Found")));
                                } else if (getReply.result().getResults().size() > 1) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            PutPaymentsByPaymentIdResponse.respond404WithTextPlain("Multiple account records")));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    asyncResultHandler.handle(Future.succeededFuture(
                            PutPaymentsByPaymentIdResponse.respond500WithTextPlain(
                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                }
            });
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            asyncResultHandler.handle(Future.succeededFuture(
                    PutPaymentsByPaymentIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
        }
    }
}
