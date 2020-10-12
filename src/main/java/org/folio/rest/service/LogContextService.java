package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.FeeFineActionHelper.isAction;
import static org.folio.rest.utils.FeeFineActionHelper.isCharge;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.folio.rest.client.UsersClient;
import org.folio.rest.domain.logs.FeeFineLogContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.User;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineRepository;
import org.joda.time.DateTime;

import java.util.Map;

public class LogContextService {
  public static final String STAFF_INFO_ONLY = "Staff info only";
  public static final String BILLED = "Billed";

  private final FeeFineRepository feeFineRepository;
  private final AccountRepository accountRepository;
  private final UsersClient usersClient;
  private FeeFineLogContext context;

  public LogContextService(Vertx vertx, Map<String, String> okapiHeaders) {
    PostgresClient pgClient = PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders);

    feeFineRepository = new FeeFineRepository(pgClient);
    accountRepository = new AccountRepository(pgClient);

    usersClient = new UsersClient(vertx, okapiHeaders);
  }

  public Future<FeeFineLogContext> createLogContext(Feefineaction action) {
    context = new FeeFineLogContext()
      .withDate(DateTime.now())
      .withType(action.getTypeAction())
      .withAmount(action.getAmountAction())
      .withSource(action.getSource())
      .withServicePointId(action.getCreatedAt());

    if (isAction(action)) {
      context = context.withAction(action.getTypeAction())
        .withBalance(action.getBalance())
        .withPaymentMethod(action.getPaymentMethod())
        .withComment(action.getComments());
    } else if (STAFF_INFO_ONLY.equalsIgnoreCase(action.getTypeAction())) {
      context = context.withAction(STAFF_INFO_ONLY);
    } else if (isCharge(action)) {
      context = context.withAction(BILLED);
    }

    return accountRepository.getAccountById(action.getAccountId())
      .compose(this::processAccount);
  }

  private Future<FeeFineLogContext> processAccount(Account account) {
    context = context.withUserId(account.getUserId())
      .withFeeFineId(account.getFeeFineId())
      .withFeeFineOwner(account.getFeeFineOwner())
      .withItemBarcode(account.getBarcode())
      .withLoanId(account.getLoanId());

    return usersClient.fetchUserById(account.getUserId())
      .compose(this::addUserData)
      .compose(c -> feeFineRepository.getById(account.getFeeFineId()))
      .compose(this::addFeeFineData);
  }

  private Future<FeeFineLogContext> addUserData(User user) {
    context = context.withUserBarcode(user.getBarcode());
    return succeededFuture(context);
  }

  private Future<FeeFineLogContext> addFeeFineData(Feefine feefine) {
    context = context.withAutomated(feefine.getAutomatic());
    return succeededFuture(context);
  }
}
