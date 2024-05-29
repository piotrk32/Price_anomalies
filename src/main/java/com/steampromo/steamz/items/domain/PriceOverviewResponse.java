package com.steampromo.steamz.items.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceOverviewResponse {
    private boolean success;
    @JsonProperty("lowest_price")
    private String lowestPrice;
    @JsonProperty("median_price")
    private String medianPrice; }

