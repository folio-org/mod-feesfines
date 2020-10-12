package org.folio.rest.domain.logs;

import static java.util.Optional.ofNullable;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.INSTANCE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEMS;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEM_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.LOAN_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.SOURCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.TEMPLATE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.TRIGGERING_EVENT;
import static org.folio.rest.domain.logs.LogEventPayloadField.USER_BARCODE;
import static org.folio.rest.domain.logs.LogEventPayloadField.USER_ID;
import static org.folio.rest.utils.JsonHelper.write;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.domain.FeeFineNoticeContext;

public class NoticeLogContextUtil {
  private NoticeLogContextUtil() {}

  public static JsonObject buildNoticeLogContext(FeeFineNoticeContext context) {
    JsonObject contextJson = new JsonObject();
    JsonObject itemJson = new JsonObject();
    ofNullable(context.getUser()).ifPresent(user -> {
      write(contextJson, USER_ID.value(), user.getId());
      write(contextJson, USER_BARCODE.value(), user.getBarcode());
    });
    ofNullable(context.getAction()).ifPresent(action -> {
      write(contextJson, SOURCE.value(), action.getSource());
      write(itemJson, SERVICE_POINT_ID.value(), action.getCreatedAt());
      write(itemJson, TRIGGERING_EVENT.value(), action.getTypeAction());
    });
    ofNullable(context.getAccount()).ifPresent(account -> {
      write(contextJson, FEE_FINE_ID.value(), account.getFeeFineId());
      write(itemJson, ITEM_BARCODE.value(), account.getBarcode());
      write(itemJson, INSTANCE_ID.value(), account.getInstanceId());
      write(itemJson, HOLDINGS_RECORD_ID.value(), account.getHoldingsRecordId());
      write(itemJson, LOAN_ID.value(), account.getLoanId());
    });
    ofNullable(context.getItem()).ifPresent(item -> write(itemJson, ITEM_ID.value(), item.getId()));
    write(itemJson, TEMPLATE_ID.value(), context.getTemplateId());
    contextJson.put(ITEMS.value(), new JsonArray().add(itemJson));
    return contextJson;
  }
}
