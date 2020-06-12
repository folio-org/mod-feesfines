package org.folio.rest.domain;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.Arrays;
import java.util.List;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
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

  public FeeFineNoticeContext() {
  }

  private FeeFineNoticeContext(Owner owner, Feefine feefine, Account account,
    Feefineaction feefineaction, User user) {

    this.owner = owner;
    this.feefine = feefine;
    this.account = account;
    this.feefineaction = feefineaction;
    this.user = user;
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

  public Owner getOwner() {
    return owner;
  }

  public FeeFineNoticeContext withOwner(Owner owner) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user);
  }

  public Feefine getFeefine() {
    return feefine;
  }

  public FeeFineNoticeContext withFeefine(Feefine feefine) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user);
  }

  public Account getAccount() {
    return account;
  }

  public FeeFineNoticeContext withAccount(Account account) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user);
  }

  public Feefineaction getFeefineaction() {
    return feefineaction;
  }

  public FeeFineNoticeContext withFeefineaction(Feefineaction feefineaction) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user);
  }

  public User getUser() {
    return user;
  }

  public FeeFineNoticeContext withUser(User user) {
    return new FeeFineNoticeContext(owner, feefine, account, feefineaction, user);
  }

}
