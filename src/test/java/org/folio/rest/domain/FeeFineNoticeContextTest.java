package org.folio.rest.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Owner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FeeFineNoticeContextTest {

  private static final String DEFAULT_CHARGE_NOTICE_ID = "19a3a1c6-1717-4119-838f-298603ae2045";
  private static final String DEFAULT_ACTION_NOTICE_ID = "b0ff1736-262d-4c10-9a5c-b7d22ce0448c";

  private static final String FEEFINE_CHARGE_NOTICE_ID = "f361fc2d-dfbb-48a9-9f3f-a40c297411cf";
  private static final String FEEFINE_ACTION_NOTICE_ID = "500ebb63-0871-4fe7-a1da-fb3f1dbec5f2";

  private static final String PAID_FULLY_ACTION_TYPE = "Paid fully";
  private static final String PAID_PARTIALLY_ACTION_TYPE = "Paid partially";

  private static Owner owner;
  private static Owner ownerWithoutDefaultNoticeIds;

  private static Feefine feeFineWithNoticeIds;
  private static Feefine defaultNoticeIdsFeeFine;

  private static Feefineaction chargeFeeFineAction;
  private static Feefineaction paidPartiallyFeeFineAction;
  private static Feefineaction paidFullyFeeFineAction;


  @BeforeAll
  static void beforeAll() {
    owner = new Owner()
      .withOwner("Test owner")
      .withDefaultChargeNoticeId(DEFAULT_CHARGE_NOTICE_ID)
      .withDefaultActionNoticeId(DEFAULT_ACTION_NOTICE_ID);

    ownerWithoutDefaultNoticeIds = new Owner()
      .withOwner("Owner without notice ids")
      .withDefaultChargeNoticeId(null)
      .withDefaultActionNoticeId(null);

    feeFineWithNoticeIds = new Feefine()
      .withFeeFineType("Test charge")
      .withChargeNoticeId(FEEFINE_CHARGE_NOTICE_ID)
      .withActionNoticeId(FEEFINE_ACTION_NOTICE_ID);

    defaultNoticeIdsFeeFine = new Feefine()
      .withFeeFineType("Test charge")
      .withChargeNoticeId(null)
      .withActionNoticeId(null);

    chargeFeeFineAction = new Feefineaction()
      .withPaymentMethod(null)
      .withTypeAction("Test charge");

    paidPartiallyFeeFineAction = new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction(PAID_PARTIALLY_ACTION_TYPE);

    paidFullyFeeFineAction = new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction(PAID_FULLY_ACTION_TYPE);
  }

  @Test
  void getTemplateIdReturnsDefaultChargeNoticeId() {
    FeeFineNoticeContext feeFineNoticeContext = new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(defaultNoticeIdsFeeFine)
      .withFeefineaction(chargeFeeFineAction);

    String templateId = feeFineNoticeContext.getTemplateId();
    assertEquals(DEFAULT_CHARGE_NOTICE_ID, templateId);
  }

  @Test
  void getTemplateIdReturnsFeeFineChargeNoticeId() {
    FeeFineNoticeContext feeFineNoticeContext = new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(feeFineWithNoticeIds)
      .withFeefineaction(chargeFeeFineAction);

    String templateId = feeFineNoticeContext.getTemplateId();
    assertEquals(FEEFINE_CHARGE_NOTICE_ID, templateId);
  }

  @Test
  void getTemplateIdReturnsDefaultActionNoticeIdWhenPaidPartially() {
    FeeFineNoticeContext feeFineNoticeContext = new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(defaultNoticeIdsFeeFine)
      .withFeefineaction(paidPartiallyFeeFineAction);

    String templateId = feeFineNoticeContext.getTemplateId();
    assertEquals(DEFAULT_ACTION_NOTICE_ID, templateId);
  }


  @Test
  void getTemplateIdReturnsDefaultActionNoticeIdWhenPaidFully() {
    FeeFineNoticeContext feeFineNoticeContext = new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(defaultNoticeIdsFeeFine)
      .withFeefineaction(paidFullyFeeFineAction);

    String templateId = feeFineNoticeContext.getTemplateId();
    assertEquals(DEFAULT_ACTION_NOTICE_ID, templateId);
  }

  @Test
  void getTemplateIdReturnsFeeFineActionNoticeIdWhenPaidPartially() {
    FeeFineNoticeContext feeFineNoticeContext = new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(feeFineWithNoticeIds)
      .withFeefineaction(paidPartiallyFeeFineAction);

    String templateId = feeFineNoticeContext.getTemplateId();
    assertEquals(FEEFINE_ACTION_NOTICE_ID, templateId);
  }


  @Test
  void getTemplateIdReturnsFeeFineActionNoticeIdWhenPaidFully() {
    FeeFineNoticeContext feeFineNoticeContext = new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(feeFineWithNoticeIds)
      .withFeefineaction(paidFullyFeeFineAction);

    String templateId = feeFineNoticeContext.getTemplateId();
    assertEquals(FEEFINE_ACTION_NOTICE_ID, templateId);
  }

  @Test
  void getTemplateIdReturnsNullWhenNoticeIdIsNotSetup() {
    FeeFineNoticeContext feeFineNoticeContext = new FeeFineNoticeContext()
      .withOwner(ownerWithoutDefaultNoticeIds)
      .withFeefine(defaultNoticeIdsFeeFine)
      .withFeefineaction(chargeFeeFineAction);

    String templateId = feeFineNoticeContext.getTemplateId();
    assertNull(templateId);
  }
}
