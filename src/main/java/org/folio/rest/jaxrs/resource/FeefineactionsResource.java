
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
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.jaxrs.model.FeefineactiondataCollection;


/**
 * Collection of feefineaction items.
 * 
 */
@Path("feefineactions")
public interface FeefineactionsResource {


    /**
     * Return a list of feefineactions
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
    void getFeefineactions(
        @QueryParam("query")
        String query,
        @QueryParam("orderBy")
        String orderBy,
        @QueryParam("order")
        @DefaultValue("desc")
        FeefineactionsResource.Order order,
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
     * Create a feefineaction
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
     *       "dateAction": "2017-07-24T01:24:01Z",
     *       "typeAction": "Payment-ckeck",
     *       "amount": -200.00,
     *       "balance": 50.00,
     *       "transactionNumber": 3452,
     *       "comment": "This a comment",
     *       "feeFineId": "d771eb5b-6c9b-408e-a624-a8656d55293e",
     *       "userId": "1ad737b0-d847-11e6-bf26-cec0c932ce01",
     *       "id": "bcb9b1d9-b967-41c4-a476-a4c282527cea"
     *     }
     *     
     */
    @POST
    @Consumes("application/json")
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void postFeefineactions(
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Feefineaction entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Get a single feefineaction
     * 
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param feefineactionId
     *     
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     */
    @GET
    @Path("{feefineactionId}")
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void getFeefineactionsByFeefineactionId(
        @PathParam("feefineactionId")
        @NotNull
        String feefineactionId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Delete feefineaction item with given {feefineactionId}
     * 
     * 
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param feefineactionId
     *     
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     */
    @DELETE
    @Path("{feefineactionId}")
    @Produces({
        "text/plain"
    })
    @Validate
    void deleteFeefineactionsByFeefineactionId(
        @PathParam("feefineactionId")
        @NotNull
        String feefineactionId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Update feefineaction item with given {feefineactionId}
     * 
     * 
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param feefineactionId
     *     
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     * @param entity
     *      e.g. {
     *       "dateAction": "2017-07-24T01:24:01Z",
     *       "typeAction": "Payment-ckeck",
     *       "amount": -200.00,
     *       "balance": 50.00,
     *       "transactionNumber": 3452,
     *       "comment": "This a comment",
     *       "feeFineId": "d771eb5b-6c9b-408e-a624-a8656d55293e",
     *       "userId": "1ad737b0-d847-11e6-bf26-cec0c932ce01",
     *       "id": "bcb9b1d9-b967-41c4-a476-a4c282527cea"
     *     }
     *     
     */
    @PUT
    @Path("{feefineactionId}")
    @Consumes("application/json")
    @Produces({
        "text/plain"
    })
    @Validate
    void putFeefineactionsByFeefineactionId(
        @PathParam("feefineactionId")
        @NotNull
        String feefineactionId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Feefineaction entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    public class DeleteFeefineactionsByFeefineactionIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private DeleteFeefineactionsByFeefineactionIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item deleted successfully
         * 
         */
        public static FeefineactionsResource.DeleteFeefineactionsByFeefineactionIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new FeefineactionsResource.DeleteFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefineaction not found"
         * 
         * 
         * @param entity
         *     "feefineaction not found"
         *     
         */
        public static FeefineactionsResource.DeleteFeefineactionsByFeefineactionIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.DeleteFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to delete feefineaction -- constraint violation"
         * 
         * 
         * @param entity
         *     "unable to delete feefineaction -- constraint violation"
         *     
         */
        public static FeefineactionsResource.DeleteFeefineactionsByFeefineactionIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.DeleteFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static FeefineactionsResource.DeleteFeefineactionsByFeefineactionIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.DeleteFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

    }

    public class GetFeefineactionsByFeefineactionIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetFeefineactionsByFeefineactionIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns item with a given ID e.g. {
         *   "dateAction": "2017-07-24T01:24:01Z",
         *   "typeAction": "Payment-ckeck",
         *   "amount": -200.00,
         *   "balance": 50.00,
         *   "transactionNumber": 3452,
         *   "comment": "This a comment",
         *   "feeFineId": "d771eb5b-6c9b-408e-a624-a8656d55293e",
         *   "userId": "1ad737b0-d847-11e6-bf26-cec0c932ce01",
         *   "id": "bcb9b1d9-b967-41c4-a476-a4c282527cea"
         * }
         * 
         * 
         * @param entity
         *     {
         *       "dateAction": "2017-07-24T01:24:01Z",
         *       "typeAction": "Payment-ckeck",
         *       "amount": -200.00,
         *       "balance": 50.00,
         *       "transactionNumber": 3452,
         *       "comment": "This a comment",
         *       "feeFineId": "d771eb5b-6c9b-408e-a624-a8656d55293e",
         *       "userId": "1ad737b0-d847-11e6-bf26-cec0c932ce01",
         *       "id": "bcb9b1d9-b967-41c4-a476-a4c282527cea"
         *     }
         *     
         */
        public static FeefineactionsResource.GetFeefineactionsByFeefineactionIdResponse withJsonOK(Feefineaction entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.GetFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefineaction not found"
         * 
         * 
         * @param entity
         *     "feefineaction not found"
         *     
         */
        public static FeefineactionsResource.GetFeefineactionsByFeefineactionIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.GetFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefineactionsResource.GetFeefineactionsByFeefineactionIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.GetFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

    }

    public class GetFeefineactionsResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetFeefineactionsResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a list of feefineaction items e.g. {
         *  "feefineactions": [{
         *    "dateAction": "2017-07-24T01:24:01Z",
         *    "typeAction": "Payment-ckeck",
         *    "amount": -200.00,
         *    "balance": 50.00,
         *    "transactionNumber": 3452,
         *    "comment": "This a comment",
         *    "feeFineId": "d771eb5b-6c9b-408e-a624-a8656d55293e",
         *    "userId": "1ad737b0-d847-11e6-bf26-cec0c932ce01",
         *    "id": "bcb9b1d9-b967-41c4-a476-a4c282527cea"
         *  }],
         *    "total_records": 1
         * }
         * 
         * 
         * @param entity
         *     {
         *      "feefineactions": [{
         *        "dateAction": "2017-07-24T01:24:01Z",
         *        "typeAction": "Payment-ckeck",
         *        "amount": -200.00,
         *        "balance": 50.00,
         *        "transactionNumber": 3452,
         *        "comment": "This a comment",
         *        "feeFineId": "d771eb5b-6c9b-408e-a624-a8656d55293e",
         *        "userId": "1ad737b0-d847-11e6-bf26-cec0c932ce01",
         *        "id": "bcb9b1d9-b967-41c4-a476-a4c282527cea"
         *      }],
         *        "total_records": 1
         *     }
         *     
         */
        public static FeefineactionsResource.GetFeefineactionsResponse withJsonOK(FeefineactiondataCollection entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.GetFeefineactionsResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. unable to list feefineactions -- malformed parameter 'query', syntax error at column 6
         * 
         * @param entity
         *     unable to list feefineactions -- malformed parameter 'query', syntax error at column 6
         */
        public static FeefineactionsResource.GetFeefineactionsResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.GetFeefineactionsResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to list feefineactions -- unauthorized
         * 
         * @param entity
         *     unable to list feefineactions -- unauthorized
         */
        public static FeefineactionsResource.GetFeefineactionsResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.GetFeefineactionsResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefineactionsResource.GetFeefineactionsResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.GetFeefineactionsResponse(responseBuilder.build());
        }

    }

    public enum Order {

        desc,
        asc;

    }

    public class PostFeefineactionsResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PostFeefineactionsResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a newly created item, with server-controlled fields like 'id' populated e.g. {
         *   "dateAction": "2017-07-24T01:24:01Z",
         *   "typeAction": "Payment-ckeck",
         *   "amount": -200.00,
         *   "balance": 50.00,
         *   "transactionNumber": 3452,
         *   "comment": "This a comment",
         *   "feeFineId": "d771eb5b-6c9b-408e-a624-a8656d55293e",
         *   "userId": "1ad737b0-d847-11e6-bf26-cec0c932ce01",
         *   "id": "bcb9b1d9-b967-41c4-a476-a4c282527cea"
         * }
         * 
         * 
         * @param location
         *     URI to the created feefineaction item
         * @param entity
         *     {
         *       "dateAction": "2017-07-24T01:24:01Z",
         *       "typeAction": "Payment-ckeck",
         *       "amount": -200.00,
         *       "balance": 50.00,
         *       "transactionNumber": 3452,
         *       "comment": "This a comment",
         *       "feeFineId": "d771eb5b-6c9b-408e-a624-a8656d55293e",
         *       "userId": "1ad737b0-d847-11e6-bf26-cec0c932ce01",
         *       "id": "bcb9b1d9-b967-41c4-a476-a4c282527cea"
         *     }
         *     
         */
        public static FeefineactionsResource.PostFeefineactionsResponse withJsonCreated(String location, StreamingOutput entity) {
            Response.ResponseBuilder responseBuilder = Response.status(201).header("Content-Type", "application/json").header("Location", location);
            responseBuilder.entity(entity);
            return new FeefineactionsResource.PostFeefineactionsResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to add feefineaction -- malformed JSON at 13:3"
         * 
         * 
         * @param entity
         *     "unable to add feefineaction -- malformed JSON at 13:3"
         *     
         */
        public static FeefineactionsResource.PostFeefineactionsResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.PostFeefineactionsResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to create feefineactions -- unauthorized
         * 
         * @param entity
         *     unable to create feefineactions -- unauthorized
         */
        public static FeefineactionsResource.PostFeefineactionsResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.PostFeefineactionsResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static FeefineactionsResource.PostFeefineactionsResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.PostFeefineactionsResponse(responseBuilder.build());
        }

    }

    public class PutFeefineactionsByFeefineactionIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PutFeefineactionsByFeefineactionIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item successfully updated
         * 
         */
        public static FeefineactionsResource.PutFeefineactionsByFeefineactionIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new FeefineactionsResource.PutFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefineaction not found"
         * 
         * 
         * @param entity
         *     "feefineaction not found"
         *     
         */
        public static FeefineactionsResource.PutFeefineactionsByFeefineactionIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.PutFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to update feefineaction -- malformed JSON at 13:4"
         * 
         * 
         * @param entity
         *     "unable to update feefineaction -- malformed JSON at 13:4"
         *     
         */
        public static FeefineactionsResource.PutFeefineactionsByFeefineactionIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.PutFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefineactionsResource.PutFeefineactionsByFeefineactionIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefineactionsResource.PutFeefineactionsByFeefineactionIdResponse(responseBuilder.build());
        }

    }

}
