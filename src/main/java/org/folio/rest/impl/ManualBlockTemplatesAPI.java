package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import java.util.Map;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.ManualBlockTemplate;
import org.folio.rest.jaxrs.model.ManualBlockTemplateCollection;
import org.folio.rest.jaxrs.model.ManualBlockTemplatesGetOrder;
import org.folio.rest.jaxrs.resource.ManualBlockTemplates;
import org.folio.rest.persist.PgUtil;

public class ManualBlockTemplatesAPI implements ManualBlockTemplates {

  private static final String TEMPLATES_TABLE = "manual_block_templates";

  @Override
  @Validate
  public void getManualBlockTemplates(String query, String orderBy,
      ManualBlockTemplatesGetOrder order, @Min(0) @Max(2147483647) int offset,
      @Min(0) @Max(2147483647) int limit, @Pattern(regexp = "[a-zA-Z]{2}") String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil
        .get(TEMPLATES_TABLE, ManualBlockTemplate.class, ManualBlockTemplateCollection.class, query,
            offset, limit, okapiHeaders, vertxContext, GetManualBlockTemplatesResponse.class,
            asyncResultHandler);
  }

  @Override
  @Validate
  public void postManualBlockTemplates(@Pattern(regexp = "[a-zA-Z]{2}") String lang,
      ManualBlockTemplate entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(TEMPLATES_TABLE, entity, okapiHeaders, vertxContext,
        PostManualBlockTemplatesResponse.class, asyncResultHandler);

  }

  @Override
  @Validate
  public void getManualBlockTemplatesById(String id, @Pattern(regexp = "[a-zA-Z]{2}") String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.getById(TEMPLATES_TABLE, ManualBlockTemplate.class, id, okapiHeaders, vertxContext,
        GetManualBlockTemplatesByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void putManualBlockTemplatesById(String id, @Pattern(regexp = "[a-zA-Z]{2}") String lang,
      ManualBlockTemplate entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(TEMPLATES_TABLE, entity, id, okapiHeaders, vertxContext,
        PutManualBlockTemplatesByIdResponse.class, asyncResultHandler);
  }

  @Override
  @Validate
  public void deleteManualBlockTemplatesById(String id,
      @Pattern(regexp = "[a-zA-Z]{2}") String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(TEMPLATES_TABLE, id, okapiHeaders, vertxContext,
        DeleteManualBlockTemplatesByIdResponse.class, asyncResultHandler);
  }
}
