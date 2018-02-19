
package org.folio.rest.jaxrs.model;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Item Information Schema
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "instance",
    "callNumber",
    "barcode",
    "itemStatus",
    "location",
    "id",
    "materialTypeId"
})
public class Chargeitem {

    @JsonProperty("instance")
    private String instance;
    @JsonProperty("callNumber")
    private String callNumber;
    @JsonProperty("barcode")
    private String barcode;
    @JsonProperty("itemStatus")
    private String itemStatus;
    @JsonProperty("location")
    private String location;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    @NotNull
    private String id;
    @JsonProperty("materialTypeId")
    private String materialTypeId;

    /**
     * 
     * @return
     *     The instance
     */
    @JsonProperty("instance")
    public String getInstance() {
        return instance;
    }

    /**
     * 
     * @param instance
     *     The instance
     */
    @JsonProperty("instance")
    public void setInstance(String instance) {
        this.instance = instance;
    }

    public Chargeitem withInstance(String instance) {
        this.instance = instance;
        return this;
    }

    /**
     * 
     * @return
     *     The callNumber
     */
    @JsonProperty("callNumber")
    public String getCallNumber() {
        return callNumber;
    }

    /**
     * 
     * @param callNumber
     *     The callNumber
     */
    @JsonProperty("callNumber")
    public void setCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }

    public Chargeitem withCallNumber(String callNumber) {
        this.callNumber = callNumber;
        return this;
    }

    /**
     * 
     * @return
     *     The barcode
     */
    @JsonProperty("barcode")
    public String getBarcode() {
        return barcode;
    }

    /**
     * 
     * @param barcode
     *     The barcode
     */
    @JsonProperty("barcode")
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Chargeitem withBarcode(String barcode) {
        this.barcode = barcode;
        return this;
    }

    /**
     * 
     * @return
     *     The itemStatus
     */
    @JsonProperty("itemStatus")
    public String getItemStatus() {
        return itemStatus;
    }

    /**
     * 
     * @param itemStatus
     *     The itemStatus
     */
    @JsonProperty("itemStatus")
    public void setItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
    }

    public Chargeitem withItemStatus(String itemStatus) {
        this.itemStatus = itemStatus;
        return this;
    }

    /**
     * 
     * @return
     *     The location
     */
    @JsonProperty("location")
    public String getLocation() {
        return location;
    }

    /**
     * 
     * @param location
     *     The location
     */
    @JsonProperty("location")
    public void setLocation(String location) {
        this.location = location;
    }

    public Chargeitem withLocation(String location) {
        this.location = location;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     * @return
     *     The id
     */
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    /**
     * 
     * (Required)
     * 
     * @param id
     *     The id
     */
    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public Chargeitem withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * 
     * @return
     *     The materialTypeId
     */
    @JsonProperty("materialTypeId")
    public String getMaterialTypeId() {
        return materialTypeId;
    }

    /**
     * 
     * @param materialTypeId
     *     The materialTypeId
     */
    @JsonProperty("materialTypeId")
    public void setMaterialTypeId(String materialTypeId) {
        this.materialTypeId = materialTypeId;
    }

    public Chargeitem withMaterialTypeId(String materialTypeId) {
        this.materialTypeId = materialTypeId;
        return this;
    }

}
