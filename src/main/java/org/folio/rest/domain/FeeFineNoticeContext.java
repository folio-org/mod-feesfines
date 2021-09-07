package org.folio.rest.domain;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.List;

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
  private List<Throwable> errors = new ArrayList<>();

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
    return ofNullable(feefine)
      .map(Feefine::getChargeNoticeId)
      .filter(not(String::isEmpty))
      .orElseGet(() -> ofNullable(owner)
        .map(Owner::getDefaultChargeNoticeId)
        .orElse(null));
  }

  private String getActionNoticeTemplateId() {
    return ofNullable(feefine)
      .map(Feefine::getActionNoticeId)
      .filter(not(String::isEmpty))
      .orElseGet(() -> ofNullable(owner)
        .map(Owner::getDefaultActionNoticeId)
        .orElse(null));
  }

  public String getUserId() {
    return getPrimaryAction().getUserId();
  }

  public String getAccountId() {
    return getPrimaryAction().getAccountId();
  }

}
