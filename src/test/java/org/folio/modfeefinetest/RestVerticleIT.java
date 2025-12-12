package org.folio.modfeefinetest;

import java.net.HttpURLConnection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

@RunWith(VertxUnitRunner.class)
public class RestVerticleIT {

    private static final String SUPPORTED_CONTENT_TYPE_JSON_DEF = "application/json";

   private static final String MANUAL_BLOCK = "{\"type\": \"Manual\",\"desc\": \"Show not expiration!\",\"borrowing\": true,\"renewals\": true,\"requests\": true,\"userId\": \"9d68864b-ee65-4ab0-9d2d-1677f8503f64\"}";

    private static Vertx vertx;
    static int port;

    @Rule
    public Timeout rule = Timeout.seconds(15);

    public static void initDatabase(TestContext context) {
        PostgresClient.setPostgresTester(new PostgresTesterContainer());

        String sql = "drop schema if exists diku_mod_feesfines cascade;\n"
                + "drop role if exists diku_mod_feesfines;\n";
        Async async = context.async();
        PostgresClient.getInstance(vertx).runSqlFile(sql)
          .onFailure(context::fail)
          .onSuccess(v -> async.complete());

        async.await();
    }

    @BeforeClass
    public static void setup(TestContext context) throws SQLException {
      vertx = Vertx.vertx();

      initDatabase(context);

      Async async = context.async();
      port = NetworkUtils.nextFreePort();
      TenantClient tenantClient = new TenantClient("localhost", port, "diku", "diku");
      DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
      vertx.deployVerticle(RestVerticle.class.getName(), options)
        .onSuccess(res -> {
          try {
            tenantClient.postTenant(new TenantAttributes(), res2 -> async.complete());
          } catch (Exception e) {
            context.fail(e);
          }
        });
      async.await();
    }

    @AfterClass
    public static void teardown(TestContext context) {
      Async async = context.async();
      vertx.close()
        .onSuccess(ignored -> {
          PostgresClient.stopPostgresTester();
          async.complete();
        });
    }

    @Test
    public void testManualBlock(TestContext context) {
        try {
            String manualBlockURL = "http://localhost:" + port + "/manualblocks";

            /**
             * add a manualblock - should return 201
             */
            CompletableFuture<Response> addManualBlockCF = new CompletableFuture();
            String addManualBlockURL = manualBlockURL;
            send(addManualBlockURL, HttpMethod.POST, MANUAL_BLOCK,
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, new HTTPResponseHandler(addManualBlockCF));
            Response addManualBlockResponse = addManualBlockCF.get(5, TimeUnit.SECONDS);
            context.assertEquals(addManualBlockResponse.code, HttpURLConnection.HTTP_CREATED);
            String manualBlockId = addManualBlockResponse.body.getString("id");

            /**
             * get all manualBlocks in manualBlocks table - should return 200
             */
            CompletableFuture<Response> getAllManualBlocksCF = new CompletableFuture();
            String getAllManualBlocksURL = manualBlockURL;
            send(getAllManualBlocksURL, HttpMethod.GET, null,
                    SUPPORTED_CONTENT_TYPE_JSON_DEF, new HTTPResponseHandler(getAllManualBlocksCF));
            Response getAllManualBlocksResponse = getAllManualBlocksCF.get(5, TimeUnit.SECONDS);
            context.assertEquals(getAllManualBlocksResponse.code, HttpURLConnection.HTTP_OK);
            context.assertTrue(isSizeMatch(getAllManualBlocksResponse, 1));
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            Logger.getLogger(RestVerticleIT.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void send(String url, HttpMethod method, String content,
            String contentType, Handler<HttpResponse<Buffer>> handler) {

      WebClient client = WebClient.create(vertx);
      var request = client.requestAbs(Objects.requireNonNullElse(method, HttpMethod.PUT), url);
      if (content == null) {
        content = "";
      }
      Buffer buffer = Buffer.buffer(content);
      request.putHeader("Authorization", "diku");
      request.putHeader("X-Okapi-Tenant", "diku");
      request.putHeader("accept", "application/json,text/plain");
      request.putHeader("content-type", "application/json");
      request.sendBuffer(buffer)
        .onSuccess(handler);
    }

    class HTTPResponseHandler implements Handler<HttpResponse<Buffer>> {

        CompletableFuture<Response> event;

        public HTTPResponseHandler(CompletableFuture<Response> cf) {
            event = cf;
        }

      @Override
      public void handle(HttpResponse<Buffer> response) {
        var r = new Response();
        r.code = response.statusCode();
        r.body = response.bodyAsJsonObject();
        event.complete(r);
      }
    }

    class HTTPNoBodyResponseHandler implements Handler<HttpClientResponse> {

        CompletableFuture<Response> event;

        public HTTPNoBodyResponseHandler(CompletableFuture<Response> cf) {
            event = cf;
        }

        @Override
        public void handle(HttpClientResponse hcr) {
            Response r = new Response();
            r.code = hcr.statusCode();
            event.complete(r);
        }
    }

    class Response {

        int code;
        JsonObject body;
    }

    private boolean isSizeMatch(Response r, int size) {
        return r.body.getInteger("totalRecords") == size;
    }

    private static JsonObject createFeeFineAction(String id, String accountId, String userId) {

        JsonObject feeFineAction = new JsonObject();
        if (id != null) {
            feeFineAction.put("id", id);
        } else {
            id = UUID.randomUUID().toString();
            feeFineAction.put("id", id);
        }
        feeFineAction.put("typeAction", "Payment-ckeck")
                .put("amountAction", "15.00")
                .put("balance", "10.00")
                .put("transactionInformation", "Department ENG-345")
                .put("comments", "This a comment")
                .put("createdAt", "Main Library")
                .put("source", "Doe,Jane")
                .put("paymentMethod", "Check")
                .put("accountId", accountId)
                .put("userId", userId);

        return feeFineAction;
    }

    private static JsonObject createAccount(String id, String ownerId, String feeFineId, String feeFineType, String materialTypeId, String itemId, String userId) {

        JsonObject account = new JsonObject();
        if (id != null) {
            account.put("id", id);
        } else {
            id = UUID.randomUUID().toString();
            account.put("id", id);
        }
        account.put("amount", "15.00")
                .put("remaining", "15.00")
                .put("feeFineType", feeFineType)
                .put("feeFineOwner", "Main Admin")
                .put("status", new JsonObject()
                        .put("name", "Open"))
                .put("paymentStatus", new JsonObject()
                        .put("name", "Outstanding"))
                .put("ownerId", ownerId)
                .put("feeFineId", feeFineId)
                .put("materialTypeId", materialTypeId)
                .put("itemId", itemId)
                .put("userId", userId);

        return account;
    }

    private static JsonObject createFeeFine(String id, String ownerId, String typeFeeFine) {

        JsonObject feefine = new JsonObject();
        if (id != null) {
            feefine.put("id", id);
        } else {
            id = UUID.randomUUID().toString();
            feefine.put("id", id);
        }
        feefine.put("feeFineType", typeFeeFine)
                .put("defaultAmount", "10.00")
                .put("ownerId", ownerId);

        return feefine;
    }

    private static JsonObject createOwner(String id, String owner, String desc) {

        JsonObject ownerJO = new JsonObject();
        if (id != null) {
            ownerJO.put("id", id);
        } else {
            id = UUID.randomUUID().toString();
            ownerJO.put("id", id);
        }
        ownerJO.put("owner", owner)
                .put("desc", desc);
        return ownerJO;
    }
}
