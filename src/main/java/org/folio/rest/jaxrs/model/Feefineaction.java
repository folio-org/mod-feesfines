
package org.folio.rest.jaxrs.model;

import java.util.Date;
import javax.annotation.Generated;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * FeeFineActions Schema
 * <p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "dateAction",
    "typeAction",
    "comment",
    "amount",
    "balance",
    "transactionNumber",
    "feeFineId",
    "userId",
    "id"
})
public class Feefineaction {

    @JsonProperty("dateAction")
    private Date dateAction;
    @JsonProperty("typeAction")
    private String typeAction;
    @JsonProperty("comment")
    private String comment;
    @JsonProperty("amount")
    private Double amount;
    @JsonProperty("balance")
    private Double balance;
    @JsonProperty("transactionNumber")
    private Double transactionNumber;
    @JsonProperty("feeFineId")
    private String feeFineId;
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
     *     The dateAction
     */
    @JsonProperty("dateAction")
    public Date getDateAction() {
        return dateAction;
    }

    /**
     * 
     * @param dateAction
     *     The dateAction
     */
    @JsonProperty("dateAction")
    public void setDateAction(Date dateAction) {
        this.dateAction = dateAction;
    }

    public Feefineaction withDateAction(Date dateAction) {
        this.dateAction = dateAction;
        return this;
    }

    /**
     * 
     * @return
     *     The typeAction
     */
    @JsonProperty("typeAction")
    public String getTypeAction() {
        return typeAction;
    }

    /**
     * 
     * @param typeAction
     *     The typeAction
     */
    @JsonProperty("typeAction")
    public void setTypeAction(String typeAction) {
        this.typeAction = typeAction;
    }

    public Feefineaction withTypeAction(String typeAction) {
        this.typeAction = typeAction;
        return this;
    }

    /**
     * 
     * @return
     *     The comment
     */
    @JsonProperty("comment")
    public String getComment() {
        return comment;
    }

    /**
     * 
     * @param comment
     *     The comment
     */
    @JsonProperty("comment")
    public void setComment(String comment) {
        this.comment = comment;
    }

    public Feefineaction withComment(String comment) {
        this.comment = comment;
        return this;
    }

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

    public Feefineaction withAmount(Double amount) {
        this.amount = amount;
        return this;
    }

    /**
     * 
     * @return
     *     The balance
     */
    @JsonProperty("balance")
    public Double getBalance() {
        return balance;
    }

    /**
     * 
     * @param balance
     *     The balance
     */
    @JsonProperty("balance")
    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Feefineaction withBalance(Double balance) {
        this.balance = balance;
        return this;
    }

    /**
     * 
     * @return
     *     The transactionNumber
     */
    @JsonProperty("transactionNumber")
    public Double getTransactionNumber() {
        return transactionNumber;
    }

    /**
     * 
     * @param transactionNumber
     *     The transactionNumber
     */
    @JsonProperty("transactionNumber")
    public void setTransactionNumber(Double transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public Feefineaction withTransactionNumber(Double transactionNumber) {
        this.transactionNumber = transactionNumber;
        return this;
    }

    /**
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
     * @param feeFineId
     *     The feeFineId
     */
    @JsonProperty("feeFineId")
    public void setFeeFineId(String feeFineId) {
        this.feeFineId = feeFineId;
    }

    public Feefineaction withFeeFineId(String feeFineId) {
        this.feeFineId = feeFineId;
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

    public Feefineaction withUserId(String userId) {
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

    public Feefineaction withId(String id) {
        this.id = id;
        return this;
    }

}
