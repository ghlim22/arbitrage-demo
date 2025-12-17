package org.example.upbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.lang.Math;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.graph.Edge;
import org.example.graph.GraphEvent;
import org.example.info.InfoUtil;
import org.example.util.Pair;
import org.example.graph.Graph;
import org.example.event.Event;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpbitClient {

    private final double KRW_MIN_ORDER_AMOUNT = 5000;
    private final double BTC_MIN_ORDER_AMOUNT = 0.00005;
    private final double USDT_MIN_ORDER_AMOUNT = 0.5;

    private final String UPBIT_WEBSOCKET_ENDPOINT = "wss://api.upbit.com/websocket/v1";
    private final int WEBSOCKET_SERVER_PORT = 3001;

    private final List<String> allMarketList = new ArrayList<>();
    private final Map<String, String> symbolNames = new HashMap<>();

    private final Map<String, Map<String, Pair<Double, String>>> swapMatrix = new HashMap<>();
    private final List<List<String>> cycles = new ArrayList<>();
    private final Map<String, Double> marketFee = Map.of("KRW", 0.0005, "BTC", 0.0025, "USDT", 0.0004);
    private final Map<String, List<List<String>>> cyclesMap = new HashMap<>();
    private final Map<String, UpbitMarket> upbitMarketMap = new HashMap<>();
    private final Map<String, Double> minOrderAmount = Map.of("KRW", KRW_MIN_ORDER_AMOUNT, "BTC", BTC_MIN_ORDER_AMOUNT, "USDT", USDT_MIN_ORDER_AMOUNT);

    private final ObjectMapper mapper = new ObjectMapper();

    private final WebSocketServer server = new WebSocketServer(new InetSocketAddress(WEBSOCKET_SERVER_PORT)) {
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
        }

        @Override
        public void onStart() {
        }
    };

    private final WebSocketClient client = new WebSocketClient(URI.create(UPBIT_WEBSOCKET_ENDPOINT)) {
        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            String req = buildAllOrderBookRequest();
            send(req);
        }

        @Override
        public void onMessage(String s) {
            // not used
        }

        @Override
        public void onMessage(ByteBuffer b) {
            try {
                JsonNode root = mapper.readTree(new String(b.array(), StandardCharsets.UTF_8));
                String type = root.get("type").asText();
                if (type.equals("orderbook")) {
                    handleOrderbookMessage(root);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClose(int i, String s, boolean b) {
            System.out.println(s);
            this.reconnect();
        }

        @Override
        public void onError(Exception e) {
            System.out.println(e.toString());
            this.reconnect();
        }

        @Override
        public void reconnect() {
            super.reconnect();
        }
    };

    public UpbitClient() {
        fetchMarketAll();
        buildSwapMatrix();
        buildCycles();

        server.start();
        client.connect();
    }

    private void handleOrderbookMessage(JsonNode root) {
        String code = root.get("code").asText();
        long timestamp = root.get("timestamp").asLong();

        JsonNode orderbookUnits = root.get("orderbook_units");
        double bestAskPrice = orderbookUnits.get(0).get("ask_price").asDouble();
        double bestBidPrice = orderbookUnits.get(0).get("bid_price").asDouble();

        List<OrderbookUnit> orderbook = mapper.convertValue(orderbookUnits, new TypeReference<List<OrderbookUnit>>(){});

        UpbitMarket market = new UpbitMarket(code, timestamp, bestAskPrice, bestBidPrice, orderbook);
        Edge e1 = new Edge(market.from(), market.to(), 1 / bestAskPrice, timestamp);
        Edge e2 = new Edge(market.to(), market.from(), bestBidPrice, timestamp);

        upbitMarketMap.put(code, market);

        Double fee = marketFee.get(market.from());
        var m1 = swapMatrix.get(e1.from());
        m1.put(e1.to(), new Pair<>(Math.log10(e1.exRate() * (1 - fee)), code));
        var m2 = swapMatrix.get(e2.from());
        m2.put(e2.to(), new Pair<>(Math.log10(e2.exRate() * (1 - fee)), code));

        updateCycle(code);
    }

    private String buildAllOrderBookRequest() {
        ArrayNode root = mapper.createArrayNode();
        ObjectNode ticket = mapper.createObjectNode();
        ticket.put("ticket", UUID.randomUUID().toString());

        ObjectNode subscribe = mapper.createObjectNode();
        subscribe.put("type", "orderbook");
        ArrayNode codes = subscribe.putArray("codes");
        allMarketList.forEach((x) -> {
//            codes.add(x + ".1");
            codes.add(x);
        });
        root.add(ticket).add(subscribe);

        ObjectNode format = mapper.createObjectNode();
        format.put("format", "DEFAULT");

        return root.toString();
    }

    private void fetchMarketAll() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String marketAllUri = "https://api.upbit.com/v1/market/all";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(marketAllUri))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            for (JsonNode node : root) {
                String market = node.get("market").asText();
                String[] pair = market.split("-");
                allMarketList.add(market);
                symbolNames.put(pair[1], node.get("english_name").asText());
            }
            symbolNames.put("KRW", "krw");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildSwapMatrix() {
        swapMatrix.put("KRW", new HashMap<>());
        swapMatrix.put("USDT", new HashMap<>());
        swapMatrix.put("BTC", new HashMap<>());
        for (String market : allMarketList) {
            String[] parts = market.split("-");
            if (!swapMatrix.containsKey(parts[1])) {
                swapMatrix.put(parts[1], new HashMap<>());
            }
            swapMatrix.get(parts[0]).put(parts[1], null);
            swapMatrix.get(parts[1]).put(parts[0], null);
        }
    }

    private void buildCycles() {
        for (String start : List.of("KRW", "BTC", "USDT")) {
            Map<String, Pair<Double, String>> m = swapMatrix.get(start);
            for (String k1 : m.keySet()) {
                Map<String, Pair<Double, String>> m2 = swapMatrix.get(k1);
                for (String k2 : m2.keySet()) {
                    if (k2.equals(start) || k2.equals(k1)) {
                        continue;
                    }
                    Map<String, Pair<Double, String>> m3 = swapMatrix.get(k2);
                    if (m3.containsKey(start)) {
                        List<String> cycle = List.of(start, k1, k2, start);
                        cycles.add(cycle);

                        String p1 = start + "-" + k1;
                        String p2 = k1 + "-" + k2;
                        String p3 = start + "-" + k2;
                        String p4 = k2 + "-" + start;
                        String p5 = k2 + "-" + k1;

                        for (String code : List.of(p1, p2, p3, p4, p5)) {
                            if (allMarketList.contains(code)) {
                                if (!cyclesMap.containsKey(code)) {
                                    cyclesMap.put(code, new ArrayList<>());
                                }
                                cyclesMap.get(code).add(cycle);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateCycle(String pair) {
        if (!cyclesMap.containsKey(pair)) {
            return;
        }
        for (List<String> cycle : cyclesMap.get(pair)) {
            findArbitrage(cycle);
        }
    }

    private void findArbitrage(List<String> cycle) {
        String start = cycle.get(0);
        String x1 = cycle.get(1);
        String x2 = cycle.get(2);

        Pair<Double, String> p1 = swapMatrix.get(start).get(x1);
        Pair<Double, String> p2 = swapMatrix.get(x1).get(x2);
        Pair<Double, String> p3 = swapMatrix.get(x2).get(start);
        if (p1 == null || p2 == null || p3 == null) {
            return;
        }

        Double rate1 = p1.first();
        Double rate2 = p2.first();
        Double rate3 = p3.first();

        Double y = rate1 + rate2 + rate3;
        Double expected = (Math.pow(10, y) - 1.0) * 100.0;

        StringBuilder consoleInfo = new StringBuilder();

        long tsMilli = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(tsMilli);
        ZonedDateTime time = instant.atZone(ZoneId.of("Asia/Seoul"));
        consoleInfo.append("------------------------------------------\n");
        consoleInfo.append(String.format("time: %s\n", time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS"))));
        consoleInfo.append(String.format("codes: %s %s %s\n", p1.second(), p2.second(), p3.second()));
        consoleInfo.append(String.format("\n%s | ask: %f | bid: %f\n", p1.second(), upbitMarketMap.get(p1.second()).askPrice(), upbitMarketMap.get(p1.second()).bidPrice()));
        consoleInfo.append(String.format("%s | ask: %f | bid: %f\n", p2.second(), upbitMarketMap.get(p2.second()).askPrice(), upbitMarketMap.get(p2.second()).bidPrice()));
        consoleInfo.append(String.format("%s | ask: %f | bid: %f\n", p3.second(), upbitMarketMap.get(p3.second()).askPrice(), upbitMarketMap.get(p3.second()).bidPrice()));

        consoleInfo.append("\npath: ");

        StringBuilder pathSb = new StringBuilder();
        for (int i = 0; i < cycle.size(); ++i) {
            String name = symbolNames.get(cycle.get(i));
            pathSb.append(name);
            if (i < cycle.size() - 1) {
                pathSb.append(" -> ");
            }
        }
        consoleInfo.append(pathSb);
        String path = pathSb.toString();
        consoleInfo.append(String.format("\nr1: %f, r2: %f, r3: %f%n", rate1, rate2, rate3));
        consoleInfo.append(String.format("expected: %f%%\n", expected));
        consoleInfo.append("------------------------------------------\n");

        List<Pair<Double, String>> pairs = List.of(p1, p2, p3);
        List<UpbitMarket> nodes = new ArrayList<>();
        for (int i = 0; i < pairs.size(); ++i) {
            nodes.add(upbitMarketMap.get(pairs.get(i).second()));
        }

        Graph g = Graph.of(cycle, tsMilli, nodes, pairs, path, expected);

        boolean possible = false;
        if (y > 0.0) {
            try {
                String json = mapper.writeValueAsString(g);
                if (simulateCycle(cycle, pairs)) {
                    possible = true;
//                    LOG.info(g.toString());
                    emitBroadcast("graph", json);
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        InfoUtil.updateCycleLog(new GraphEvent(g, possible));
    }

    private void emitBroadcast(String event, String data) {
        ObjectNode root = mapper.createObjectNode();
        root.put("event", event);
        root.put("data", data);
        String json = root.toPrettyString();
        server.broadcast(json);
    }

    boolean simulateCycle(List<String> cycle, List<Pair<Double, String>> pairs) {
        Map<String, Double> defaultIn = Map.of("KRW", KRW_MIN_ORDER_AMOUNT, "BTC", BTC_MIN_ORDER_AMOUNT, "USDT", USDT_MIN_ORDER_AMOUNT);
        double amount = defaultIn.get(cycle.get(0));
        for (int i = 0; i < pairs.size(); ++i) {
            String[] tmp = pairs.get(i).second().split("-");
            String quote = tmp[0];
            if (quote.equals(cycle.get(i))) {
                amount = simulateBuy(amount, pairs.get(i).second(), quote, tmp[1]);
            } else {
                amount = simulateSell(amount, pairs.get(i).second(), quote, tmp[1]);
            }
        }

        return amount >= defaultIn.get(cycle.get(0));
    }

    double simulateBuy(double in, String pair, String quote, String base) {
        List<OrderbookUnit> orderbook = upbitMarketMap.get(pair).orderbook();
        double remainingQuote = in;
        double acquiredBase = 0.0;

        for (OrderbookUnit unit : orderbook) {
            double maxBaseSize = remainingQuote / unit.askPrice();
            if (maxBaseSize < 1e-9) {
                break;
            }

            double baseToBuy = Math.min(maxBaseSize, unit.askSize());
            double quoteCost = baseToBuy * unit.askPrice();

            acquiredBase += baseToBuy;
            remainingQuote -= quoteCost;
            if (remainingQuote < 1e-15) {
                break;
            }
        }

        return acquiredBase;
    }

    double simulateSell(double in, String pair, String quote, String base) {
        List<OrderbookUnit> orderbook = upbitMarketMap.get(pair).orderbook();
        double remainingBase = in;
        double acquiredQuote = 0.0;

        for (OrderbookUnit unit : orderbook) {
            double baseToSell = Math.min(remainingBase, unit.bidSize());
            double quoteGet = baseToSell * unit.bidPrice();
            acquiredQuote += quoteGet;
            remainingBase -= baseToSell;
            if (Math.abs(remainingBase) < 1e-15) {
                break;
            }
        }

        return acquiredQuote;
    }
}
