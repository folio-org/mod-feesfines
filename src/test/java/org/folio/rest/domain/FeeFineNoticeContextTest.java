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

  private final FeeFineNoticeContext context;
  private final String expectedTemplateId;

  public FeeFineNoticeContextTest(FeeFineNoticeContext context, String expectedTemplateId) {
    this.context = context;
    this.expectedTemplateId = expectedTemplateId;
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

    Feefine feeFineWithNullNoticeIds = new Feefine()
      .withFeeFineType("Test charge")
      .withChargeNoticeId(null)
      .withActionNoticeId(null);

    Feefine feeFineWithEmptyNoticeIds = new Feefine()
      .withFeeFineType("Test charge")
      .withChargeNoticeId("")
      .withActionNoticeId("");

    Feefineaction charge = new Feefineaction()
      .withPaymentMethod(null)
      .withTypeAction("Test charge");

    Feefineaction paidFully = createActionWithType("Paid fully");
    Feefineaction paidPartially = createActionWithType("Paid partially");
    Feefineaction waivedFully = createActionWithType("Waived fully");
    Feefineaction waivedPartially = createActionWithType("Waived partially");
    Feefineaction transferredFully = createActionWithType("Transferred fully");
    Feefineaction transferredPartially = createActionWithType("Transferred partially");
    Feefineaction refundedFully = createActionWithType("Refunded fully");
    Feefineaction refundedPartially = createActionWithType("Refunded partially");
    Feefineaction cancelledAsError = createActionWithType("Cancelled as error");

    List<Object[]> parameters = new ArrayList<>();

    parameters.add(new Object[]
      {createChargeContext(owner, feeFineWithNullNoticeIds, charge), DEFAULT_CHARGE_NOTICE_ID});
    parameters.add(new Object[]
      {createChargeContext(owner, feeFineWithEmptyNoticeIds, charge), DEFAULT_CHARGE_NOTICE_ID});
    parameters.add(new Object[]
      {createChargeContext(owner, feeFineWithNoticeIds, charge), FEEFINE_CHARGE_NOTICE_ID});

    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, paidFully), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, paidPartially), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, paidFully), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, paidPartially), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, paidFully), FEEFINE_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, paidPartially), FEEFINE_ACTION_NOTICE_ID});

    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, waivedFully), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, waivedPartially), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, waivedFully), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, waivedPartially), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, waivedFully), FEEFINE_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, waivedPartially), FEEFINE_ACTION_NOTICE_ID});

    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, transferredFully), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, transferredPartially), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, transferredFully), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, transferredPartially), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, transferredFully), FEEFINE_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, transferredPartially), FEEFINE_ACTION_NOTICE_ID});

    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, refundedFully), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, refundedPartially), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, refundedFully), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, refundedPartially), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, refundedFully), FEEFINE_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, refundedPartially), FEEFINE_ACTION_NOTICE_ID});

    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNullNoticeIds, cancelledAsError), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithEmptyNoticeIds, cancelledAsError), DEFAULT_ACTION_NOTICE_ID});
    parameters.add(new Object[]
      {createActionContext(owner, feeFineWithNoticeIds, cancelledAsError), FEEFINE_ACTION_NOTICE_ID});

    parameters.add(new Object[]
      {createChargeContext(ownerWithoutDefaultNoticeIds, feeFineWithNullNoticeIds, charge), null});
    parameters.add(new Object[]
      {createChargeContext(ownerWithoutDefaultNoticeIds, feeFineWithEmptyNoticeIds, charge), null});

    return parameters;
  }

  @Test
  public void getTemplateIdReturnsCorrectId() {
    final String templateId = context.getTemplateId();

    if (expectedTemplateId == null) {
      assertNull(templateId);
    }
    else {
      assertEquals(expectedTemplateId, templateId);
    }
  }

  private static Feefineaction createActionWithType(String type) {
    return new Feefineaction()
      .withPaymentMethod("Test payment method")
      .withTypeAction(type);
  }

  private static FeeFineNoticeContext createChargeContext(Owner owner, Feefine feeFine,
    Feefineaction charge) {

    return new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(feeFine)
      .withCharge(charge);
  }

  private static FeeFineNoticeContext createActionContext(Owner owner, Feefine feeFine,
    Feefineaction action) {

    return new FeeFineNoticeContext()
      .withOwner(owner)
      .withFeefine(feeFine)
      .withAction(action);
  }

}
