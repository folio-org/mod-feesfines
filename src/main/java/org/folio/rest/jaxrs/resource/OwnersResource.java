
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
import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.jaxrs.model.OwnerdataCollection;


/**
 * Collection of owner items.
 * 
 */
@Path("owners")
public interface OwnersResource {


    /**
     * Return a list of owners
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
    void getOwners(
        @QueryParam("query")
        String query,
        @QueryParam("orderBy")
        String orderBy,
        @QueryParam("order")
        @DefaultValue("desc")
        OwnersResource.Order order,
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
     * Create a owner
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
     *       "owner": "Main Circ", 
     *       "id": "7261ecaae3a74dc68b468e12a70b1aef",
     *       "desc": "Main Library Circulation Desk"
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
    void postOwners(
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Owner entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Get a single owner
     * 
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param ownerId
     *     
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     */
    @GET
    @Path("{ownerId}")
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void getOwnersByOwnerId(
        @PathParam("ownerId")
        @NotNull
        String ownerId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Delete owner item with given {ownerId}
     * 
     * 
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param ownerId
     *     
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     */
    @DELETE
    @Path("{ownerId}")
    @Produces({
        "text/plain"
    })
    @Validate
    void deleteOwnersByOwnerId(
        @PathParam("ownerId")
        @NotNull
        String ownerId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Update owner item with given {ownerId}
     * 
     * 
     * @param vertxContext
     *      The Vertx Context Object <code>io.vertx.core.Context</code> 
     * @param asyncResultHandler
     *     A <code>Handler<AsyncResult<Response>>></code> handler {@link io.vertx.core.Handler} which must be called as follows - Note the 'GetPatronsResponse' should be replaced with '[nameOfYourFunction]Response': (example only) <code>asyncResultHandler.handle(io.vertx.core.Future.succeededFuture(GetPatronsResponse.withJsonOK( new ObjectMapper().readValue(reply.result().body().toString(), Patron.class))));</code> in the final callback (most internal callback) of the function.
     * @param ownerId
     *     
     * @param lang
     *     Requested language. Optional. [lang=en]
     *     
     * @param entity
     *      e.g. {
     *       "owner": "Main Circ", 
     *       "id": "7261ecaae3a74dc68b468e12a70b1aef",
     *       "desc": "Main Library Circulation Desk"
     *     }
     *     
     */
    @PUT
    @Path("{ownerId}")
    @Consumes("application/json")
    @Produces({
        "text/plain"
    })
    @Validate
    void putOwnersByOwnerId(
        @PathParam("ownerId")
        @NotNull
        String ownerId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Owner entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    public class DeleteOwnersByOwnerIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private DeleteOwnersByOwnerIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item deleted successfully
         * 
         */
        public static OwnersResource.DeleteOwnersByOwnerIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new OwnersResource.DeleteOwnersByOwnerIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "owner not found"
         * 
         * 
         * @param entity
         *     "owner not found"
         *     
         */
        public static OwnersResource.DeleteOwnersByOwnerIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.DeleteOwnersByOwnerIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to delete owner -- constraint violation"
         * 
         * 
         * @param entity
         *     "unable to delete owner -- constraint violation"
         *     
         */
        public static OwnersResource.DeleteOwnersByOwnerIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.DeleteOwnersByOwnerIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static OwnersResource.DeleteOwnersByOwnerIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.DeleteOwnersByOwnerIdResponse(responseBuilder.build());
        }

    }

    public class GetOwnersByOwnerIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetOwnersByOwnerIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns item with a given ID e.g. {
         *   "owner": "Main Circ", 
         *   "id": "7261ecaae3a74dc68b468e12a70b1aef",
         *   "desc": "Main Library Circulation Desk"
         * }
         * 
         * 
         * @param entity
         *     {
         *       "owner": "Main Circ", 
         *       "id": "7261ecaae3a74dc68b468e12a70b1aef",
         *       "desc": "Main Library Circulation Desk"
         *     }
         *     
         */
        public static OwnersResource.GetOwnersByOwnerIdResponse withJsonOK(Owner entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new OwnersResource.GetOwnersByOwnerIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "owner not found"
         * 
         * 
         * @param entity
         *     "owner not found"
         *     
         */
        public static OwnersResource.GetOwnersByOwnerIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.GetOwnersByOwnerIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static OwnersResource.GetOwnersByOwnerIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.GetOwnersByOwnerIdResponse(responseBuilder.build());
        }

    }

    public class GetOwnersResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetOwnersResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a list of owner items e.g. {
         *   "owners": [
         *    {
         *     "owner": "Main Admin",
         *     "id": "7261ecaae3a74dc68b468e12a70b1aed",
         *     "desc": "Main Library Administration Office"
         *    }
         *   ],
         *   "total_records": 1
         * }
         * 
         * 
         * @param entity
         *     {
         *       "owners": [
         *        {
         *         "owner": "Main Admin",
         *         "id": "7261ecaae3a74dc68b468e12a70b1aed",
         *         "desc": "Main Library Administration Office"
         *        }
         *       ],
         *       "total_records": 1
         *     }
         *     
         */
        public static OwnersResource.GetOwnersResponse withJsonOK(OwnerdataCollection entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new OwnersResource.GetOwnersResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. unable to list owners -- malformed parameter 'query', syntax error at column 6
         * 
         * @param entity
         *     unable to list owners -- malformed parameter 'query', syntax error at column 6
         */
        public static OwnersResource.GetOwnersResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.GetOwnersResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to list owners -- unauthorized
         * 
         * @param entity
         *     unable to list owners -- unauthorized
         */
        public static OwnersResource.GetOwnersResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.GetOwnersResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static OwnersResource.GetOwnersResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.GetOwnersResponse(responseBuilder.build());
        }

    }

    public enum Order {

        desc,
        asc;

    }

    public class PostOwnersResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PostOwnersResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a newly created item, with server-controlled fields like 'id' populated e.g. {
         *   "owner": "Main Circ", 
         *   "id": "7261ecaae3a74dc68b468e12a70b1aef",
         *   "desc": "Main Library Circulation Desk"
         * }
         * 
         * 
         * @param location
         *     URI to the created owner item
         * @param entity
         *     {
         *       "owner": "Main Circ", 
         *       "id": "7261ecaae3a74dc68b468e12a70b1aef",
         *       "desc": "Main Library Circulation Desk"
         *     }
         *     
         */
        public static OwnersResource.PostOwnersResponse withJsonCreated(String location, StreamingOutput entity) {
            Response.ResponseBuilder responseBuilder = Response.status(201).header("Content-Type", "application/json").header("Location", location);
            responseBuilder.entity(entity);
            return new OwnersResource.PostOwnersResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to add owner -- malformed JSON at 13:3"
         * 
         * 
         * @param entity
         *     "unable to add owner -- malformed JSON at 13:3"
         *     
         */
        public static OwnersResource.PostOwnersResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.PostOwnersResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to create owners -- unauthorized
         * 
         * @param entity
         *     unable to create owners -- unauthorized
         */
        public static OwnersResource.PostOwnersResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.PostOwnersResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static OwnersResource.PostOwnersResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.PostOwnersResponse(responseBuilder.build());
        }

    }

    public class PutOwnersByOwnerIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PutOwnersByOwnerIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item successfully updated
         * 
         */
        public static OwnersResource.PutOwnersByOwnerIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new OwnersResource.PutOwnersByOwnerIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "owner not found"
         * 
         * 
         * @param entity
         *     "owner not found"
         *     
         */
        public static OwnersResource.PutOwnersByOwnerIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.PutOwnersByOwnerIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to update owner -- malformed JSON at 13:4"
         * 
         * 
         * @param entity
         *     "unable to update owner -- malformed JSON at 13:4"
         *     
         */
        public static OwnersResource.PutOwnersByOwnerIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.PutOwnersByOwnerIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static OwnersResource.PutOwnersByOwnerIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new OwnersResource.PutOwnersByOwnerIdResponse(responseBuilder.build());
        }

    }

}
