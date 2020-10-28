package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.core.Response;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.CQL2PgJSONException;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.ManualblockTemplate;
import org.folio.rest.jaxrs.model.ManualblockTemplatesGetOrder;
import org.folio.rest.jaxrs.model.Manualblocktemplate;
import org.folio.rest.jaxrs.model.ManualblocktemplateCollection;
import org.folio.rest.jaxrs.resource.ManualblockTemplates;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.messages.Messages;

public class ManualBlockTemplatesAPI implements ManualblockTemplates {

  private static final String TEMPLATES_TABLE = "manualblock_teamplates";
  private final Messages messages = Messages.getInstance();
  private final Logger logger = LoggerFactory.getLogger(ManualBlocksAPI.class);

  private CQLWrapper getCQL(String query, int limit, int offset)
      throws CQL2PgJSONException, IOException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(TEMPLATES_TABLE + ".jsonb");
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit))
        .setOffset(new Offset(offset));
  }

  @Override
  @Validate
  public void getManualblockTemplates(String query, String orderBy,
      ManualblockTemplatesGetOrder order, @Min(0) @Max(2147483647) int offset,
      @Min(0) @Max(2147483647) int limit, @Pattern(regexp = "[a-zA-Z]{2}") String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil
        .get(TEMPLATES_TABLE, ManualblockTemplate.class, ManualblocktemplateCollection.class, query,
            offset, limit, okapiHeaders, vertxContext, GetManualblockTemplatesResponse.class,
            asyncResultHandler);
  }

  @Override
  @Validate
  public void postManualblockTemplates(@Pattern(regexp = "[a-zA-Z]{2}") String lang,
      Manualblocktemplate entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(TEMPLATES_TABLE, entity, okapiHeaders, vertxContext,
        PostManualblockTemplatesResponse.class, asyncResultHandler);

  }

  @Override
  @Validate
  public void getManualblockTemplatesById(String id, @Pattern(regexp = "[a-zA-Z]{2}") String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.getById(TEMPLATES_TABLE, Manualblocktemplate.class, id, okapiHeaders, vertxContext,
        GetManualblockTemplatesByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void putManualblockTemplatesById(String id, @Pattern(regexp = "[a-zA-Z]{2}") String lang,
      Manualblocktemplate entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(TEMPLATES_TABLE, entity, okapiHeaders, vertxContext,
        PutManualblockTemplatesByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteManualblockTemplatesById(String id,
      @Pattern(regexp = "[a-zA-Z]{2}") String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(TEMPLATES_TABLE, id, okapiHeaders, vertxContext,
        DeleteManualblockTemplatesByIdResponse.class, asyncResultHandler);
  }
}
