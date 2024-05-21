package com.steampromo.steamz.items.domain.item;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public class PriceOverviewResponse {

    @Data
    public class PriceOverviewResponse {
        private boolean success;
        @JsonProperty("lowest_price")
        private String lowestPrice;
        private String volume;
        @JsonProperty("median_price")
        private String medianPrice;
    }
}
