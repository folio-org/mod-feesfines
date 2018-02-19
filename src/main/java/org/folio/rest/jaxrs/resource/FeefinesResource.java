
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
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.FeefinedataCollection;


/**
 * Collection of feefine items.
 * 
 */
@Path("feefines")
public interface FeefinesResource {


    /**
     * Return a list of feefines
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
    void getFeefines(
        @QueryParam("query")
        String query,
        @QueryParam("orderBy")
        String orderBy,
        @QueryParam("order")
        @DefaultValue("desc")
        FeefinesResource.Order order,
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
     * Create a feefine
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
     *     	"feeFineType": "overdue",
     *     	"id": "7261ecaae3a74dc68b468e12a70b1aec",
     *     	"defaultAmount": 100,
     *     	"allowManualCreation": true,
     *     	"taxVat": 15,
     *     	"ownerId": "03ece16e-3fc6-4390-8511-ca7a7882c5c6"
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
    void postFeefines(
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Feefine entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Get a single feefine
     * 
     * @param feefineId
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
    @Path("{feefineId}")
    @Produces({
        "application/json",
        "text/plain"
    })
    @Validate
    void getFeefinesByFeefineId(
        @PathParam("feefineId")
        @NotNull
        String feefineId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Delete feefine item with given {feefineId}
     * 
     * 
     * @param feefineId
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
    @Path("{feefineId}")
    @Produces({
        "text/plain"
    })
    @Validate
    void deleteFeefinesByFeefineId(
        @PathParam("feefineId")
        @NotNull
        String feefineId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    /**
     * Update feefine item with given {feefineId}
     * 
     * 
     * @param feefineId
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
     *     	"feeFineType": "overdue",
     *     	"id": "7261ecaae3a74dc68b468e12a70b1aec",
     *     	"defaultAmount": 100,
     *     	"allowManualCreation": true,
     *     	"taxVat": 15,
     *     	"ownerId": "03ece16e-3fc6-4390-8511-ca7a7882c5c6"
     *     }
     *     
     */
    @PUT
    @Path("{feefineId}")
    @Consumes("application/json")
    @Produces({
        "text/plain"
    })
    @Validate
    void putFeefinesByFeefineId(
        @PathParam("feefineId")
        @NotNull
        String feefineId,
        @QueryParam("lang")
        @DefaultValue("en")
        @Pattern(regexp = "[a-zA-Z]{2}")
        String lang, Feefine entity, java.util.Map<String, String>okapiHeaders, io.vertx.core.Handler<io.vertx.core.AsyncResult<Response>>asyncResultHandler, Context vertxContext)
        throws Exception
    ;

    public class DeleteFeefinesByFeefineIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private DeleteFeefinesByFeefineIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item deleted successfully
         * 
         */
        public static FeefinesResource.DeleteFeefinesByFeefineIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new FeefinesResource.DeleteFeefinesByFeefineIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefine not found"
         * 
         * 
         * @param entity
         *     "feefine not found"
         *     
         */
        public static FeefinesResource.DeleteFeefinesByFeefineIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.DeleteFeefinesByFeefineIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to delete feefine -- constraint violation"
         * 
         * 
         * @param entity
         *     "unable to delete feefine -- constraint violation"
         *     
         */
        public static FeefinesResource.DeleteFeefinesByFeefineIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.DeleteFeefinesByFeefineIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static FeefinesResource.DeleteFeefinesByFeefineIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.DeleteFeefinesByFeefineIdResponse(responseBuilder.build());
        }

    }

    public class GetFeefinesByFeefineIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetFeefinesByFeefineIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns item with a given ID e.g. {
         * 	"feeFineType": "overdue",
         * 	"id": "7261ecaae3a74dc68b468e12a70b1aec",
         * 	"defaultAmount": 100,
         * 	"allowManualCreation": true,
         * 	"taxVat": 15,
         * 	"ownerId": "03ece16e-3fc6-4390-8511-ca7a7882c5c6"
         * }
         * 
         * 
         * @param entity
         *     {
         *     	"feeFineType": "overdue",
         *     	"id": "7261ecaae3a74dc68b468e12a70b1aec",
         *     	"defaultAmount": 100,
         *     	"allowManualCreation": true,
         *     	"taxVat": 15,
         *     	"ownerId": "03ece16e-3fc6-4390-8511-ca7a7882c5c6"
         *     }
         *     
         */
        public static FeefinesResource.GetFeefinesByFeefineIdResponse withJsonOK(Feefine entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new FeefinesResource.GetFeefinesByFeefineIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefine not found"
         * 
         * 
         * @param entity
         *     "feefine not found"
         *     
         */
        public static FeefinesResource.GetFeefinesByFeefineIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.GetFeefinesByFeefineIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefinesResource.GetFeefinesByFeefineIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.GetFeefinesByFeefineIdResponse(responseBuilder.build());
        }

    }

    public class GetFeefinesResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private GetFeefinesResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a list of feefine items e.g. {
         * 	"feefines": [{
         * 		"feeFineType": "overdue",
         * 		"id": "7261ecaae3a74dc68b468e12a70b1aec",
         * 		"defaultAmount": 100,
         * 		"allowManualCreation": true,
         * 		"taxVat": 15,
         * 		"ownerId": "03ece16e-3fc6-4390-8511-ca7a7882c5c6"
         * 	}],
         * 	"total_records": 1
         * }
         * 
         * 
         * @param entity
         *     {
         *     	"feefines": [{
         *     		"feeFineType": "overdue",
         *     		"id": "7261ecaae3a74dc68b468e12a70b1aec",
         *     		"defaultAmount": 100,
         *     		"allowManualCreation": true,
         *     		"taxVat": 15,
         *     		"ownerId": "03ece16e-3fc6-4390-8511-ca7a7882c5c6"
         *     	}],
         *     	"total_records": 1
         *     }
         *     
         */
        public static FeefinesResource.GetFeefinesResponse withJsonOK(FeefinedataCollection entity) {
            Response.ResponseBuilder responseBuilder = Response.status(200).header("Content-Type", "application/json");
            responseBuilder.entity(entity);
            return new FeefinesResource.GetFeefinesResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. unable to list feefines -- malformed parameter 'query', syntax error at column 6
         * 
         * @param entity
         *     unable to list feefines -- malformed parameter 'query', syntax error at column 6
         */
        public static FeefinesResource.GetFeefinesResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.GetFeefinesResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to list feefines -- unauthorized
         * 
         * @param entity
         *     unable to list feefines -- unauthorized
         */
        public static FeefinesResource.GetFeefinesResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.GetFeefinesResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefinesResource.GetFeefinesResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.GetFeefinesResponse(responseBuilder.build());
        }

    }

    public enum Order {

        desc,
        asc;

    }

    public class PostFeefinesResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PostFeefinesResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Returns a newly created item, with server-controlled fields like 'id' populated e.g. {
         * 	"feeFineType": "overdue",
         * 	"id": "7261ecaae3a74dc68b468e12a70b1aec",
         * 	"defaultAmount": 100,
         * 	"allowManualCreation": true,
         * 	"taxVat": 15,
         * 	"ownerId": "03ece16e-3fc6-4390-8511-ca7a7882c5c6"
         * }
         * 
         * 
         * @param location
         *     URI to the created feefine item
         * @param entity
         *     {
         *     	"feeFineType": "overdue",
         *     	"id": "7261ecaae3a74dc68b468e12a70b1aec",
         *     	"defaultAmount": 100,
         *     	"allowManualCreation": true,
         *     	"taxVat": 15,
         *     	"ownerId": "03ece16e-3fc6-4390-8511-ca7a7882c5c6"
         *     }
         *     
         */
        public static FeefinesResource.PostFeefinesResponse withJsonCreated(String location, StreamingOutput entity) {
            Response.ResponseBuilder responseBuilder = Response.status(201).header("Content-Type", "application/json").header("Location", location);
            responseBuilder.entity(entity);
            return new FeefinesResource.PostFeefinesResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to add feefine -- malformed JSON at 13:3"
         * 
         * 
         * @param entity
         *     "unable to add feefine -- malformed JSON at 13:3"
         *     
         */
        public static FeefinesResource.PostFeefinesResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.PostFeefinesResponse(responseBuilder.build());
        }

        /**
         * Not authorized to perform requested action e.g. unable to create feefines -- unauthorized
         * 
         * @param entity
         *     unable to create feefines -- unauthorized
         */
        public static FeefinesResource.PostFeefinesResponse withPlainUnauthorized(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(401).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.PostFeefinesResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. Internal server error, contact administrator
         * 
         * @param entity
         *     Internal server error, contact administrator
         */
        public static FeefinesResource.PostFeefinesResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.PostFeefinesResponse(responseBuilder.build());
        }

    }

    public class PutFeefinesByFeefineIdResponse
        extends org.folio.rest.jaxrs.resource.support.ResponseWrapper
    {


        private PutFeefinesByFeefineIdResponse(Response delegate) {
            super(delegate);
        }

        /**
         * Item successfully updated
         * 
         */
        public static FeefinesResource.PutFeefinesByFeefineIdResponse withNoContent() {
            Response.ResponseBuilder responseBuilder = Response.status(204);
            return new FeefinesResource.PutFeefinesByFeefineIdResponse(responseBuilder.build());
        }

        /**
         * Item with a given ID not found e.g. "feefine not found"
         * 
         * 
         * @param entity
         *     "feefine not found"
         *     
         */
        public static FeefinesResource.PutFeefinesByFeefineIdResponse withPlainNotFound(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(404).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.PutFeefinesByFeefineIdResponse(responseBuilder.build());
        }

        /**
         * Bad request, e.g. malformed request body or query parameter. Details of the error (e.g. name of the parameter or line/character number with malformed data) provided in the response. e.g. "unable to update feefine -- malformed JSON at 13:4"
         * 
         * 
         * @param entity
         *     "unable to update feefine -- malformed JSON at 13:4"
         *     
         */
        public static FeefinesResource.PutFeefinesByFeefineIdResponse withPlainBadRequest(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(400).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.PutFeefinesByFeefineIdResponse(responseBuilder.build());
        }

        /**
         * Internal server error, e.g. due to misconfiguration e.g. internal server error, contact administrator
         * 
         * @param entity
         *     internal server error, contact administrator
         */
        public static FeefinesResource.PutFeefinesByFeefineIdResponse withPlainInternalServerError(String entity) {
            Response.ResponseBuilder responseBuilder = Response.status(500).header("Content-Type", "text/plain");
            responseBuilder.entity(entity);
            return new FeefinesResource.PutFeefinesByFeefineIdResponse(responseBuilder.build());
        }

    }

}
