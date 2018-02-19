
package org.folio.rest.jaxrs.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "feefinehistory",
    "total_records"
})
public class FeefinehistorydataCollection {

    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("feefinehistory")
    @Valid
    @NotNull
    private List<Feefinehistory> feefinehistory = new ArrayList<Feefinehistory>();
    /**
     * 
     * (Required)
     * 
     */
    @JsonProperty("total_records")
    @NotNull
    private Integer totalRecords;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * 
     * (Required)
     * 
     * @return
     *     The feefinehistory
     */
    @JsonProperty("feefinehistory")
    public List<Feefinehistory> getFeefinehistory() {
        return feefinehistory;
    }

    /**
     * 
     * (Required)
     * 
     * @param feefinehistory
     *     The feefinehistory
     */
    @JsonProperty("feefinehistory")
    public void setFeefinehistory(List<Feefinehistory> feefinehistory) {
        this.feefinehistory = feefinehistory;
    }

    public FeefinehistorydataCollection withFeefinehistory(List<Feefinehistory> feefinehistory) {
        this.feefinehistory = feefinehistory;
        return this;
    }

    /**
     * 
     * (Required)
     * 
     * @return
     *     The totalRecords
     */
    @JsonProperty("total_records")
    public Integer getTotalRecords() {
        return totalRecords;
    }

    /**
     * 
     * (Required)
     * 
     * @param totalRecords
     *     The total_records
     */
    @JsonProperty("total_records")
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public FeefinehistorydataCollection withTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public FeefinehistorydataCollection withAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
        return this;
    }

}
