package org.folio.rest.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Owner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class FeeFineNoticeContextTest {
  private static final String DEFAULT_CHARGE_NOTICE_ID = "19a3a1c6-1717-4119-838f-298603ae2045";
  private static final String DEFAULT_ACTION_NOTICE_ID = "b0ff1736-262d-4c10-9a5c-b7d22ce0448c";
  private static final String FEEFINE_CHARGE_NOTICE_ID = "f361fc2d-dfbb-48a9-9f3f-a40c297411cf";
  private static final String FEEFINE_ACTION_NOTICE_ID = "500ebb63-0871-4fe7-a1da-fb3f1dbec5f2";

  private Owner owner;
  private Feefine feeFine;
  private Feefineaction feeFineAction;
  private String correctNoticeId;

  public FeeFineNoticeContextTest(Owner owner, Feefine feeFine, Feefineaction feeFineAction,
    String correctNoticeId) {
    this.owner = owner;
    this.feeFine = feeFine;
    this.feeFineAction = feeFineAction;
    this.correctNoticeId = correctNoticeId;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> testParameters() {
    Owner owner = new Owner()
      .withOwner("Test owner")
      .withDefaultChargeNoticeId(DEFAULT_CHARGE_NOTICE_ID)
      .withDefaultActionNoticeId(DEFAULT_ACTION_NOTICE_ID);

    Owner ownerWithoutDefaultNoticeIds = new Owner()
      .withOwner("Owner without notice ids")
      .withDefaultChargeNoticeId(null)
      .withDefaultActionNoticeId(null);

    Feefine feeFineWithNoticeIds = new Feefine()
      .withFeeFineType("Test charge")
      .withChargeNoticeId(FEEFINE_CHARGE_NOTICE_ID)
      .withActionNoticeId(FEEFINE_ACTION_NOTICE_ID);

    Feefine feeFineWithoutNoticeIds = new Feefine()
      .withFeeFineType("Test charge")
      .withChargeNoticeId(null)
      .withActionNoticeId(null);

    Feefineaction chargeFeeFineAction = new Feefineaction()
      .withPaymentMethod(null)
      .withTypeAction("Test charge");

    Feefineaction paidFullyFeeFineAction = new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction("Paid fully");

    Feefineaction paidPartiallyFeeFineAction = new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction("Paid partially");

    Feefineaction waivedFullyFeeFineAction = new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction("Waived fully");

    Feefineaction waivedPartiallyFeeFineAction = new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction("Waived partially");

    Feefineaction transferredFullyFeeFineAction = new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction("Transferred fully");

    Feefineaction transferredPartiallyFeeFineAction = new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction("Transferred partially");

    List<Object[]> parameters = new ArrayList<>();

    parameters.add(new Object[]
      {owner, feeFineWithoutNoticeIds, chargeFeeFineAction, DEFAULT_CHARGE_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithNoticeIds, chargeFeeFineAction, FEEFINE_CHARGE_NOTICE_ID});

    parameters.add(new Object[]
      {owner, feeFineWithoutNoticeIds, paidFullyFeeFineAction, DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithoutNoticeIds, paidPartiallyFeeFineAction, DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithNoticeIds, paidFullyFeeFineAction, FEEFINE_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithNoticeIds, paidPartiallyFeeFineAction, FEEFINE_ACTION_NOTICE_ID});

    parameters.add(new Object[]
      {owner, feeFineWithoutNoticeIds, waivedFullyFeeFineAction, DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithoutNoticeIds, waivedPartiallyFeeFineAction, DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithNoticeIds, waivedFullyFeeFineAction, FEEFINE_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithNoticeIds, waivedPartiallyFeeFineAction, FEEFINE_ACTION_NOTICE_ID});

    parameters.add(new Object[]
      {owner, feeFineWithoutNoticeIds, transferredFullyFeeFineAction, DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithoutNoticeIds, transferredPartiallyFeeFineAction, DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithNoticeIds, transferredFullyFeeFineAction, FEEFINE_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {owner, feeFineWithNoticeIds, transferredPartiallyFeeFineAction, FEEFINE_ACTION_NOTICE_ID});

    parameters.add(new Object[]
      {ownerWithoutDefaultNoticeIds, feeFineWithoutNoticeIds, chargeFeeFineAction, null});

    return parameters;
  }

  @Test
  public void getTemplateIdReturnsCorrectId() {
    FeeFineNoticeContext feeFineNoticeContext = new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(feeFine)
      .withFeefineaction(feeFineAction);

    String templateId = feeFineNoticeContext.getTemplateId();
    if (correctNoticeId == null) {
      assertNull(templateId);
    }
    else {
      assertEquals(correctNoticeId, templateId);
    }
  }
}
