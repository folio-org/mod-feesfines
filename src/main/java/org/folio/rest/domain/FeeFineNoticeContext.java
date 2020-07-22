package org.folio.rest.domain;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.Arrays;
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

public class FeeFineNoticeContext {
  private static final List<String> FEE_FINE_ACTION_TYPES = Arrays.asList(
    "Paid fully", "Paid partially",
    "Waived fully", "Waived partially",
    "Transferred fully", "Transferred partially",
    "Refunded fully", "Refunded partially",
    "Cancelled as error");

  private Owner owner;
  private Feefine feefine;
  private Account account;
  private Feefineaction feefineaction;
  private User user;
  private Item item;
  private Instance instance;
  private HoldingsRecord holdingsRecord;
  private Location effectiveLocation;

  public FeeFineNoticeContext() {
  }

  public FeeFineNoticeContext(Owner owner, Feefine feefine, Account account,
    Feefineaction feefineaction, User user, Item item, Instance instance,
    HoldingsRecord holdingsRecord, Location effectiveLocation) {

    this.owner = owner;
    this.feefine = feefine;
    this.account = account;
    this.feefineaction = feefineaction;
    this.user = user;
    this.item = item;
    this.instance = instance;
    this.holdingsRecord = holdingsRecord;
    this.effectiveLocation = effectiveLocation;
  }

  public String getTemplateId() {
    if (isFeeFineActon()) {
      return getActionNoticeTemplateId();
    }
    else if (isCharge()) {
      return getChargeNoticeTemplateId();
    }
    return null;
  }

  private boolean isCharge() {
    return feefineaction.getPaymentMethod() == null;
  }

  private boolean isFeeFineActon() {
    return FEE_FINE_ACTION_TYPES.contains(feefineaction.getTypeAction());
  }

  private String getChargeNoticeTemplateId() {
    return defaultIfBlank(feefine.getChargeNoticeId(), owner.getDefaultChargeNoticeId());
  }

  private String getActionNoticeTemplateId() {
    return defaultIfBlank(feefine.getActionNoticeId(), owner.getDefaultActionNoticeId());
  }

  public String getUserId() {
    return feefineaction.getUserId();
  }

  public FeeFineNoticeContext withOwner(Owner owner) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withFeefine(Feefine feefine) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withAccount(Account account) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withFeefineaction(Feefineaction feefineaction) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withUser(User user) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withItem(Item item) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withInstance(Instance instance) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withHoldingsRecord(HoldingsRecord holdingsRecord) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
      instance, holdingsRecord, effectiveLocation);
  }

  public FeeFineNoticeContext withEffectiveLocation(Location effectiveLocation) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user, item,
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

  public Feefineaction getFeefineaction() {
    return feefineaction;
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
