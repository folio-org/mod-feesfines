package org.folio.rest.impl;

import static org.folio.rest.tools.messages.Messages.DEFAULT_LANGUAGE;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
import org.folio.rest.jaxrs.model.Comment;
import org.folio.rest.jaxrs.model.CommentdataCollection;
import org.folio.rest.jaxrs.model.CommentsGetOrder;
import org.folio.rest.jaxrs.resource.Comments;
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

public class CommentsAPI implements Comments {

  private static final String COMMENTS_TABLE = "comments";
  private static final String COMMENT_ID_FIELD = "'id'";
  private static final String OKAPI_HEADER_TENANT = "x-okapi-tenant";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LogManager.getLogger(CommentsAPI.class);

  private CQLWrapper getCQL(String query, int limit, int offset) throws CQL2PgJSONException, IOException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(COMMENTS_TABLE + ".jsonb");
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
  }

  @Validate
  @Override
  public void getComments(String query, String orderBy, CommentsGetOrder order, String totalRecords,
    int offset, int limit, List<String> facets, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
    List<FacetField> facetList = FacetManager.convertFacetStrings2FacetFields(facets, "jsonb");
    try {
      CQLWrapper cql = getCQL(query, limit, offset);
      vertxContext.runOnContext(v -> {
        try {
          PostgresClient postgresClient = PostgresClient.getInstance(
            vertxContext.owner(), TenantTool.calculateTenantId(tenantId));
          String[] fieldList = {"*"};

          postgresClient.get(COMMENTS_TABLE, Comment.class, fieldList, cql,
            true, false, facetList, reply -> {
              try {
                if (reply.succeeded()) {
                  CommentdataCollection commentCollection = new CommentdataCollection();
                  List<Comment> comments = reply.result().getResults();
                  commentCollection.setComments(comments);
                  commentCollection.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                  commentCollection.setResultInfo(reply.result().getResultInfo());
                  asyncResultHandler.handle(Future.succeededFuture(
                    GetCommentsResponse.respond200WithApplicationJson(commentCollection)));
                } else {
                  asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                    GetCommentsResponse.respond500WithTextPlain(
                      reply.cause().getMessage())));
                }
              } catch (Exception e) {
                logger.debug(e.getLocalizedMessage());
                asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
                  GetCommentsResponse.respond500WithTextPlain(
                    reply.cause().getMessage())));
              }
            });
        } catch (Exception e) {
          logger.error(e.getLocalizedMessage(), e);
          if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
            logger.debug("BAD CQL");
            asyncResultHandler.handle(Future.succeededFuture(GetCommentsResponse.respond400WithTextPlain(
              "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
          } else {
            asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
              GetCommentsResponse.respond500WithTextPlain(
                messages.getMessage(DEFAULT_LANGUAGE,
                  MessageConsts.InternalServerError))));
          }
        }
      });
    } catch (IOException | CQL2PgJSONException e) {
      logger.error(e.getLocalizedMessage(), e);
      if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("CQLParseException")) {
        logger.debug("BAD CQL");
        asyncResultHandler.handle(Future.succeededFuture(GetCommentsResponse.respond400WithTextPlain(
          "CQL Parsing Error for '" + query + "': " + e.getLocalizedMessage())));
      } else {
        asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(
          GetCommentsResponse.respond500WithTextPlain(
            messages.getMessage(DEFAULT_LANGUAGE,
              MessageConsts.InternalServerError))));
      }
    }
  }

  @Validate
  @Override
  public void postComments(Comment entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }
    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        PostgresClient postgresClient = PostgresClient.getInstance(vertxContext.owner(), tenantId);

        postgresClient.startTx(beginTx -> {
          try {
            postgresClient.save(beginTx, COMMENTS_TABLE, entity.getId(), entity, reply -> {
              try {
                if (reply.succeeded()) {
                  final Comment comment = entity;
                  comment.setId(entity.getId());
                  postgresClient.endTx(beginTx, done
                    -> asyncResultHandler.handle(
                    Future.succeededFuture(PostCommentsResponse.respond201WithApplicationJson(comment,
                      PostCommentsResponse.headersFor201().withLocation(reply.result())))));

                } else {
                  postgresClient.rollbackTx(beginTx, rollback -> {
                    asyncResultHandler.handle(Future.succeededFuture(
                      PostCommentsResponse.respond400WithTextPlain(
                        messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.UnableToProcessRequest))));
                  });
                }
              } catch (Exception e) {
                asyncResultHandler.handle(Future.succeededFuture(
                  PostCommentsResponse.respond500WithTextPlain(
                    e.getMessage())));
              }
            });
          } catch (Exception e) {
            postgresClient.rollbackTx(beginTx, rollback -> {
              asyncResultHandler.handle(Future.succeededFuture(
                PostCommentsResponse.respond500WithTextPlain(
                  e.getMessage())));
            });
          }
        });
      });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(
        PostCommentsResponse.respond500WithTextPlain(
          messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
    }
  }

  @Validate
  @Override
  public void getCommentsByCommentId(String commentId, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        try {
          Criteria idCrit = new Criteria();
          idCrit.addField(COMMENT_ID_FIELD);
          idCrit.setOperation("=");
          idCrit.setVal(commentId);
          Criterion criterion = new Criterion(idCrit);

          PostgresClient.getInstance(vertxContext.owner(), tenantId).get(COMMENTS_TABLE, Comment.class, criterion,
            true, false, getReply -> {
              if (getReply.failed()) {
                logger.error(getReply.result());
                asyncResultHandler.handle(Future.succeededFuture(
                  GetCommentsByCommentIdResponse.respond500WithTextPlain(
                    messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
              } else {
                List<Comment> commentList = getReply.result().getResults();
                if (commentList.isEmpty()) {
                  asyncResultHandler.handle(Future.succeededFuture(
                    GetCommentsByCommentIdResponse.respond404WithTextPlain("Comment"
                      + messages.getMessage(DEFAULT_LANGUAGE,
                      MessageConsts.ObjectDoesNotExist))));
                } else if (commentList.size() > 1) {
                  logger.error("Multiple comments found with the same id");
                  asyncResultHandler.handle(Future.succeededFuture(
                    GetCommentsByCommentIdResponse.respond500WithTextPlain(
                      messages.getMessage(DEFAULT_LANGUAGE,
                        MessageConsts.InternalServerError))));
                } else {
                  asyncResultHandler.handle(Future.succeededFuture(
                    GetCommentsByCommentIdResponse.respond200WithApplicationJson(commentList.get(0))));
                }
              }
            });
        } catch (Exception e) {
          logger.error(e.getMessage());
          asyncResultHandler.handle(Future.succeededFuture(
            GetCommentsResponse.respond500WithTextPlain(messages.getMessage(
              DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
        }
      });
    } catch (Exception e) {
      asyncResultHandler.handle(Future.succeededFuture(
        GetCommentsResponse.respond500WithTextPlain(messages.getMessage(
          DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
    }
  }

  @Validate
  @Override
  public void deleteCommentsByCommentId(String commentId, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));
        Criteria idCrit = new Criteria();
        idCrit.addField(COMMENT_ID_FIELD);
        idCrit.setOperation("=");
        idCrit.setVal(commentId);
        Criterion criterion = new Criterion(idCrit);

        try {
          PostgresClient.getInstance(vertxContext.owner(), tenantId).delete(
            COMMENTS_TABLE, criterion, deleteReply -> {
              if (deleteReply.succeeded()) {
                if (deleteReply.result().rowCount() == 1) {
                  asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCommentsByCommentIdResponse.respond204()));
                } else {
                  asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCommentsByCommentIdResponse.respond404WithTextPlain("Record Not Found")));
                }
              } else {
                logger.error(deleteReply.result());
                String error = PgExceptionUtil.badRequestMessage(deleteReply.cause());
                logger.error(error, deleteReply.cause());
                if (error == null) {
                  asyncResultHandler.handle(Future.succeededFuture(DeleteCommentsByCommentIdResponse.respond500WithTextPlain(
                    messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
                } else {
                  asyncResultHandler.handle(
                    Future.succeededFuture(DeleteCommentsByCommentIdResponse.respond400WithTextPlain(error)));
                }
              }
            });
        } catch (Exception e) {
          logger.error(e.getMessage());
          asyncResultHandler.handle(
            Future.succeededFuture(
              DeleteCommentsByCommentIdResponse.respond500WithTextPlain(
                messages.getMessage(DEFAULT_LANGUAGE,
                  MessageConsts.InternalServerError))));
        }
      });
    } catch (Exception e) {
      logger.error(e.getMessage());
      asyncResultHandler.handle(
        Future.succeededFuture(
          DeleteCommentsByCommentIdResponse.respond500WithTextPlain(
            messages.getMessage(DEFAULT_LANGUAGE,
              MessageConsts.InternalServerError))));
    }
  }

  @Validate
  @Override
  public void putCommentsByCommentId(String commentId, Comment entity,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    try {
      if (commentId == null) {
        logger.error("commentId is missing");
        asyncResultHandler.handle(
          Future.succeededFuture(PutCommentsByCommentIdResponse.respond400WithTextPlain("commentId is missing")));
      }

      vertxContext.runOnContext(v -> {
        String tenantId = TenantTool.calculateTenantId(okapiHeaders.get(OKAPI_HEADER_TENANT));

        Criteria idCrit = new Criteria();
        idCrit.addField(COMMENT_ID_FIELD);
        idCrit.setOperation("=");
        idCrit.setVal(commentId);
        Criterion criterion = new Criterion(idCrit);

        try {
          PostgresClient.getInstance(vertxContext.owner(), tenantId).get(COMMENTS_TABLE,
            Comment.class, criterion, true, false, getReply -> {
              if (getReply.failed()) {
                logger.error(getReply.cause().getLocalizedMessage());
                asyncResultHandler.handle(Future.succeededFuture(
                  PutCommentsByCommentIdResponse.respond500WithTextPlain(
                    messages.getMessage(DEFAULT_LANGUAGE,
                      MessageConsts.InternalServerError))));
              } else if (getReply.result().getResults().size() == 1) {
                try {
                  PostgresClient.getInstance(vertxContext.owner(), tenantId).update(
                    COMMENTS_TABLE, entity, criterion, true, putReply -> {
                      if (putReply.failed()) {
                        asyncResultHandler.handle(Future.succeededFuture(
                          PutCommentsByCommentIdResponse.respond500WithTextPlain(putReply.cause().getMessage())));
                      } else if (putReply.result().rowCount() == 1) {
                        asyncResultHandler.handle(Future.succeededFuture(
                          PutCommentsByCommentIdResponse.respond204()));
                      }
                    });
                } catch (Exception e) {
                  logger.error(e.getLocalizedMessage(), e);
                  asyncResultHandler.handle(Future.succeededFuture(
                    PutCommentsByCommentIdResponse.respond500WithTextPlain(messages.getMessage(DEFAULT_LANGUAGE,
                      MessageConsts.InternalServerError))));
                }
              } else if (getReply.result().getResults().isEmpty()) {
                asyncResultHandler.handle(Future.succeededFuture(
                  PutCommentsByCommentIdResponse.respond404WithTextPlain("Record Not Found")));
              } else if (getReply.result().getResults().size() > 1) {
                asyncResultHandler.handle(Future.succeededFuture(
                  PutCommentsByCommentIdResponse.respond404WithTextPlain("Multiple comment records")));
              }
            });
        } catch (Exception e) {
          logger.error(e.getLocalizedMessage(), e);
          asyncResultHandler.handle(Future.succeededFuture(
            PutCommentsByCommentIdResponse.respond500WithTextPlain(
              messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
        }
      });
    } catch (Exception e) {
      logger.error(e.getLocalizedMessage(), e);
      asyncResultHandler.handle(Future.succeededFuture(
        PutCommentsByCommentIdResponse.respond500WithTextPlain(
          messages.getMessage(DEFAULT_LANGUAGE, MessageConsts.InternalServerError))));
    }
  }
}
