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

  public FeeFineNoticeContext() {
  }

  public FeeFineNoticeContext(Owner owner, Feefine feefine, Account account,
    Feefineaction charge, Feefineaction action, User user, Item item, Instance instance,
    HoldingsRecord holdingsRecord, Location effectiveLocation) {

    this.owner = owner;
    this.feefine = feefine;
    this.account = account;
    this.charge = charge;
    this.action = action;
    this.user = user;
    this.item = item;
    this.instance = instance;
    this.holdingsRecord = holdingsRecord;
    this.effectiveLocation = effectiveLocation;
  }

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

  public FeeFineNoticeContext withOwner(Owner owner) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withFeefine(Feefine feefine) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withAccount(Account account) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withCharge(Feefineaction charge) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withAction(Feefineaction action) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withUser(User user) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withItem(Item item) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withInstance(Instance instance) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withHoldingsRecord(HoldingsRecord holdingsRecord) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withEffectiveLocation(Location effectiveLocation) {
    return new FeeFineNoticeContext(owner, feefine, account, charge, action, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public Owner getOwner() {
    return owner;
  }

  public Feefine getFeefine() {
    return feefine;
  }

  public Account getAccount() {
    return account;
  }

  public Feefineaction getCharge() {
    return charge;
  }

  public Feefineaction getAction() {
    return action;
  }

  public User getUser() {
    return user;
  }

  public Item getItem() {
    return item;
  }

  public Instance getInstance() {
    return instance;
  }

  public HoldingsRecord getHoldingsRecord() {
    return holdingsRecord;
  }

  public Location getEffectiveLocation() {
    return effectiveLocation;
  }
}
