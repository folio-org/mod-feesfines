package org.folio.rest.domain;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Owner;

public class FeeFineNoticeContext {

  private static final String PAID_FULLY = "Paid fully";
  private static final String PAID_PARTIALLY = "Paid partially";
  private static final String WAIVED_FULLY = "Waived fully";
  private static final String WAIVED_PARTIALLY = "Waived partially";

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
    if (isCharge()) {
      return getChargeNoticeTemplateId();
    }
    if (isFeeFineActon()) {
      return getActionNoticeTemplateId();
    }
    return null;
  }

  private boolean isCharge() {
    return feefineaction.getPaymentMethod() == null;
  }

  private boolean isFeeFineActon() {
    return feefineaction.getTypeAction().equals(PAID_FULLY) ||
      feefineaction.getTypeAction().equals(PAID_PARTIALLY) ||
      feefineaction.getTypeAction().equals(WAIVED_FULLY) ||
      feefineaction.getTypeAction().equals(WAIVED_PARTIALLY);
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
