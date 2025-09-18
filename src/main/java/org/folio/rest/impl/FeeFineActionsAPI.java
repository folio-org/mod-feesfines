package org.folio.rest.impl;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.folio.rest.service.LogEventPublisher.LogEventPayloadType.FEE_FINE;
import static org.folio.rest.tools.messages.Messages.DEFAULT_LANGUAGE;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.CQL2PgJSONException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.FeefineactiondataCollection;
import org.folio.rest.jaxrs.model.FeefineactionsGetOrder;
import org.folio.rest.jaxrs.resource.Feefineactions;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.service.LogEventPublisher;
import org.folio.rest.service.LogEventService;
import org.folio.rest.service.PatronNoticeService;
import org.folio.rest.tools.messages.MessageConsts;
import org.folio.rest.tools.messages.Messages;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class FeeFineActionsAPI implements Feefineactions {

  private static final String FEEFINEACTIONS_TABLE = "feefineactions";

  private final Messages messages = Messages.getInstance();
  private static final String FEEFINEACTION_ID_FIELD = "'id'";
  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
  private final Logger logger = LogManager.getLogger(FeeFineActionsAPI.class);

  private CQLWrapper getCQL(String query, int limit, int offset)
    throws CQL2PgJSONException, IOException {

    CQL2PgJSON cql2pgJson = new CQL2PgJSON(FEEFINEACTIONS_TABLE + ".jsonb");
    return new CQLWrapper(cql2pgJson, query)
      .setLimit(new Limit(limit))
      .setOffset(new Offset(offset));
  }

  @Validate
  @Override
  public void getFeefineactions(String query, String orderBy, FeefineactionsGetOrder order,
    String totalRecords, int offset, int limit, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

    try {
      CQLWrapper cql = getCQL(query, limit, offset);
      vertxContext.runOnContext(v -> {
        try {
          PostgresClient postgresClient = PostgresClient.getInstance(
            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
          String[] fieldList = {"*"};

          postgresClient.get(FEEFINEACTIONS_TABLE, Feefineaction.class, fieldList, cql,
            true, false, reply -> {
              try {
                if (reply.succeeded()) {
                  FeefineactiondataCollection feefineactionCollection =
                    new FeefineactiondataCollection();
                  List<Feefineaction> feefineactions = reply.result().getResults();
                  feefineactionCollection.setFeefineactions(feefineactions);
                  feefineactionCollection.setTotalRecords(
                    reply.result().getResultInfo().getTotalRecords());
                  asyncResultHandler.handle(Future.succeededFuture(
                    GetFeefineactionsResponse.respond200WithApplicationJson(
                      feefineactionCollection)));
                } else {
                  asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                    GetFeefineactionsResponse.respond500WithTextPlain(
                      reply.cause().getMessage())));
                }
              } catch (Exception e) {
                logger.debug(e.getLocalizedMessage());
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                  GetFeefineactionsResponse.respond500WithTextPlain(
                    reply.cause().getMessage())));
              }
            }
          );
        } catch (Exception e) {
          logger.error(e.getLocalizedMessage(), e);
          if (e.getCause() != null && e.getCause().getClass().getSimpleName()
            .contains("CQLParseException")) {

            logger.debug("BAD CQL");
            asyncResultHandler.handle(Future.succeededFuture(
              GetFeefineactionsResponse.respond400WithTextPlain(
                "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
          } else {
            asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
              GetFeefineactionsResponse.respond500WithTextPlain(
                messages.getMessage(DEFAULT_LANGUAGE,
                  MessageConsts.InternalServerError))));
          }
        }
      });
    } catch (IOException | CQL2PgJSONException e) {

      logger.error(e.getLocalizedMessage(), e);
      if (e.getCause() != null && e.getCause().getClass().getSimpleName()
        .contains("CQLParseException")) {

        logger.debug("BAD CQL");
        asyncResultHandler.handle(Future.succeededFuture(
          GetFeefineactionsResponse.respond400WithTextPlain(
            "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
          GetFeefineactionsResponse.respond500WithTextPlain(
            messages.getMessage(DEFAULT_LANGUAGE,
              MessageConsts.InternalServerError))));
      }
    }
  }

  @Validate
  @Override
  public void postFeefineactions(Feefineaction entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    PgUtil.post(FEEFINEACTIONS_TABLE, entity, okapiHeaders, vertxContext, PostFeefineactionsResponse.class)
      .onSuccess(r -> publishLogEvent(entity, okapiHeaders, vertxContext))
      .onSuccess(r -> sendPatronNoticeIfNeedBe(entity, okapiHeaders, vertxContext))
      .onComplete(asyncResultHandler);
  }

  private void sendPatronNoticeIfNeedBe(Feefineaction action, Map<String, String> okapiHeaders,
    Context vertxContext) {

    if (isTrue(action.getNotify())) {
      new PatronNoticeService(vertxContext.owner(), okapiHeaders)
        .sendPatronNotice(action);
    }
  }

  private void publishLogEvent(Feefineaction entity, Map<String, String> okapiHeaders,
    Context vertxContext) {

    new LogEventService(vertxContext.owner(), okapiHeaders)
      .createFeeFineLogEventPayload(entity)
      .onSuccess(eventPayload -> new LogEventPublisher(vertxContext, okapiHeaders)
        .publishLogEvent(eventPayload, FEE_FINE));
  }

  @Validate
  @Override
  public void getFeefineactionsByFeefineactionId(String feefineactionId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        try {
          Criteria idCrit = new Criteria();
          idCrit.addField(FEEFINEACTION_ID_FIELD);
          idCrit.setOperation("=");
          idCrit.setVal(feefineactionId);
          Criterion criterion = new Criterion(idCrit);
          PostgresClient.getInstance(vertxContext.owner(), tenantId)
            .get(FEEFINEACTIONS_TABLE, Feefineaction.class, criterion, true, false, getReply -> {
              if (getReply.failed()) {
                logger.error(getReply.result());
                asyncResultHandler.handle(Future.succeededFuture(
                  GetFeefineactionsByFeefineactionIdResponse.respond500WithTextPlain(
                    messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
              } else {
                List<Feefineaction> feefineactionList = getReply.result().getResults();
                if (feefineactionList.isEmpty()) {
                  asyncResultHandler.handle(Future.succeededFuture(
                    GetFeefineactionsByFeefineactionIdResponse.respond404WithTextPlain(
                      "Feefineaction" + messages.getMessage(DEFAULT_LANGUAGE,
                        MessageConsts.ObjectDoesNotExist))));
                } else if (feefineactionList.size() > 1) {
                  logger.error("Multiple feefineactions found with the same id");
                  asyncResultHandler.handle(Future.succeededFuture(
                    GetFeefineactionsByFeefineactionIdResponse.respond500WithTextPlain(
                      messages.getMessage(DEFAULT_LANGUAGE,
                        MessageConsts.InternalServerError))));
                } else {
                  asyncResultHandler.handle(Future.succeededFuture(
                    GetFeefineactionsByFeefineactionIdResponse
                      .respond200WithApplicationJson(feefineactionList.get(0))));
                }
              }
            });
        } catch (Exception e) {
          logger.error(e.getMessage());
          asyncResultHandler.handle(Future.succeededFuture(
            GetFeefineactionsResponse.respond500WithTextPlain(messages.getMessage(
              DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
        }
      });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(
        GetFeefineactionsResponse.respond500WithTextPlain(messages.getMessage(
          DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
    }
  }

  @Validate
  @Override
  public void deleteFeefineactionsByFeefineactionId(String feefineactionId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        Criteria idCriteria = new Criteria();
        idCriteria.addField(FEEFINEACTION_ID_FIELD);
        idCriteria.setOperation("=");
        idCriteria.setVal(feefineactionId);
        Criterion criterion = new Criterion(idCriteria);

        try {
          PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
            FEEFINEACTIONS_TABLE, criterion, deleteReply -> {
              if (deleteReply.succeeded()) {
                if (deleteReply.result().rowCount() == 1) {
                  asyncResultHandler.handle(Future.succeededFuture(
                    DeleteFeefineactionsByFeefineactionIdResponse.respond204()));
                } else {
                  asyncResultHandler.handle(Future.succeededFuture(
                    DeleteFeefineactionsByFeefineactionIdResponse
                      .respond404WithTextPlain("Record Not Found")));
                }
              } else {
                logger.error(deleteReply.result());
                String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                logger.error(error, deleteReply.cause());
                if (error == null) {
                  asyncResultHandler.handle(Future.succeededFuture(
                    DeleteFeefineactionsByFeefineactionIdResponse.respond500WithTextPlain(
                      messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
                } else {
                  asyncResultHandler.handle(Future.succeededFuture(
                    DeleteFeefineactionsByFeefineactionIdResponse.respond400WithTextPlain(error)));
                }
              }
            });
        } catch (Exception e) {
          logger.error(e.getMessage());
          asyncResultHandler.handle(
            Future.succeededFuture(
              DeleteFeefineactionsByFeefineactionIdResponse.respond500WithTextPlain(
                messages.getMessage(DEFAULT_LANGUAGE,
                  MessageConsts.InternalServerError))));
        }

      });
    } catch (Exception e) {
      logger.error(e.getMessage());
      asyncResultHandler.handle(
        Future.succeededFuture(
          DeleteFeefineactionsByFeefineactionIdResponse.respond500WithTextPlain(
            messages.getMessage(DEFAULT_LANGUAGE,
              MessageConsts.InternalServerError))));
    }
  }

  @Validate
  @Override
  public void putFeefineactionsByFeefineactionId(String feefineactionId, Feefineaction entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    try {
      if (feefineactionId == null) {
        logger.error("feefineactionId is missing");
        asyncResultHandler.handle(Future.succeededFuture(PutFeefineactionsByFeefineactionIdResponse
          .respond400WithTextPlain("feefineactionId is missing")));
      }

      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        Criteria idCrit = new Criteria();
        idCrit.addField(FEEFINEACTION_ID_FIELD);
        idCrit.setOperation("=");
        idCrit.setVal(feefineactionId);
        Criterion criterion = new Criterion(idCrit);

        try {
          PostgresClient.getInstance(vertxContext.owner(), tenantId).get(FEEFINEACTIONS_TABLE,
            Feefineaction.class, criterion, true, false, getReply -> {
              if (getReply.failed()) {
                logger.error(getReply.cause().getLocalizedMessage());
                asyncResultHandler.handle(Future.succeededFuture(
                  PutFeefineactionsByFeefineactionIdResponse.respond500WithTextPlain(
                    messages.getMessage(DEFAULT_LANGUAGE,
                      MessageConsts.InternalServerError))));
              } else if (getReply.result().getResults().size() == 1) {
                try {
                  PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                    FEEFINEACTIONS_TABLE, entity, criterion, true, putReply -> {
                      if (putReply.failed()) {
                        asyncResultHandler.handle(Future.succeededFuture(
                          PutFeefineactionsByFeefineactionIdResponse
                            .respond500WithTextPlain(putReply.cause().getMessage())));
                      } else if (putReply.result().rowCount() == 1) {
                        asyncResultHandler.handle(Future.succeededFuture(
                          PutFeefineactionsByFeefineactionIdResponse.respond204()));
                      }
                    });
                } catch (Exception e) {
                  asyncResultHandler.handle(Future.succeededFuture(
                    PutFeefineactionsByFeefineactionIdResponse
                      .respond500WithTextPlain(messages.getMessage(DEFAULT_LANGUAGE,
                        MessageConsts.InternalServerError))));
                }
              } else if (getReply.result().getResults().isEmpty()) {
                asyncResultHandler.handle(Future.succeededFuture(
                  PutFeefineactionsByFeefineactionIdResponse
                    .respond404WithTextPlain("Record Not Found")));
              } else if (getReply.result().getResults().size() > 1) {
                asyncResultHandler.handle(Future.succeededFuture(
                  PutFeefineactionsByFeefineactionIdResponse
                    .respond404WithTextPlain("Multiple fee/fine action records")));
              }
            });
        } catch (Exception e) {
          logger.error(e.getLocalizedMessage(), e);
          asyncResultHandler.handle(Future.succeededFuture(
            PutFeefineactionsByFeefineactionIdResponse.respond500WithTextPlain(
              messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
        }
      });
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        PutFeefineactionsByFeefineactionIdResponse.respond500WithTextPlain(
          messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
    }
  }
}
