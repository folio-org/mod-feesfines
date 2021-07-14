package org.folio.rest.domain.logs;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.folio.rest.domain.logs.LogEventPayloadField.ACCOUNT_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.ACTION;
import static org.folio.rest.domain.logs.LogEventPayloadField.AMOUNT;
import static org.folio.rest.domain.logs.LogEventPayloadField.AUTOMATED;
import static org.folio.rest.domain.logs.LogEventPayloadField.BALANCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.COMMENTS;
import static org.folio.rest.domain.logs.LogEventPayloadField.DATE;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.FEE_FINE_OWNER;
import static org.folio.rest.domain.logs.LogEventPayloadField.HOLDINGS_RECORD_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.INSTANCE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEMS;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEM_BARCODE;
import static org.folio.rest.domain.logs.LogEventPayloadField.ITEM_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.LOAN_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.PAYMENT_METHOD;
import static org.folio.rest.domain.logs.LogEventPayloadField.SERVICE_POINT_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.SOURCE;
import static org.folio.rest.domain.logs.LogEventPayloadField.TEMPLATE_ID;
import static org.folio.rest.domain.logs.LogEventPayloadField.TRIGGERING_EVENT;
import static org.folio.rest.domain.logs.LogEventPayloadField.TYPE;
import static org.folio.rest.domain.logs.LogEventPayloadField.USER_BARCODE;
import static org.folio.rest.domain.logs.LogEventPayloadField.USER_ID;
import static org.folio.rest.utils.FeeFineActionHelper.isAction;
import static org.folio.rest.utils.FeeFineActionHelper.isCharge;
import static org.folio.rest.utils.JsonHelper.write;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.joda.time.DateTime;

public class LogEventPayloadHelper {
  private static final String STAFF_INFO_ONLY = "Staff info only";
  private static final String STAFF_INFO_ONLY_ADDED = "Staff information only added";
  private static final String BILLED = "Billed";

  private LogEventPayloadHelper() {
  }

  public static JsonObject buildNoticeLogEventPayload(FeeFineNoticeContext context) {
    JsonObject contextJson = new JsonObject();
    JsonObject itemJson = new JsonObject();

    ofNullable(context.getUser()).ifPresent(user -> {
      write(contextJson, USER_ID.value(), user.getId());
      write(contextJson, USER_BARCODE.value(), user.getBarcode());
    });

    ofNullable(nonNull(context.getAction()) ? context.getAction() : context.getCharge()).ifPresent(action -> {
      write(contextJson, SOURCE.value(), action.getSource());
      write(itemJson, SERVICE_POINT_ID.value(), action.getCreatedAt());
      write(itemJson, TRIGGERING_EVENT.value(), action.getTypeAction());
    });

    ofNullable(context.getAccount()).ifPresent(account -> {
      write(contextJson, ACCOUNT_ID.value(), account.getId());
      write(contextJson, FEE_FINE_ID.value(), account.getFeeFineId());
      write(itemJson, ITEM_BARCODE.value(), account.getBarcode());
      write(itemJson, LOAN_ID.value(), account.getLoanId());
    });

    ofNullable(context.getItem()).ifPresent(item -> {
      write(itemJson, ITEM_ID.value(), item.getId());
      write(itemJson, HOLDINGS_RECORD_ID.value(), item.getHoldingsRecordId());
    });

    ofNullable(context.getInstance()).ifPresent(instance -> write(itemJson, INSTANCE_ID.value(), instance.getId()));
    write(itemJson, TEMPLATE_ID.value(), context.getTemplateId());
    contextJson.put(ITEMS.value(), new JsonArray().add(itemJson));

    return contextJson;
  }

  public static Future<JsonObject> buildFeeFineLogEventPayload(Feefineaction action, Account account, Feefine feefine) {
    JsonObject json = new JsonObject();

    ofNullable(action).ifPresent(act -> {
      write(json, AMOUNT.value(), act.getAmountAction().toDouble());
      write(json, SOURCE.value(), act.getSource());
      write(json, SERVICE_POINT_ID.value(), act.getCreatedAt());
      write(json, DATE.value(), DateTime.now());
      if (isAction(act)) {
        write(json, ACTION.value(), act.getTypeAction());
        ofNullable(act.getBalance()).ifPresent(balance -> write(json, BALANCE.value(), balance.toDouble()));
        write(json, PAYMENT_METHOD.value(), act.getPaymentMethod());
        write(json, COMMENTS.value(), act.getComments());
      } else if (STAFF_INFO_ONLY.equalsIgnoreCase(act.getTypeAction())) {
        write(json, ACTION.value(), STAFF_INFO_ONLY_ADDED);
      } else if (isCharge(act)) {
        write(json, ACTION.value(), BILLED);
      }
    });

    ofNullable(account).ifPresent(acc -> {
      write(json, USER_ID.value(), acc.getUserId());
      write(json, ACCOUNT_ID.value(), acc.getId());
      write(json, FEE_FINE_ID.value(), acc.getFeeFineId());
      write(json, FEE_FINE_OWNER.value(), acc.getFeeFineOwner());
      write(json, ITEM_BARCODE.value(), acc.getBarcode());
      write(json, ITEM_ID.value(), acc.getItemId());
      write(json, LOAN_ID.value(), acc.getLoanId());
    });

    ofNullable(feefine).ifPresent(ff -> {
      write(json, TYPE.value(), ff.getFeeFineType());
      write(json, AUTOMATED.value(), ff.getAutomatic());
    });

    return succeededFuture(json);
  }
}
