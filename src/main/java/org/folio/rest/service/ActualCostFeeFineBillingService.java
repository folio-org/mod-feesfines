package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.rest.jaxrs.model.ActualCostRecord.Status.BILLED;
import static org.folio.rest.utils.FeeFineActionHelper.buildComments;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActualCostFeeFineBill;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.rest.jaxrs.model.ActualCostRecordItem;
import org.folio.rest.jaxrs.model.EffectiveCallNumberComponents;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.PaymentStatus;
import org.folio.rest.jaxrs.model.Status;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.utils.MetadataHelper;
import org.folio.rest.utils.PatronHelper;

import io.vertx.core.Context;
import io.vertx.core.Future;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

public class ActualCostFeeFineBillingService extends ActualCostFeeFineService {
  private static final Logger log = LogManager.getLogger(ActualCostFeeFineBillingService.class);
  private static final String USER_ID_HEADER = "X-Okapi-User-Id";
  private static final String NEW_ACCOUNT_STATUS_NAME = "Open";
  private static final String NEW_ACCOUNT_TRANSACTION_INFO = "-";

  private final Map<String, String> okapiHeaders;
  private final FeeFineChargeService feeFineChargeService;
  private final UsersClient usersClient;

  public ActualCostFeeFineBillingService(Context context, Map<String, String> okapiHeaders) {
    super(context, okapiHeaders);
    this.okapiHeaders = new CaseInsensitiveMap<>(okapiHeaders);
    this.feeFineChargeService = new FeeFineChargeService(context, okapiHeaders);
    this.usersClient = new UsersClient(context.owner(), okapiHeaders);
  }

  public Future<ActualCostRecord> bill(ActualCostFeeFineBill request) {
    final String recordId = request.getActualCostRecordId();

    log.info("Billing fee/fine for actual cost record {}", recordId);

    return succeededFuture(new BillingContext(request))
      .compose(this::fetchActualCostRecord)
      .compose(this::validateRequest)
      .compose(this::fetchLoan)
      .compose(this::fetchUser)
      .map(this::buildAccount)
      .map(this::buildAction)
      .compose(this::chargeFeeFine)
      .compose(this::updateRecord)
      .onSuccess(r -> log.info("Fee/fine for actual cost record {} billed successfully", recordId))
      .onFailure(t -> log.error("Failed to bill fee/fine for actual cost record " + recordId, t));
  }

  private Future<BillingContext> fetchActualCostRecord(BillingContext context) {
    return fetchActualCostRecord(context.getBillingRequest().getActualCostRecordId())
      .map(context::withActualCostRecord);
  }

  private Future<BillingContext> validateRequest(BillingContext context) {
    return failIfActualCostRecordIsNotOpen(context.getActualCostRecord())
      .map(context);
  }

  private Future<BillingContext> fetchLoan(BillingContext context) {
    return circulationStorageClient.getLoanById(context.getActualCostRecord().getLoan().getId())
      .map(context::withLoan);
  }

  private Future<BillingContext> fetchUser(BillingContext context) {
    String userId = okapiHeaders.get(USER_ID_HEADER);

    if (isBlank(userId)) {
      log.warn("Failed to find userId in request headers, fee/fine action will not have a 'source'");
      return succeededFuture(context);
    }

    return usersClient.fetchUserById(userId)
      .map(context::withSource);
  }

  private BillingContext buildAccount(BillingContext context) {
    ActualCostFeeFineBill request = context.getBillingRequest();
    ActualCostRecord actualCostRecord = context.getActualCostRecord();
    Loan loan = context.getLoan();

    String callNumber = ofNullable(actualCostRecord.getItem())
      .map(ActualCostRecordItem::getEffectiveCallNumberComponents)
      .map(EffectiveCallNumberComponents::getCallNumber)
      .orElse(null);

    Date returnDate = ofNullable(loan.getReturnDate())
      .map(ZonedDateTime::parse)
      .map(ZonedDateTime::toInstant)
      .map(Date::from)
      .orElse(null);

    Account account = new Account()
      .withId(UUID.randomUUID().toString())
      .withAmount(request.getAmount())
      .withRemaining(request.getAmount())
      .withFeeFineId(actualCostRecord.getFeeFine().getTypeId())
      .withFeeFineType(actualCostRecord.getFeeFine().getType())
      .withOwnerId(actualCostRecord.getFeeFine().getOwnerId())
      .withFeeFineOwner(actualCostRecord.getFeeFine().getOwner())
      .withTitle(actualCostRecord.getInstance().getTitle())
      .withBarcode(actualCostRecord.getItem().getBarcode())
      .withCallNumber(callNumber)
      .withLocation(actualCostRecord.getItem().getEffectiveLocation())
      .withMaterialTypeId(actualCostRecord.getItem().getMaterialTypeId())
      .withMaterialType(actualCostRecord.getItem().getMaterialType())
      .withLoanId(actualCostRecord.getLoan().getId())
      .withUserId(actualCostRecord.getUser().getId())
      .withItemId(actualCostRecord.getItem().getId())
      .withHoldingsRecordId(actualCostRecord.getItem().getHoldingsRecordId())
      .withInstanceId(actualCostRecord.getInstance().getId())
      .withDueDate(loan.getDueDate())
      .withReturnedDate(returnDate)
      .withPaymentStatus(new PaymentStatus().withName(PaymentStatus.Name.OUTSTANDING))
      .withStatus(new Status().withName(NEW_ACCOUNT_STATUS_NAME))
      .withContributors(actualCostRecord.getInstance().getContributors())
      .withLoanPolicyId(loan.getLoanPolicyId())
      .withOverdueFinePolicyId(loan.getOverdueFinePolicyId())
      .withLostItemFeePolicyId(loan.getLostItemPolicyId());

    // we are creating the account internally (not via API) so we need to populate metadata manually
    MetadataHelper.populateMetadata(account, okapiHeaders);

    return context.withAccount(account);
  }

  private BillingContext buildAction(BillingContext context) {
    Account account = context.getAccount();
    ActualCostRecord actualCostRecord = context.getActualCostRecord();
    ActualCostFeeFineBill billingRequest = context.getBillingRequest();

    String comments = buildComments(
      billingRequest.getAdditionalInfoForStaff(),
      billingRequest.getAdditionalInfoForPatron());

    String source = ofNullable(context.getSource())
      .map(PatronHelper::getUserName)
      .orElse(null);

    Feefineaction action = new Feefineaction()
      .withId(UUID.randomUUID().toString())
      .withUserId(account.getUserId())
      .withAccountId(account.getId())
      .withSource(source)
      .withCreatedAt(billingRequest.getServicePointId())
      .withTransactionInformation(NEW_ACCOUNT_TRANSACTION_INFO)
      .withBalance(account.getAmount())
      .withAmountAction(account.getAmount())
      .withNotify(false)
      .withTypeAction(actualCostRecord.getFeeFine().getType())
      .withDateAction(new Date())
      .withComments(comments);

    return context.withAction(action);
  }

  private Future<BillingContext> chargeFeeFine(BillingContext context) {
    return feeFineChargeService.chargeFeeFine(context.getAccount(), context.getAction())
      .map(context);
  }

  private Future<ActualCostRecord> updateRecord(BillingContext context) {
    ActualCostFeeFineBill billingRequest = context.getBillingRequest();
    Account account = context.getAccount();
    ActualCostRecord actualCostRecord = context.getActualCostRecord();

    actualCostRecord
      .withStatus(BILLED)
      .withAdditionalInfoForStaff(billingRequest.getAdditionalInfoForStaff())
      .withAdditionalInfoForPatron(billingRequest.getAdditionalInfoForPatron())
      .getFeeFine()
      .withAccountId(account.getId())
      .withBilledAmount(account.getAmount());

    return updateActualCostRecord(actualCostRecord);
  }

  @Getter
  @With
  @AllArgsConstructor
  @RequiredArgsConstructor
  private static class BillingContext {
    private final ActualCostFeeFineBill billingRequest;
    private ActualCostRecord actualCostRecord;
    private Loan loan;
    private User source;
    private Account account;
    private Feefineaction action;
  }

}
