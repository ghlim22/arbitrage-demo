package org.example.graph;

import org.example.util.Pair;
import org.example.upbit.UpbitMarket;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record Graph(List<String> cycle, Long time, List<UpbitMarket> nodes, List<GraphEdge> edges, String path, Double expected) {
    public static Graph of(List<String> cycle, Long time, List<UpbitMarket> nodes, List<Pair<Double, String>> pairs, String path, Double expected) {
       List<GraphEdge> edges = new ArrayList<>();
       for (int i = 0; i < pairs.size(); ++i) {
           String quote = pairs.get(i).second().split("-")[0];
           String type;

           if (Objects.equals(cycle.get(i), quote)) {
               type = "buy";
           } else {
               type = "sell";
           }
           GraphEdge e = new GraphEdge(cycle.get(i), cycle.get(i + 1), pairs.get(i).first(), type);
           edges.add(e);
       }
       return new Graph(cycle, time, nodes, edges, path, expected);
    }
}
