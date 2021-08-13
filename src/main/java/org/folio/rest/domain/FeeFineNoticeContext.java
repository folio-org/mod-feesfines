package org.folio.rest.domain;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.HoldingsRecord;
import org.folio.rest.jaxrs.model.Instance;
import org.folio.rest.jaxrs.model.Item;
import org.folio.rest.jaxrs.model.Location;
import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.jaxrs.model.User;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@With
public class FeeFineNoticeContext {

  private Owner owner;
  private Feefine feefine;
  private Account account;
  private Feefineaction charge;
  private Feefineaction action;
  private User user;
  private Item item;
  private Instance instance;
  private HoldingsRecord holdingsRecord;
  private Location effectiveLocation;
  private JsonObject logEventPayload;

  public String getTemplateId() {
    if (action != null) {
      return getActionNoticeTemplateId();
    }

    if (charge != null) {
      return getChargeNoticeTemplateId();
    }

    return null;
  }

  public Feefineaction getPrimaryAction() {
    return action != null ? action : charge;
  }

  private String getChargeNoticeTemplateId() {
    return defaultIfBlank(feefine.getChargeNoticeId(), owner.getDefaultChargeNoticeId());
  }

  private String getActionNoticeTemplateId() {
    return defaultIfBlank(feefine.getActionNoticeId(), owner.getDefaultActionNoticeId());
  }

  public String getUserId() {
    return getPrimaryAction().getUserId();
  }

  public String getAccountId() {
    return getPrimaryAction().getAccountId();
  }

}
