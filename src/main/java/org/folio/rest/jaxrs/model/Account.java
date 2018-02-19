
package org.folio.rest.jaxrs.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Accounts Schema
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "amount",
    "remaining",
    "accountTransaction",
    "comments",
    "dateCreated",
    "dateUpdated",
    "status",
    "paymentStatus",
    "loanId",
    "materialTypeId",
    "userId",
    "itemId",
    "feeFineId",
    "ownerId",
    "id"
})
public class Account {

    @JsonProperty("amount")
    private Double amount;
    @JsonProperty("remaining")
    private Double remaining;
    @JsonProperty("accountTransaction")
    private Integer accountTransaction;
    @JsonProperty("comments")
    private String comments;
    @JsonProperty("dateCreated")
    private Date dateCreated;
    @JsonProperty("dateUpdated")
    private Date dateUpdated;
    /**
     * 
     */
    @JsonProperty("status")
    @Valid
    private Status status;
    /**
     * 
     */
    @JsonProperty("paymentStatus")
    @Valid
    private PaymentStatus paymentStatus;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("loanId")
    @NotNull
    private String loanId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("materialTypeId")
    @NotNull
    private String materialTypeId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("userId")
    @NotNull
    private String userId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("itemId")
    @NotNull
    private String itemId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("feeFineId")
    @NotNull
    private String feeFineId;
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("ownerId")
    @NotNull
    private String ownerId;
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
     *     The amount
     */
    @JsonProperty("amount")
    public Double getAmount() {
        return amount;
    }

    /**
     * 
     * @param amount
     *     The amount
     */
    @JsonProperty("amount")
    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Account withAmount(Double amount) {
        this.amount = amount;
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

    public Account withRemaining(Double remaining) {
        this.remaining = remaining;
        return this;
    }

    /**
     * 
     * @return
     *     The accountTransaction
     */
    @JsonProperty("accountTransaction")
    public Integer getAccountTransaction() {
        return accountTransaction;
    }

    /**
     * 
     * @param accountTransaction
     *     The accountTransaction
     */
    @JsonProperty("accountTransaction")
    public void setAccountTransaction(Integer accountTransaction) {
        this.accountTransaction = accountTransaction;
    }

    public Account withAccountTransaction(Integer accountTransaction) {
        this.accountTransaction = accountTransaction;
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

    public Account withComments(String comments) {
        this.comments = comments;
        return this;
    }

    /**
     * 
     * @return
     *     The dateCreated
     */
    @JsonProperty("dateCreated")
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * 
     * @param dateCreated
     *     The dateCreated
     */
    @JsonProperty("dateCreated")
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Account withDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
        return this;
    }

    /**
     * 
     * @return
     *     The dateUpdated
     */
    @JsonProperty("dateUpdated")
    public Date getDateUpdated() {
        return dateUpdated;
    }

    /**
     * 
     * @param dateUpdated
     *     The dateUpdated
     */
    @JsonProperty("dateUpdated")
    public void setDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public Account withDateUpdated(Date dateUpdated) {
        this.dateUpdated = dateUpdated;
        return this;
    }

    /**
     * 
     * @return
     *     The status
     */
    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    /**
     * 
     * @param status
     *     The status
     */
    @JsonProperty("status")
    public void setStatus(Status status) {
        this.status = status;
    }

    public Account withStatus(Status status) {
        this.status = status;
        return this;
    }

    /**
     * 
     * @return
     *     The paymentStatus
     */
    @JsonProperty("paymentStatus")
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    /**
     * 
     * @param paymentStatus
     *     The paymentStatus
     */
    @JsonProperty("paymentStatus")
    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Account withPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     * @return
     *     The loanId
     */
    @JsonProperty("loanId")
    public String getLoanId() {
        return loanId;
    }

    /**
     * 
     * (Required)
     * 
     * @param loanId
     *     The loanId
     */
    @JsonProperty("loanId")
    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public Account withLoanId(String loanId) {
        this.loanId = loanId;
        return this;
    }

    /**
     * 
     * (Required)
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
     * (Required)
     * 
     * @param materialTypeId
     *     The materialTypeId
     */
    @JsonProperty("materialTypeId")
    public void setMaterialTypeId(String materialTypeId) {
        this.materialTypeId = materialTypeId;
    }

    public Account withMaterialTypeId(String materialTypeId) {
        this.materialTypeId = materialTypeId;
        return this;
    }

    /**
     * 
     * (Required)
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
     * (Required)
     * 
     * @param userId
     *     The userId
     */
    @JsonProperty("userId")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Account withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     * @return
     *     The itemId
     */
    @JsonProperty("itemId")
    public String getItemId() {
        return itemId;
    }

    /**
     * 
     * (Required)
     * 
     * @param itemId
     *     The itemId
     */
    @JsonProperty("itemId")
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Account withItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     * @return
     *     The feeFineId
     */
    @JsonProperty("feeFineId")
    public String getFeeFineId() {
        return feeFineId;
    }

    /**
     * 
     * (Required)
     * 
     * @param feeFineId
     *     The feeFineId
     */
    @JsonProperty("feeFineId")
    public void setFeeFineId(String feeFineId) {
        this.feeFineId = feeFineId;
    }

    public Account withFeeFineId(String feeFineId) {
        this.feeFineId = feeFineId;
        return this;
    }

    /**
     * 
     * (Required)
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
     * (Required)
     * 
     * @param ownerId
     *     The ownerId
     */
    @JsonProperty("ownerId")
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Account withOwnerId(String ownerId) {
        this.ownerId = ownerId;
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

    public Account withId(String id) {
        this.id = id;
        return this;
    }

}
