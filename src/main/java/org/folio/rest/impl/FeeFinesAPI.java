package org.folio.rest.impl;

import static org.folio.rest.jaxrs.resource.Feefines.PostFeefinesResponse.respond422WithApplicationJson;
import static org.folio.rest.utils.ErrorHelper.createErrors;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.CQL2PgJSONException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.domain.AutomaticFeeFineType;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.FeefinedataCollection;
import org.folio.rest.jaxrs.model.FeefinesGetOrder;
import org.folio.rest.jaxrs.resource.Feefines;
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

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class FeeFinesAPI implements Feefines {

    public static final String FEEFINES_TABLE = "feefines";

    private final Messages messages = Messages.getInstance();
    private static final String FEEFINE_ID_FIELD = "'id'";
    private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
    private final Logger logger = LogManager.getLogger(FeeFinesAPI.class);
    private static final String VALIDATION_ERROR_MSG = "Attempt to delete/update an automatic fee/fine type";

    private CQLWrapper getCQL(String query, int limit, int offset) throws CQL2PgJSONException, IOException {
        CQL2PgJSON cql2pgJson = new CQL2PgJSON(FEEFINES_TABLE + ".jsonb");
        return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }

    @Validate
    @Override
    public void getFeefines(String query, String orderBy, FeefinesGetOrder order, int offset, int limit, List<String> facets, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        List<FacetField> facetList = FacetManager.convertFacetStrings2FacetFields(facets, "jsonb");
        try {
            CQLWrapper cql = getCQL(query, limit, offset);
            vertxContext.runOnContext(v -> {
                try {
                    PostgresClient postgresClient = PostgresClient.getInstance(
                            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
                    String[] fieldList = {"*"};

                    postgresClient.get(FEEFINES_TABLE, Feefine.class, fieldList, cql,
                            true, false, facetList, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        FeefinedataCollection feefineCollection = new FeefinedataCollection();
                                        List<Feefine> feefines = reply.result().getResults();
                                        feefineCollection.setFeefines(feefines);
                                        feefineCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                                        feefineCollection.setResultInfo(reply.result().getResultInfo());
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetFeefinesResponse.respond200WithApplicationJson(feefineCollection)));
                                    } else {
                                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                                GetFeefinesResponse.respond500WithTextPlain(
                                                        reply.cause().getMessage())));
                                    }

                                } catch (Exception e) {
                                    logger.debug(e.getLocalizedMessage());
                                    asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                            GetFeefinesResponse.respond500WithTextPlain(
                                                    reply.cause().getMessage())));
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getLocalizedMessage(), e);
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                        logger.debug("BAD CQL");
                        asyncResultHandler.handle(Future.succeededFuture(GetFeefinesResponse.respond400WithTextPlain(
                                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
                    } else {
                        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                                GetFeefinesResponse.respond500WithTextPlain(
                                        messages.getMessage(lang,
                                                MessageConsts.InternalServerError))));
                    }
                }
            });
        } catch (IOException | CQL2PgJSONException e) {

            logger.error(e.getLocalizedMessage(), e);
            if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
                logger.debug("BAD CQL");
                asyncResultHandler.handle(Future.succeededFuture(GetFeefinesResponse.respond400WithTextPlain(
                        "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
            } else {
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                        GetFeefinesResponse.respond500WithTextPlain(
                                messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void postFeefines(String lang, Feefine entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID().toString());
        }
        if (AutomaticFeeFineType.getById(entity.getId()) != null) {
            asyncResultHandler.handle(Future.succeededFuture(
              respond422WithApplicationJson(createErrors(new Error().withMessage(VALIDATION_ERROR_MSG)))
            ));
        } else {
            try {
                vertxContext.runOnContext(v -> {
                    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
                    PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

                    postgresClient.startTx(beginTx -> {
                        try {
                            postgresClient.save(beginTx, FEEFINES_TABLE, entity.getId(), entity, reply -> {
                                try {
                                    if (reply.succeeded()) {
                                        final Feefine feefine = entity;
                                        feefine.setId(entity.getId());
                                        postgresClient.endTx(beginTx, done
                                          -> asyncResultHandler.handle(Future.succeededFuture(PostFeefinesResponse.respond201WithApplicationJson(feefine,
                                          PostFeefinesResponse.headersFor201().withLocation(reply.result())))));

                                    } else {
                                        postgresClient.rollbackTx(beginTx, rollback -> {
                                            asyncResultHandler.handle(Future.succeededFuture(
                                              PostFeefinesResponse.respond400WithTextPlain(messages.getMessage(lang, MessageConsts.UnableToProcessRequest))));
                                        });
                                    }
                                } catch (Exception e) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                      PostFeefinesResponse.respond500WithTextPlain(
                                        e.getMessage())));
                                }
                            });
                        } catch (Exception e) {
                            postgresClient.rollbackTx(beginTx, rollback -> {
                                asyncResultHandler.handle(Future.succeededFuture(
                                  PostFeefinesResponse.respond500WithTextPlain(
                                    e.getMessage())));
                            });
                        }
                    });

                });
            } catch (Exception e) {
                asyncResultHandler.handle(Future.succeededFuture(
                  PostFeefinesResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void getFeefinesByFeefineId(String feefineId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
        try {
            vertxContext.runOnContext(v -> {
                String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                try {
                    Criteria idCrit = new Criteria();
                    idCrit.addField(FEEFINE_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setVal(feefineId);
                    Criterion criterion = new Criterion(idCrit);

                    PostgresClient.getInstance(vertxContext.owner(), tenantId).get(FEEFINES_TABLE, Feefine.class, criterion,
                            true, false, getReply -> {
                                if (getReply.failed()) {
                                    asyncResultHandler.handle(Future.succeededFuture(
                                            GetFeefinesByFeefineIdResponse.respond500WithTextPlain(
                                                    messages.getMessage(lang, MessageConsts.InternalServerError))));
                                } else {
                                    List<Feefine> feefineList = getReply.result().getResults();
                                    if (feefineList.isEmpty()) {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetFeefinesByFeefineIdResponse.respond404WithTextPlain("Feefine"
                                                        + messages.getMessage(lang,
                                                                MessageConsts.ObjectDoesNotExist))));
                                    } else if (feefineList.size() > 1) {
                                        logger.error("Multiple feefines found with the same id");
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetFeefinesByFeefineIdResponse.respond500WithTextPlain(
                                                        messages.getMessage(lang,
                                                                MessageConsts.InternalServerError))));
                                    } else {
                                        asyncResultHandler.handle(Future.succeededFuture(
                                                GetFeefinesByFeefineIdResponse.respond200WithApplicationJson(feefineList.get(0))));
                                    }
                                }
                            });
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    asyncResultHandler.handle(Future.succeededFuture(
                            GetFeefinesResponse.respond500WithTextPlain(messages.getMessage(
                                    lang, MessageConsts.InternalServerError))));
                }

            });
        } catch (Exception e) {
            asyncResultHandler.handle(Future.succeededFuture(
                    GetFeefinesResponse.respond500WithTextPlain(messages.getMessage(
                            lang, MessageConsts.InternalServerError))));
        }
    }

    @Validate
    @Override
    public void deleteFeefinesByFeefineId(String feefineId,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        if (AutomaticFeeFineType.getById(feefineId) != null) {
            asyncResultHandler.handle(Future.succeededFuture(
              respond422WithApplicationJson(createErrors(new Error().withMessage(VALIDATION_ERROR_MSG)))
            ));
        } else {
            try {
                vertxContext.runOnContext(v -> {
                    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                    Criteria idCrit = new Criteria();
                    idCrit.addField(FEEFINE_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setVal(feefineId);
                    Criterion criterion = new Criterion(idCrit);

                    try {
                        PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
                          FEEFINES_TABLE, criterion, deleteReply -> {
                              if (deleteReply.succeeded()) {
                                  if (deleteReply.result().rowCount() == 1) {
                                      asyncResultHandler.handle(Future.succeededFuture(
                                        DeleteFeefinesByFeefineIdResponse.respond204()));
                                  } else {
                                      asyncResultHandler.handle(Future.succeededFuture(
                                        DeleteFeefinesByFeefineIdResponse.respond404WithTextPlain("Record Not Found")));
                                  }
                              } else {
                                  logger.error(deleteReply.result());
                                  String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                                  logger.error(error, deleteReply.cause());
                                  if (error == null) {
                                      asyncResultHandler.handle(Future.succeededFuture(DeleteFeefinesByFeefineIdResponse.respond500WithTextPlain(
                                        messages.getMessage(lang, MessageConsts.InternalServerError))));
                                  } else {
                                      asyncResultHandler.handle(Future.succeededFuture(DeleteFeefinesByFeefineIdResponse.respond400WithTextPlain(error)));
                                  }
                              }
                          });
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        asyncResultHandler.handle(
                          Future.succeededFuture(
                            DeleteFeefinesByFeefineIdResponse.respond500WithTextPlain(
                              messages.getMessage(lang,
                                MessageConsts.InternalServerError))));
                    }

                });
            } catch (Exception e) {
                logger.error(e.getMessage());
                asyncResultHandler.handle(
                  Future.succeededFuture(
                    DeleteFeefinesByFeefineIdResponse.respond500WithTextPlain(
                      messages.getMessage(lang,
                        MessageConsts.InternalServerError))));
            }
        }
    }

    @Validate
    @Override
    public void putFeefinesByFeefineId(String feefineId,
      String lang, Feefine entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

        if (AutomaticFeeFineType.getById(entity.getId()) != null) {
            asyncResultHandler.handle(Future.succeededFuture(
              respond422WithApplicationJson(createErrors(new Error().withMessage(VALIDATION_ERROR_MSG)))
            ));
        } else {
            try {
                if (feefineId == null) {
                    logger.error("feefineId is missing");
                    asyncResultHandler.handle(Future.succeededFuture(PutFeefinesByFeefineIdResponse.respond400WithTextPlain("feefineId is missing")));
                }

                vertxContext.runOnContext(v -> {
                    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

                    Criteria idCrit = new Criteria();
                    idCrit.addField(FEEFINE_ID_FIELD);
                    idCrit.setOperation("=");
                    idCrit.setVal(feefineId);
                    Criterion criterion = new Criterion(idCrit);

                    try {
                        PostgresClient.getInstance(vertxContext.owner(), tenantId).get(FEEFINES_TABLE,
                          Feefine.class, criterion, true, false, getReply -> {
                              if (getReply.failed()) {
                                  logger.error(getReply.cause().getLocalizedMessage());
                                  asyncResultHandler.handle(Future.succeededFuture(
                                    PutFeefinesByFeefineIdResponse.respond500WithTextPlain(
                                      messages.getMessage(lang,
                                        MessageConsts.InternalServerError))));
                              } else if (getReply.result().getResults().size() == 1) {
                                  try {
                                      PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                                        FEEFINES_TABLE, entity, criterion, true, putReply -> {
                                            if (putReply.failed()) {
                                                asyncResultHandler.handle(Future.succeededFuture(
                                                  PutFeefinesByFeefineIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                                            } else if (putReply.result().rowCount() == 1) {
                                                asyncResultHandler.handle(Future.succeededFuture(
                                                  PutFeefinesByFeefineIdResponse.respond204()));
                                            }
                                        });
                                  } catch (Exception e) {
                                      asyncResultHandler.handle(Future.succeededFuture(
                                        PutFeefinesByFeefineIdResponse.respond500WithTextPlain(messages.getMessage(lang,
                                          MessageConsts.InternalServerError))));
                                  }
                              } else if (getReply.result().getResults().isEmpty()) {
                                  asyncResultHandler.handle(Future.succeededFuture(
                                    PutFeefinesByFeefineIdResponse.respond404WithTextPlain("Record Not Found")));
                              } else if (getReply.result().getResults().size() > 1) {
                                  asyncResultHandler.handle(Future.succeededFuture(
                                    PutFeefinesByFeefineIdResponse.respond404WithTextPlain("Multiple fee/fine records")));
                              }
                          });
                    } catch (Exception e) {
                        logger.error(e.getLocalizedMessage(), e);
                        asyncResultHandler.handle(Future.succeededFuture(
                          PutFeefinesByFeefineIdResponse.respond500WithTextPlain(
                            messages.getMessage(lang, MessageConsts.InternalServerError))));
                    }
                });
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
                asyncResultHandler.handle(Future.succeededFuture(
                  PutFeefinesByFeefineIdResponse.respond500WithTextPlain(
                    messages.getMessage(lang, MessageConsts.InternalServerError))));
            }
        }
    }
}
