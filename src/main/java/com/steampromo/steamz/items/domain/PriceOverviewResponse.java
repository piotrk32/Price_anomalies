package com.steampromo.steamz.items.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PriceOverviewResponse {
    private boolean success;
    @JsonProperty("lowest_price")
    private String lowestPrice;
    private String volume;
    @JsonProperty("median_price")
    private String medianPrice;
}

