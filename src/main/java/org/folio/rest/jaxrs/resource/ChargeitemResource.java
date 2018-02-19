
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
import org.folio.rest.jaxrs.model.Chargeitem;
import org.folio.rest.jaxrs.model.ChargeitemdataCollection;


/**
 * Collection of chargeitem items.
 * 
 */
@Path("chargeitem")
public interface ChargeitemResource {


    /**
     * Return a list of chargeitem
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
    void getChargeitem(
        @QueryParam("query")
        String query,
        @QueryParam("orderBy")
        String orderBy,
        @QueryParam("order")
        @DefaultValue("desc")
        ChargeitemResource.Order order,
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
     * Create a chargeitem
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
     *       "instance": "Transparent water",
     *       "callNumber": "M1366.S67 T73 2017",
     *       "barcode": "2453505835707",
     *       "itemStatus": "Checked out",
     *       "location": "Main Library",
     *       "id": "e9285a1c-1dfc-4380-868c-e74073003f43"
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
    void postChargeitem(
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Chargeitem entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Get a single chargeitem
     * 
     * @param chargeitemId
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
    @Path("{chargeitemId}")
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void getChargeitemByChargeitemId(
        @PathParam("chargeitemId")
        @NotNull
        String chargeitemId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Delete chargeitem item with given {chargeitemId}
     * 
     * 
     * @param chargeitemId
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
    @Path("{chargeitemId}")
    @Produces({
        "text/plain"
    })
    @Validate
    void deleteChargeitemByChargeitemId(
        @PathParam("chargeitemId")
        @NotNull
        String chargeitemId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Update chargeitem item with given {chargeitemId}
     * 
     * 
     * @param chargeitemId
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
     *       "instance": "Transparent water",
     *       "callNumber": "M1366.S67 T73 2017",
     *       "barcode": "2453505835707",
     *       "itemStatus": "Checked out",
     *       "location": "Main Library",
     *       "id": "e9285a1c-1dfc-4380-868c-e74073003f43"
     *     }
     *     
     */
    @PUT
    @Path("{chargeitemId}")
    @Consumes("application/json")
    @Produces({
        "text/plain"
    })
    @Validate
    void putChargeitemByChargeitemId(
        @PathParam("chargeitemId")
        @NotNull
        String chargeitemId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Chargeitem entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    public class DeleteChargeitemByChargeitemIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private DeleteChargeitemByChargeitemIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item deleted successfully
         * 
         */
        public static ChargeitemResource.DeleteChargeitemByChargeitemIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new ChargeitemResource.DeleteChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "chargeitem not found"
         * 
         * 
         * @param entity
         *     "chargeitem not found"
         *     
         */
        public static ChargeitemResource.DeleteChargeitemByChargeitemIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.DeleteChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to delete chargeitem -- constraint violation"
         * 
         * 
         * @param entity
         *     "unable to delete chargeitem -- constraint violation"
         *     
         */
        public static ChargeitemResource.DeleteChargeitemByChargeitemIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.DeleteChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static ChargeitemResource.DeleteChargeitemByChargeitemIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.DeleteChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

    }

    public class GetChargeitemByChargeitemIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetChargeitemByChargeitemIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns item with a given ID e.g. {
         *   "instance": "Transparent water",
         *   "callNumber": "M1366.S67 T73 2017",
         *   "barcode": "2453505835707",
         *   "itemStatus": "Checked out",
         *   "location": "Main Library",
         *   "id": "e9285a1c-1dfc-4380-868c-e74073003f43"
         * }
         * 
         * 
         * @param entity
         *     {
         *       "instance": "Transparent water",
         *       "callNumber": "M1366.S67 T73 2017",
         *       "barcode": "2453505835707",
         *       "itemStatus": "Checked out",
         *       "location": "Main Library",
         *       "id": "e9285a1c-1dfc-4380-868c-e74073003f43"
         *     }
         *     
         */
        public static ChargeitemResource.GetChargeitemByChargeitemIdResponse withJsonOK(Chargeitem entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new ChargeitemResource.GetChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "chargeitem not found"
         * 
         * 
         * @param entity
         *     "chargeitem not found"
         *     
         */
        public static ChargeitemResource.GetChargeitemByChargeitemIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.GetChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static ChargeitemResource.GetChargeitemByChargeitemIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.GetChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

    }

    public class GetChargeitemResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetChargeitemResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a list of chargeitem items e.g. {
         *   "chargeitem": [{
         *     "instance": "Transparent water",
         *     "callNumber": "M1366.S67 T73 2017",
         *     "barcode": "2453505835707",
         *     "itemStatus": "Checked out",
         *     "location": "Main Library",
         *     "id": "e9285a1c-1dfc-4380-868c-e74073003f43"
         *     }, {
         *     "instance": "Temeraire",
         *     "callNumber": "some-callnumber",
         *     "barcode": "645398607547",
         *     "itemStatus": "Checked out",
         *     "location": "Main Library",
         *     "id": "e6d7e91a-4dbc-4a70-9b38-e000d2fbdc79"  
         *   } ],
         *   "total_records": 2
         * }
         * 
         * 
         * @param entity
         *     {
         *       "chargeitem": [{
         *         "instance": "Transparent water",
         *         "callNumber": "M1366.S67 T73 2017",
         *         "barcode": "2453505835707",
         *         "itemStatus": "Checked out",
         *         "location": "Main Library",
         *         "id": "e9285a1c-1dfc-4380-868c-e74073003f43"
         *         }, {
         *         "instance": "Temeraire",
         *         "callNumber": "some-callnumber",
         *         "barcode": "645398607547",
         *         "itemStatus": "Checked out",
         *         "location": "Main Library",
         *         "id": "e6d7e91a-4dbc-4a70-9b38-e000d2fbdc79"  
         *       } ],
         *       "total_records": 2
         *     }
         *     
         */
        public static ChargeitemResource.GetChargeitemResponse withJsonOK(ChargeitemdataCollection entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new ChargeitemResource.GetChargeitemResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. unable to list chargeitem -- malformed parameter 'query', syntax error at column 6
         * 
         * @param entity
         *     unable to list chargeitem -- malformed parameter 'query', syntax error at column 6
         */
        public static ChargeitemResource.GetChargeitemResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.GetChargeitemResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to list chargeitem -- unauthorized
         * 
         * @param entity
         *     unable to list chargeitem -- unauthorized
         */
        public static ChargeitemResource.GetChargeitemResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.GetChargeitemResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static ChargeitemResource.GetChargeitemResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.GetChargeitemResponse(responseBuilder.build());
        }

    }

    public enum Order {

        desc,
        asc;

    }

    public class PostChargeitemResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PostChargeitemResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a newly created item, with server-controlled fields like 'id' populated e.g. {
         *   "instance": "Transparent water",
         *   "callNumber": "M1366.S67 T73 2017",
         *   "barcode": "2453505835707",
         *   "itemStatus": "Checked out",
         *   "location": "Main Library",
         *   "id": "e9285a1c-1dfc-4380-868c-e74073003f43"
         * }
         * 
         * 
         * @param location
         *     URI to the created chargeitem item
         * @param entity
         *     {
         *       "instance": "Transparent water",
         *       "callNumber": "M1366.S67 T73 2017",
         *       "barcode": "2453505835707",
         *       "itemStatus": "Checked out",
         *       "location": "Main Library",
         *       "id": "e9285a1c-1dfc-4380-868c-e74073003f43"
         *     }
         *     
         */
        public static ChargeitemResource.PostChargeitemResponse withJsonCreated(String location, StreamingOutput entity) {
            Response.ResponseBuilder responseBuilder = Response.status(201).header("Content-Type", "application/json").header("Location", location);
            responseBuilder.entity(entity);
            return new ChargeitemResource.PostChargeitemResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to add chargeitem -- malformed JSON at 13:3"
         * 
         * 
         * @param entity
         *     "unable to add chargeitem -- malformed JSON at 13:3"
         *     
         */
        public static ChargeitemResource.PostChargeitemResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.PostChargeitemResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to create chargeitem -- unauthorized
         * 
         * @param entity
         *     unable to create chargeitem -- unauthorized
         */
        public static ChargeitemResource.PostChargeitemResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.PostChargeitemResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static ChargeitemResource.PostChargeitemResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.PostChargeitemResponse(responseBuilder.build());
        }

    }

    public class PutChargeitemByChargeitemIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PutChargeitemByChargeitemIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item successfully updated
         * 
         */
        public static ChargeitemResource.PutChargeitemByChargeitemIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new ChargeitemResource.PutChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "chargeitem not found"
         * 
         * 
         * @param entity
         *     "chargeitem not found"
         *     
         */
        public static ChargeitemResource.PutChargeitemByChargeitemIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.PutChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to update chargeitem -- malformed JSON at 13:4"
         * 
         * 
         * @param entity
         *     "unable to update chargeitem -- malformed JSON at 13:4"
         *     
         */
        public static ChargeitemResource.PutChargeitemByChargeitemIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.PutChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static ChargeitemResource.PutChargeitemByChargeitemIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new ChargeitemResource.PutChargeitemByChargeitemIdResponse(responseBuilder.build());
        }

    }

}
