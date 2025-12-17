package org.example.upbit;

import java.util.List;

public record UpbitMarket(String code, String from, String to, long timestamp, double askPrice, double bidPrice, List<OrderbookUnit> orderbook) {
    UpbitMarket(String code, long timestamp, double askPrice, double bidPrice, List<OrderbookUnit> orderbook) {
        String[] parts = code.split("-");
        this(code, parts[0], parts[1], timestamp, askPrice, bidPrice, orderbook);
    }
}
