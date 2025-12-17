package org.example.upbit;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderbookUnit(@JsonProperty("ask_price") double askPrice, @JsonProperty("bid_price") double bidPrice, @JsonProperty("ask_size") double askSize, @JsonProperty("bid_size") double bidSize) {
}
