
package org.folio.rest.jaxrs.model;

import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Fee Fine History View Schema
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "dateCreated",
    "dateUpdated",
    "feeFineType",
    "charged",
    "remaining",
    "status",
    "paymentStatus",
    "loanTransaction",
    "item",
    "barcode",
    "itemType",
    "callNumber",
    "feeFineOwner",
    "comments",
    "userId",
    "id"
})
public class Feefinehistory {

    @JsonProperty("dateCreated")
    private String dateCreated;
    @JsonProperty("dateUpdated")
    private String dateUpdated;
    @JsonProperty("feeFineType")
    private String feeFineType;
    @JsonProperty("charged")
    private Double charged;
    @JsonProperty("remaining")
    private Double remaining;
    @JsonProperty("status")
    private String status;
    @JsonProperty("paymentStatus")
    private String paymentStatus;
    @JsonProperty("loanTransaction")
    private Integer loanTransaction;
    @JsonProperty("item")
    private String item;
    @JsonProperty("barcode")
    private String barcode;
    @JsonProperty("itemType")
    private String itemType;
    @JsonProperty("callNumber")
    private String callNumber;
    @JsonProperty("feeFineOwner")
    private String feeFineOwner;
    @JsonProperty("comments")
    private String comments;
    @JsonProperty("userId")
    private String userId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("id")
    @NotNull
    private String id;

    /**
     * 
     * @return
     *     The dateCreated
     */
    @JsonProperty("dateCreated")
    public String getDateCreated() {
        return dateCreated;
    }

    /**
     * 
     * @param dateCreated
     *     The dateCreated
     */
    @JsonProperty("dateCreated")
    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Feefinehistory withDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
        return this;
    }

    /**
     * 
     * @return
     *     The dateUpdated
     */
    @JsonProperty("dateUpdated")
    public String getDateUpdated() {
        return dateUpdated;
    }

    /**
     * 
     * @param dateUpdated
     *     The dateUpdated
     */
    @JsonProperty("dateUpdated")
    public void setDateUpdated(String dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public Feefinehistory withDateUpdated(String dateUpdated) {
        this.dateUpdated = dateUpdated;
        return this;
    }

    /**
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
     * @param feeFineType
     *     The feeFineType
     */
    @JsonProperty("feeFineType")
    public void setFeeFineType(String feeFineType) {
        this.feeFineType = feeFineType;
    }

    public Feefinehistory withFeeFineType(String feeFineType) {
        this.feeFineType = feeFineType;
        return this;
    }

    /**
     * 
     * @return
     *     The charged
     */
    @JsonProperty("charged")
    public Double getCharged() {
        return charged;
    }

    /**
     * 
     * @param charged
     *     The charged
     */
    @JsonProperty("charged")
    public void setCharged(Double charged) {
        this.charged = charged;
    }

    public Feefinehistory withCharged(Double charged) {
        this.charged = charged;
        return this;
    }

    /**
     * 
     * @return
     *     The remaining
     */
    @JsonProperty("remaining")
    public Double getRemaining() {
        return remaining;
    }

    /**
     * 
     * @param remaining
     *     The remaining
     */
    @JsonProperty("remaining")
    public void setRemaining(Double remaining) {
        this.remaining = remaining;
    }

    public Feefinehistory withRemaining(Double remaining) {
        this.remaining = remaining;
        return this;
    }

    /**
     * 
     * @return
     *     The status
     */
    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    /**
     * 
     * @param status
     *     The status
     */
    @JsonProperty("status")
    public void setStatus(String status) {
        this.status = status;
    }

    public Feefinehistory withStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * 
     * @return
     *     The paymentStatus
     */
    @JsonProperty("paymentStatus")
    public String getPaymentStatus() {
        return paymentStatus;
    }

    /**
     * 
     * @param paymentStatus
     *     The paymentStatus
     */
    @JsonProperty("paymentStatus")
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Feefinehistory withPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
        return this;
    }

    /**
     * 
     * @return
     *     The loanTransaction
     */
    @JsonProperty("loanTransaction")
    public Integer getLoanTransaction() {
        return loanTransaction;
    }

    /**
     * 
     * @param loanTransaction
     *     The loanTransaction
     */
    @JsonProperty("loanTransaction")
    public void setLoanTransaction(Integer loanTransaction) {
        this.loanTransaction = loanTransaction;
    }

    public Feefinehistory withLoanTransaction(Integer loanTransaction) {
        this.loanTransaction = loanTransaction;
        return this;
    }

    /**
     * 
     * @return
     *     The item
     */
    @JsonProperty("item")
    public String getItem() {
        return item;
    }

    /**
     * 
     * @param item
     *     The item
     */
    @JsonProperty("item")
    public void setItem(String item) {
        this.item = item;
    }

    public Feefinehistory withItem(String item) {
        this.item = item;
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

    public Feefinehistory withBarcode(String barcode) {
        this.barcode = barcode;
        return this;
    }

    /**
     * 
     * @return
     *     The itemType
     */
    @JsonProperty("itemType")
    public String getItemType() {
        return itemType;
    }

    /**
     * 
     * @param itemType
     *     The itemType
     */
    @JsonProperty("itemType")
    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Feefinehistory withItemType(String itemType) {
        this.itemType = itemType;
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

    public Feefinehistory withCallNumber(String callNumber) {
        this.callNumber = callNumber;
        return this;
    }

    /**
     * 
     * @return
     *     The feeFineOwner
     */
    @JsonProperty("feeFineOwner")
    public String getFeeFineOwner() {
        return feeFineOwner;
    }

    /**
     * 
     * @param feeFineOwner
     *     The feeFineOwner
     */
    @JsonProperty("feeFineOwner")
    public void setFeeFineOwner(String feeFineOwner) {
        this.feeFineOwner = feeFineOwner;
    }

    public Feefinehistory withFeeFineOwner(String feeFineOwner) {
        this.feeFineOwner = feeFineOwner;
        return this;
    }

    /**
     * 
     * @return
     *     The comments
     */
    @JsonProperty("comments")
    public String getComments() {
        return comments;
    }

    /**
     * 
     * @param comments
     *     The comments
     */
    @JsonProperty("comments")
    public void setComments(String comments) {
        this.comments = comments;
    }

    public Feefinehistory withComments(String comments) {
        this.comments = comments;
        return this;
    }

    /**
     * 
     * @return
     *     The userId
     */
    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }

    /**
     * 
     * @param userId
     *     The userId
     */
    @JsonProperty("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Feefinehistory withUserId(String userId) {
        this.userId = userId;
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

    public Feefinehistory withId(String id) {
        this.id = id;
        return this;
    }

}
