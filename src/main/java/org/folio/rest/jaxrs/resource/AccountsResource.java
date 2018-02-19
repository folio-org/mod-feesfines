
package org.folio.rest.jaxrs.resource;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import io.vertx.core.Context;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.AccountdataCollection;


/**
 * Collection of account items.
 * 
 */
@Path("accounts")
public interface AccountsResource {


    /**
     * Return a list of accounts
     * 
     * @param offset
     *     Skip over a number of elements by specifying an offset value for the query e.g. 0
     * @param query
     *     A query expressed as a CQL string
     *     (see [dev.folio.org/doc/glossary#cql](http://dev.folio.org/doc/glossary#cql))
     *     using valid searchable fields.
     *     The first example below shows the general form of a full CQL query,
     *     but those fields might not be relevant in this context.
     *     
     *     with valid searchable fields
     *      e.g. (username="ab*" or personal.firstName="ab*" or personal.lastName="ab*") and active="true" sortby personal.lastName personal.firstName barcode
     *     
     *     active=true
     *     
     * @param limit
     *     Limit the number of elements returned in the response e.g. 10
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param orderBy
     *     Order by field: field A, field B
     *     
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     * @param order
     *     Order
     */
    @GET
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void getAccounts(
        @QueryParam("query")
        String query,
        @QueryParam("orderBy")
        String orderBy,
        @QueryParam("order")
        @DefaultValue("desc")
        AccountsResource.Order order,
        @QueryParam("offset")
        @DefaultValue("0")
        @Min(0L)
        @Max(1000L)
        int offset,
        @QueryParam("limit")
        @DefaultValue("10")
        @Min(1L)
        @Max(100L)
        int limit,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Create a account
     * 
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     * @param entity
     *      e.g. {
     *       "amount": 15.0,
     *       "remaining": 15.0,
     *       "accountTransaction": 1,
     *       "notes": "This is a note account",
     *       "dateCreated": "2018-01-31T00:00:01Z",
     *       "dateUpdated": "2018-01-31T00:00:01Z",
     *       "status": {
     *         "name": "Open"
     *       },
     *       "paymentStatus": {
     *         "name": "Paid Partially"
     *       },
     *       "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
     *       "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
     *       "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
     *       "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
     *       "feeFineId": "3a0d8d79-7f28-4795-aa92-2f5d67140e6b",
     *       "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
     *       "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
     *     }
     *     
     *     
     */
    @POST
    @Consumes("application/json")
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void postAccounts(
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Account entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Get a single account
     * 
     * @param accountId
     *     
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     */
    @GET
    @Path("{accountId}")
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void getAccountsByAccountId(
        @PathParam("accountId")
        @NotNull
        String accountId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Delete account item with given {accountId}
     * 
     * 
     * @param accountId
     *     
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     */
    @DELETE
    @Path("{accountId}")
    @Produces({
        "text/plain"
    })
    @Validate
    void deleteAccountsByAccountId(
        @PathParam("accountId")
        @NotNull
        String accountId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Update account item with given {accountId}
     * 
     * 
     * @param accountId
     *     
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     * @param entity
     *      e.g. {
     *       "amount": 15.0,
     *       "remaining": 15.0,
     *       "accountTransaction": 1,
     *       "notes": "This is a note account",
     *       "dateCreated": "2018-01-31T00:00:01Z",
     *       "dateUpdated": "2018-01-31T00:00:01Z",
     *       "status": {
     *         "name": "Open"
     *       },
     *       "paymentStatus": {
     *         "name": "Paid Partially"
     *       },
     *       "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
     *       "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
     *       "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
     *       "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
     *       "feeFineId": "3a0d8d79-7f28-4795-aa92-2f5d67140e6b",
     *       "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
     *       "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
     *     }
     *     
     *     
     */
    @PUT
    @Path("{accountId}")
    @Consumes("application/json")
    @Produces({
        "text/plain"
    })
    @Validate
    void putAccountsByAccountId(
        @PathParam("accountId")
        @NotNull
        String accountId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Account entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    public class DeleteAccountsByAccountIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private DeleteAccountsByAccountIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item deleted successfully
         * 
         */
        public static AccountsResource.DeleteAccountsByAccountIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new AccountsResource.DeleteAccountsByAccountIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "account not found"
         * 
         * 
         * @param entity
         *     "account not found"
         *     
         */
        public static AccountsResource.DeleteAccountsByAccountIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.DeleteAccountsByAccountIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to delete account -- constraint violation"
         * 
         * 
         * @param entity
         *     "unable to delete account -- constraint violation"
         *     
         */
        public static AccountsResource.DeleteAccountsByAccountIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.DeleteAccountsByAccountIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static AccountsResource.DeleteAccountsByAccountIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.DeleteAccountsByAccountIdResponse(responseBuilder.build());
        }

    }

    public class GetAccountsByAccountIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetAccountsByAccountIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns item with a given ID e.g. {
         *   "amount": 15.0,
         *   "remaining": 15.0,
         *   "accountTransaction": 1,
         *   "notes": "This is a note account",
         *   "dateCreated": "2018-01-31T00:00:01Z",
         *   "dateUpdated": "2018-01-31T00:00:01Z",
         *   "status": {
         *     "name": "Open"
         *   },
         *   "paymentStatus": {
         *     "name": "Paid Partially"
         *   },
         *   "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
         *   "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
         *   "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
         *   "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
         *   "feeFineId": "3a0d8d79-7f28-4795-aa92-2f5d67140e6b",
         *   "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
         *   "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
         * }
         * 
         * 
         * 
         * @param entity
         *     {
         *       "amount": 15.0,
         *       "remaining": 15.0,
         *       "accountTransaction": 1,
         *       "notes": "This is a note account",
         *       "dateCreated": "2018-01-31T00:00:01Z",
         *       "dateUpdated": "2018-01-31T00:00:01Z",
         *       "status": {
         *         "name": "Open"
         *       },
         *       "paymentStatus": {
         *         "name": "Paid Partially"
         *       },
         *       "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
         *       "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
         *       "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
         *       "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
         *       "feeFineId": "3a0d8d79-7f28-4795-aa92-2f5d67140e6b",
         *       "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
         *       "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
         *     }
         *     
         *     
         */
        public static AccountsResource.GetAccountsByAccountIdResponse withJsonOK(Account entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new AccountsResource.GetAccountsByAccountIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "account not found"
         * 
         * 
         * @param entity
         *     "account not found"
         *     
         */
        public static AccountsResource.GetAccountsByAccountIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.GetAccountsByAccountIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static AccountsResource.GetAccountsByAccountIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.GetAccountsByAccountIdResponse(responseBuilder.build());
        }

    }

    public class GetAccountsResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetAccountsResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a list of account items e.g. {
         *   "accounts" : [ {
         *     "amount": 15.0,
         *     "remaining": 15.0,
         *     "accountTransaction": 1,
         *     "notes": "This is a note",
         *     "dateCreated": "2018-01-31T00:00:01Z",
         *     "dateUpdated": "2018-01-31T00:00:01Z",
         *     "status": {
         *       "name": "Open"
         *     },
         *     "paymentStatus": {
         *       "name": "Paid Partially"
         *     },
         *     "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
         *     "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
         *     "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
         *     "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
         *     "feefineid": "3a0d8d79-7f28-4795-aa92-2f5d67140e6b",
         *     "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
         *     "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
         *   }, {
         *     "amount": 5.0,
         *     "remaining": 5.0,
         *     "accountTransaction": 2,
         *     "notes": "This is a note 2",
         *     "dateCreated": "2018-01-31T00:00:01Z",
         *     "dateUpdated": "2018-01-31T00:00:01Z",
         *     "status": {
         *       "name": "Closed"
         *     },
         *     "paymentStatus": {
         *       "name": "Waived Partially"
         *     },
         *     "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
         *     "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
         *     "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
         *     "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
         *     "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
         *     "feeFineId": "d7549eed-bc92-4dac-8900-0cf3c8396794",
         *     "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
         *   } ],
         *   "total_records" : 2
         * }
         * 
         * 
         * 
         * @param entity
         *     {
         *       "accounts" : [ {
         *         "amount": 15.0,
         *         "remaining": 15.0,
         *         "accountTransaction": 1,
         *         "notes": "This is a note",
         *         "dateCreated": "2018-01-31T00:00:01Z",
         *         "dateUpdated": "2018-01-31T00:00:01Z",
         *         "status": {
         *           "name": "Open"
         *         },
         *         "paymentStatus": {
         *           "name": "Paid Partially"
         *         },
         *         "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
         *         "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
         *         "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
         *         "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
         *         "feefineid": "3a0d8d79-7f28-4795-aa92-2f5d67140e6b",
         *         "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
         *         "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
         *       }, {
         *         "amount": 5.0,
         *         "remaining": 5.0,
         *         "accountTransaction": 2,
         *         "notes": "This is a note 2",
         *         "dateCreated": "2018-01-31T00:00:01Z",
         *         "dateUpdated": "2018-01-31T00:00:01Z",
         *         "status": {
         *           "name": "Closed"
         *         },
         *         "paymentStatus": {
         *           "name": "Waived Partially"
         *         },
         *         "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
         *         "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
         *         "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
         *         "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
         *         "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
         *         "feeFineId": "d7549eed-bc92-4dac-8900-0cf3c8396794",
         *         "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
         *       } ],
         *       "total_records" : 2
         *     }
         *     
         *     
         */
        public static AccountsResource.GetAccountsResponse withJsonOK(AccountdataCollection entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new AccountsResource.GetAccountsResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. unable to list accounts -- malformed parameter 'query', syntax error at column 6
         * 
         * @param entity
         *     unable to list accounts -- malformed parameter 'query', syntax error at column 6
         */
        public static AccountsResource.GetAccountsResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.GetAccountsResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to list accounts -- unauthorized
         * 
         * @param entity
         *     unable to list accounts -- unauthorized
         */
        public static AccountsResource.GetAccountsResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.GetAccountsResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static AccountsResource.GetAccountsResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.GetAccountsResponse(responseBuilder.build());
        }

    }

    public enum Order {

        desc,
        asc;

    }

    public class PostAccountsResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PostAccountsResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a newly created item, with server-controlled fields like 'id' populated e.g. {
         *   "amount": 15.0,
         *   "remaining": 15.0,
         *   "accountTransaction": 1,
         *   "notes": "This is a note account",
         *   "dateCreated": "2018-01-31T00:00:01Z",
         *   "dateUpdated": "2018-01-31T00:00:01Z",
         *   "status": {
         *     "name": "Open"
         *   },
         *   "paymentStatus": {
         *     "name": "Paid Partially"
         *   },
         *   "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
         *   "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
         *   "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
         *   "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
         *   "feeFineId": "3a0d8d79-7f28-4795-aa92-2f5d67140e6b",
         *   "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
         *   "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
         * }
         * 
         * 
         * 
         * @param location
         *     URI to the created account item
         * @param entity
         *     {
         *       "amount": 15.0,
         *       "remaining": 15.0,
         *       "accountTransaction": 1,
         *       "notes": "This is a note account",
         *       "dateCreated": "2018-01-31T00:00:01Z",
         *       "dateUpdated": "2018-01-31T00:00:01Z",
         *       "status": {
         *         "name": "Open"
         *       },
         *       "paymentStatus": {
         *         "name": "Paid Partially"
         *       },
         *       "loanId": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378",
         *       "materialTypeId": "2b94c631-fca9-a892-c730-03ee529ffe27",
         *       "userId": "fc45c606-410d-4a7c-9f95-1a3fea2eef42",
         *       "itemId": "23fdb0bc-ab58-442a-b326-577a96204487",
         *       "feeFineId": "3a0d8d79-7f28-4795-aa92-2f5d67140e6b",
         *       "ownerId": "cfebe06e-d4f8-4d4e-8f5c-4d70595d74f8",
         *       "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434379"
         *     }
         *     
         *     
         */
        public static AccountsResource.PostAccountsResponse withJsonCreated(String location, StreamingOutput entity) {
            Response.ResponseBuilder responseBuilder = Response.status(201).header("Content-Type", "application/json").header("Location", location);
            responseBuilder.entity(entity);
            return new AccountsResource.PostAccountsResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to add account -- malformed JSON at 13:3"
         * 
         * 
         * @param entity
         *     "unable to add account -- malformed JSON at 13:3"
         *     
         */
        public static AccountsResource.PostAccountsResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.PostAccountsResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to create accounts -- unauthorized
         * 
         * @param entity
         *     unable to create accounts -- unauthorized
         */
        public static AccountsResource.PostAccountsResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.PostAccountsResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static AccountsResource.PostAccountsResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.PostAccountsResponse(responseBuilder.build());
        }

    }

    public class PutAccountsByAccountIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PutAccountsByAccountIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item successfully updated
         * 
         */
        public static AccountsResource.PutAccountsByAccountIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new AccountsResource.PutAccountsByAccountIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "account not found"
         * 
         * 
         * @param entity
         *     "account not found"
         *     
         */
        public static AccountsResource.PutAccountsByAccountIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.PutAccountsByAccountIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to update account -- malformed JSON at 13:4"
         * 
         * 
         * @param entity
         *     "unable to update account -- malformed JSON at 13:4"
         *     
         */
        public static AccountsResource.PutAccountsByAccountIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.PutAccountsByAccountIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static AccountsResource.PutAccountsByAccountIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new AccountsResource.PutAccountsByAccountIdResponse(responseBuilder.build());
        }

    }

}
