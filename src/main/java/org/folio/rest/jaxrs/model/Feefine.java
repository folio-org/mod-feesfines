
package org.folio.rest.jaxrs.model;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Fee/Fine Schema
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "feeFineType",
    "id",
    "defaultAmount",
    "allowManualCreation",
    "taxVat",
    "ownerId"
})
public class Feefine {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("feeFineType")
    @NotNull
    private String feeFineType;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    @NotNull
    private String id;
    @JsonProperty("defaultAmount")
    private Double defaultAmount;
    @JsonProperty("allowManualCreation")
    private Boolean allowManualCreation;
    @JsonProperty("taxVat")
    private Integer taxVat;
    @JsonProperty("ownerId")
    private String ownerId;

    /**
     * 
     * (Required)
     * 
     * @return
     *     The feeFineType
     */
    @JsonProperty("feeFineType")
    public String getFeeFineType() {
        return feeFineType;
    }

    /**
     * 
     * (Required)
     * 
     * @param feeFineType
     *     The feeFineType
     */
    @JsonProperty("feeFineType")
    public void setFeeFineType(String feeFineType) {
        this.feeFineType = feeFineType;
    }

    public Feefine withFeeFineType(String feeFineType) {
        this.feeFineType = feeFineType;
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

    public Feefine withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * 
     * @return
     *     The defaultAmount
     */
    @JsonProperty("defaultAmount")
    public Double getDefaultAmount() {
        return defaultAmount;
    }

    /**
     * 
     * @param defaultAmount
     *     The defaultAmount
     */
    @JsonProperty("defaultAmount")
    public void setDefaultAmount(Double defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    public Feefine withDefaultAmount(Double defaultAmount) {
        this.defaultAmount = defaultAmount;
        return this;
    }

    /**
     * 
     * @return
     *     The allowManualCreation
     */
    @JsonProperty("allowManualCreation")
    public Boolean getAllowManualCreation() {
        return allowManualCreation;
    }

    /**
     * 
     * @param allowManualCreation
     *     The allowManualCreation
     */
    @JsonProperty("allowManualCreation")
    public void setAllowManualCreation(Boolean allowManualCreation) {
        this.allowManualCreation = allowManualCreation;
    }

    public Feefine withAllowManualCreation(Boolean allowManualCreation) {
        this.allowManualCreation = allowManualCreation;
        return this;
    }

    /**
     * 
     * @return
     *     The taxVat
     */
    @JsonProperty("taxVat")
    public Integer getTaxVat() {
        return taxVat;
    }

    /**
     * 
     * @param taxVat
     *     The taxVat
     */
    @JsonProperty("taxVat")
    public void setTaxVat(Integer taxVat) {
        this.taxVat = taxVat;
    }

    public Feefine withTaxVat(Integer taxVat) {
        this.taxVat = taxVat;
        return this;
    }

    /**
     * 
     * @return
     *     The ownerId
     */
    @JsonProperty("ownerId")
    public String getOwnerId() {
        return ownerId;
    }

    /**
     * 
     * @param ownerId
     *     The ownerId
     */
    @JsonProperty("ownerId")
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Feefine withOwnerId(String ownerId) {
        this.ownerId = ownerId;
        return this;
    }

}
