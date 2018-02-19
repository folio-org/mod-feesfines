
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
import org.folio.rest.jaxrs.model.Feefinehistory;
import org.folio.rest.jaxrs.model.FeefinehistorydataCollection;


/**
 * Collection of feefinehistory items.
 * 
 */
@Path("feefinehistory")
public interface FeefinehistoryResource {


    /**
     * Return a list of feefinehistorys
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
    void getFeefinehistory(
        @QueryParam("query")
        String query,
        @QueryParam("orderBy")
        String orderBy,
        @QueryParam("order")
        @DefaultValue("desc")
        FeefinehistoryResource.Order order,
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
     * Create a feefinehistory
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
     *       "dateCreated": "2018-01-31",
     *       "dateUpdated": "2018-01-31",
     *       "feeFineType": "Damaged Camera Fee",
     *       "charged": 1000.00,
     *       "remaining": 50.00,
     *       "status":"Open",
     *       "paymentStatus":"Paid Partially",
     *       "loanTransaction": 1,
     *       "item": "Digital Camera 1",
     *       "barcode": "1234567890",
     *       "itemType": "Equipment",
     *       "callNumber": "Equip Shelf 1 Spot 42",
     *       "feeFineOwner": "Main Library Circulation Desk",
     *       "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
     *       "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378"
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
    void postFeefinehistory(
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Feefinehistory entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Get a single feefinehistory
     * 
     * @param feefinehistoryId
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
    @Path("{feefinehistoryId}")
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void getFeefinehistoryByFeefinehistoryId(
        @PathParam("feefinehistoryId")
        @NotNull
        String feefinehistoryId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Delete feefinehistory item with given {feefinehistoryId}
     * 
     * 
     * @param feefinehistoryId
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
    @Path("{feefinehistoryId}")
    @Produces({
        "text/plain"
    })
    @Validate
    void deleteFeefinehistoryByFeefinehistoryId(
        @PathParam("feefinehistoryId")
        @NotNull
        String feefinehistoryId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Update feefinehistory item with given {feefinehistoryId}
     * 
     * 
     * @param feefinehistoryId
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
     *       "dateCreated": "2018-01-31",
     *       "dateUpdated": "2018-01-31",
     *       "feeFineType": "Damaged Camera Fee",
     *       "charged": 1000.00,
     *       "remaining": 50.00,
     *       "status":"Open",
     *       "paymentStatus":"Paid Partially",
     *       "loanTransaction": 1,
     *       "item": "Digital Camera 1",
     *       "barcode": "1234567890",
     *       "itemType": "Equipment",
     *       "callNumber": "Equip Shelf 1 Spot 42",
     *       "feeFineOwner": "Main Library Circulation Desk",
     *       "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
     *       "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378"
     *     }
     *     
     */
    @PUT
    @Path("{feefinehistoryId}")
    @Consumes("application/json")
    @Produces({
        "text/plain"
    })
    @Validate
    void putFeefinehistoryByFeefinehistoryId(
        @PathParam("feefinehistoryId")
        @NotNull
        String feefinehistoryId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Feefinehistory entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    public class DeleteFeefinehistoryByFeefinehistoryIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private DeleteFeefinehistoryByFeefinehistoryIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item deleted successfully
         * 
         */
        public static FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefinehistory not found"
         * 
         * 
         * @param entity
         *     "feefinehistory not found"
         *     
         */
        public static FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to delete feefinehistory -- constraint violation"
         * 
         * 
         * @param entity
         *     "unable to delete feefinehistory -- constraint violation"
         *     
         */
        public static FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.DeleteFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

    }

    public class GetFeefinehistoryByFeefinehistoryIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetFeefinehistoryByFeefinehistoryIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns item with a given ID e.g. {
         *   "dateCreated": "2018-01-31",
         *   "dateUpdated": "2018-01-31",
         *   "feeFineType": "Damaged Camera Fee",
         *   "charged": 1000.00,
         *   "remaining": 50.00,
         *   "status":"Open",
         *   "paymentStatus":"Paid Partially",
         *   "loanTransaction": 1,
         *   "item": "Digital Camera 1",
         *   "barcode": "1234567890",
         *   "itemType": "Equipment",
         *   "callNumber": "Equip Shelf 1 Spot 42",
         *   "feeFineOwner": "Main Library Circulation Desk",
         *   "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
         *   "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378"
         * }
         * 
         * 
         * @param entity
         *     {
         *       "dateCreated": "2018-01-31",
         *       "dateUpdated": "2018-01-31",
         *       "feeFineType": "Damaged Camera Fee",
         *       "charged": 1000.00,
         *       "remaining": 50.00,
         *       "status":"Open",
         *       "paymentStatus":"Paid Partially",
         *       "loanTransaction": 1,
         *       "item": "Digital Camera 1",
         *       "barcode": "1234567890",
         *       "itemType": "Equipment",
         *       "callNumber": "Equip Shelf 1 Spot 42",
         *       "feeFineOwner": "Main Library Circulation Desk",
         *       "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
         *       "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378"
         *     }
         *     
         */
        public static FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse withJsonOK(Feefinehistory entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefinehistory not found"
         * 
         * 
         * @param entity
         *     "feefinehistory not found"
         *     
         */
        public static FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.GetFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

    }

    public class GetFeefinehistoryResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetFeefinehistoryResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a list of feefinehistory items e.g. 
         * {
         *   "feefinehistory": [{
         *     "dateCreated": "2018-01-31",
         *     "dateUpdated": "2018-01-31",
         *     "feeFineType": "Damaged Camera Fee",
         *     "charged": 1000.00,
         *     "remaining": 50.00,
         *     "status": "Open",
         *     "paymentStatus": "Paid Partially",
         *     "loanTransaction": 1,
         *     "item": "Digital Camera 1",
         *     "barcode": "2345678901",
         *     "itemType": "Equipment",
         *     "callNumber": "Equip Shelf 1 Spot 42",
         *     "feeFineOwner": "IISUE Library Circulation Desk",
         *     "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
         *     "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434376"
         *     }, {
         *     "dateCreated": "2018-01-31",
         *     "dateUpdated": "2018-01-31",
         *     "feeFineType": "Damaged Item Fee",
         *     "charged": 25.00,
         *     "remaining": 15.00,
         *     "status": "Closed",
         *     "paymentStatus": "Refunded Fully",
         *     "loanTransaction": 2,
         *     "item": "Interesting Times",
         *     "barcode": "326547658598",
         *     "itemType": "book",
         *     "callNumber": "QH332.A44 2006 V.1 C.2",
         *     "feeFineOwner": "Library Circulation Desk",
         *     "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
         *     "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434377"
         *   } ],
         *   "total_records": 2
         * }
         * 
         * 
         * 
         * @param entity
         *     
         *     {
         *       "feefinehistory": [{
         *         "dateCreated": "2018-01-31",
         *         "dateUpdated": "2018-01-31",
         *         "feeFineType": "Damaged Camera Fee",
         *         "charged": 1000.00,
         *         "remaining": 50.00,
         *         "status": "Open",
         *         "paymentStatus": "Paid Partially",
         *         "loanTransaction": 1,
         *         "item": "Digital Camera 1",
         *         "barcode": "2345678901",
         *         "itemType": "Equipment",
         *         "callNumber": "Equip Shelf 1 Spot 42",
         *         "feeFineOwner": "IISUE Library Circulation Desk",
         *         "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
         *         "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434376"
         *         }, {
         *         "dateCreated": "2018-01-31",
         *         "dateUpdated": "2018-01-31",
         *         "feeFineType": "Damaged Item Fee",
         *         "charged": 25.00,
         *         "remaining": 15.00,
         *         "status": "Closed",
         *         "paymentStatus": "Refunded Fully",
         *         "loanTransaction": 2,
         *         "item": "Interesting Times",
         *         "barcode": "326547658598",
         *         "itemType": "book",
         *         "callNumber": "QH332.A44 2006 V.1 C.2",
         *         "feeFineOwner": "Library Circulation Desk",
         *         "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
         *         "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434377"
         *       } ],
         *       "total_records": 2
         *     }
         *     
         *     
         */
        public static FeefinehistoryResource.GetFeefinehistoryResponse withJsonOK(FeefinehistorydataCollection entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.GetFeefinehistoryResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. unable to list feefinehistory -- malformed parameter 'query', syntax error at column 6
         * 
         * @param entity
         *     unable to list feefinehistory -- malformed parameter 'query', syntax error at column 6
         */
        public static FeefinehistoryResource.GetFeefinehistoryResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.GetFeefinehistoryResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to list feefinehistory -- unauthorized
         * 
         * @param entity
         *     unable to list feefinehistory -- unauthorized
         */
        public static FeefinehistoryResource.GetFeefinehistoryResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.GetFeefinehistoryResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefinehistoryResource.GetFeefinehistoryResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.GetFeefinehistoryResponse(responseBuilder.build());
        }

    }

    public enum Order {

        desc,
        asc;

    }

    public class PostFeefinehistoryResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PostFeefinehistoryResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a newly created item, with server-controlled fields like 'id' populated e.g. {
         *   "dateCreated": "2018-01-31",
         *   "dateUpdated": "2018-01-31",
         *   "feeFineType": "Damaged Camera Fee",
         *   "charged": 1000.00,
         *   "remaining": 50.00,
         *   "status":"Open",
         *   "paymentStatus":"Paid Partially",
         *   "loanTransaction": 1,
         *   "item": "Digital Camera 1",
         *   "barcode": "1234567890",
         *   "itemType": "Equipment",
         *   "callNumber": "Equip Shelf 1 Spot 42",
         *   "feeFineOwner": "Main Library Circulation Desk",
         *   "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
         *   "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378"
         * }
         * 
         * 
         * @param location
         *     URI to the created feefinehistory item
         * @param entity
         *     {
         *       "dateCreated": "2018-01-31",
         *       "dateUpdated": "2018-01-31",
         *       "feeFineType": "Damaged Camera Fee",
         *       "charged": 1000.00,
         *       "remaining": 50.00,
         *       "status":"Open",
         *       "paymentStatus":"Paid Partially",
         *       "loanTransaction": 1,
         *       "item": "Digital Camera 1",
         *       "barcode": "1234567890",
         *       "itemType": "Equipment",
         *       "callNumber": "Equip Shelf 1 Spot 42",
         *       "feeFineOwner": "Main Library Circulation Desk",
         *       "userId": "04d830a8-810c-415c-aa50-4d09c1fee133",
         *       "id": "0bab56e5-1ab6-4ac2-afdf-8b2df0434378"
         *     }
         *     
         */
        public static FeefinehistoryResource.PostFeefinehistoryResponse withJsonCreated(String location, StreamingOutput entity) {
            Response.ResponseBuilder responseBuilder = Response.status(201).header("Content-Type", "application/json").header("Location", location);
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.PostFeefinehistoryResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to add feefinehistory -- malformed JSON at 13:3"
         * 
         * 
         * @param entity
         *     "unable to add feefinehistory -- malformed JSON at 13:3"
         *     
         */
        public static FeefinehistoryResource.PostFeefinehistoryResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.PostFeefinehistoryResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to create feefinehistory -- unauthorized
         * 
         * @param entity
         *     unable to create feefinehistory -- unauthorized
         */
        public static FeefinehistoryResource.PostFeefinehistoryResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.PostFeefinehistoryResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static FeefinehistoryResource.PostFeefinehistoryResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.PostFeefinehistoryResponse(responseBuilder.build());
        }

    }

    public class PutFeefinehistoryByFeefinehistoryIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PutFeefinehistoryByFeefinehistoryIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item successfully updated
         * 
         */
        public static FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefinehistory not found"
         * 
         * 
         * @param entity
         *     "feefinehistory not found"
         *     
         */
        public static FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to update feefinehistory -- malformed JSON at 13:4"
         * 
         * 
         * @param entity
         *     "unable to update feefinehistory -- malformed JSON at 13:4"
         *     
         */
        public static FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinehistoryResource.PutFeefinehistoryByFeefinehistoryIdResponse(responseBuilder.build());
        }

    }

}
