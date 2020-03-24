package org.folio.rest.domain;

import java.util.Arrays;
import java.util.List;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Owner;

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

  public FeeFineNoticeContext() {
  }

  private FeeFineNoticeContext(Owner owner,
                               Feefine feefine,
                               Account account,
                               Feefineaction feefineaction) {
    this.owner = owner;
    this.feefine = feefine;
    this.account = account;
    this.feefineaction = feefineaction;
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
    return feefine.getChargeNoticeId() != null
      ? feefine.getChargeNoticeId()
      : owner.getDefaultChargeNoticeId();
  }

  private String getActionNoticeTemplateId() {
    return feefine.getActionNoticeId() != null
      ? feefine.getActionNoticeId()
      : owner.getDefaultActionNoticeId();
  }

  public String getUserId() {
    return feefineaction.getUserId();
  }

  public Owner getOwner() {
    return owner;
  }

  public FeeFineNoticeContext withOwner(Owner owner) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction);
  }

  public Feefine getFeefine() {
    return feefine;
  }

  public FeeFineNoticeContext withFeefine(Feefine feefine) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction);
  }

  public Account getAccount() {
    return account;
  }

  public FeeFineNoticeContext withAccount(Account account) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction);
  }

  public Feefineaction getFeefineaction() {
    return feefineaction;
  }

  public FeeFineNoticeContext withFeefineaction(Feefineaction feefineaction) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction);
  }
}
