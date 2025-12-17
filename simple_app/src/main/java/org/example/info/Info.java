package org.example.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.client.MongoClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.graph.Graph;
import org.example.graph.GraphEvent;
import org.example.info.entity.CycleLog;
import org.example.info.entity.TimeExpectedPair;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
@RestController
public class Info {
    private final MongoTemplate mongoTemplate;
    private final Comparator<CycleUnit> expectedCmp = Comparator.comparing(CycleUnit::getExpected).reversed();
    private final Comparator<CycleUnit> tsCmp = Comparator.comparing(CycleUnit::getTs).reversed();


    @GetMapping("api/cycle/list/possible")
    public ResponseEntity<List<CycleUnit>> getPossibleCycleList() {
        long tsMilli = System.currentTimeMillis();
        List<CycleLog> cycleLogs = mongoTemplate.findAll(CycleLog.class);
        List<CycleUnit> cycleUnits = cycleLogs.stream()
//                .filter((x) -> {
//                    TimeExpectedPair p = x.getLogs().getLast();
//                    return tsMilli - p.ts() <= 10000;
//                })
                .map((x) -> {
                    TimeExpectedPair pair = x.getLogs().getLast();
                    return new CycleUnit(x.getPath(), pair.ts(), pair.expected(), pair.possible());
                })
                .sorted(expectedCmp)
                .toList();
        return ResponseEntity.status(HttpStatus.OK).body(cycleUnits);
    }

    @GetMapping("/api/cycle")
    public ResponseEntity<CycleInfo> getCycleInfo(@RequestParam String path) {
        Query query = new Query(Criteria.where("path").is(path));
        CycleLog cycleLog = mongoTemplate.findOne(query, CycleLog.class);
        if (cycleLog == null) {
            return ResponseEntity.notFound().build();
        }
        List<TimeExpectedPair> allLogs = cycleLog.getLogs()
                .stream()
//                .filter(x-> x.ts() >= start && x.ts() <= end)
                .toList();

        List<ChanceUnit> chances = new ArrayList<>();
//        allLogs.stream()
//                .forEach(x -> {
//                    if (x.possible()) {
//                        chances.add(new ChanceUnit(x.ts(), x.ts(), x.expected()));
//                    }
//                });
        long chanceStart = 0;
        long currentTs = 0;
        double expectedSum = 0.0;
        double expected = 0.0;
        for (TimeExpectedPair p : allLogs) {
            if (chanceStart == 0) {
                if (p.possible()) {
                    chanceStart = p.ts();
                    currentTs = p.ts();
                    expected = p.expected();
                }
            } else {
                if (p.possible()) {
                    expectedSum += expected * (p.ts() - currentTs);
                    currentTs = p.ts();
                    expected = p.expected();
                } else {
                    expectedSum += expected * (p.ts() - currentTs);
                    ChanceUnit cu = new ChanceUnit(chanceStart, p.ts(), expectedSum / (p.ts() - chanceStart));
                    chances.add(cu);
                    chanceStart = 0;
                    expectedSum = 0.0;
                }
            }
        }
        if (chanceStart != 0) {
            long lastTs = allLogs.getLast().ts();
            expectedSum += expected * (lastTs - currentTs);
            ChanceUnit cu = new ChanceUnit(chanceStart, lastTs, expectedSum / (lastTs - chanceStart));
            chances.add(cu);
        }

        return ResponseEntity.ok(new CycleInfo(cycleLog.getPath(), chances));
    }

    public void updateGraphInfo(GraphEvent g) {
//        if (!g.possible()) {
//            return;
//        }
        Graph graph = g.graph();
        Query query = new Query(Criteria.where("path").is(graph.path()));
        Update update = new Update()
                        .setOnInsert("path", graph.path())
                        .push("logs", new TimeExpectedPair(graph.time(), graph.expected(), g.possible()));
        mongoTemplate.upsert(query, update, CycleLog.class);
    }
}

@Getter
@AllArgsConstructor
class CycleInfo {
   public String path;
   public List<ChanceUnit> list;
}

@AllArgsConstructor
class ChanceUnit {
    @JsonProperty
    long start;
    @JsonProperty
    long end;
    @JsonProperty
    double averageExpectedReturn;
};

@Getter
@AllArgsConstructor
class CycleUnit {
    public String path;
    public long ts;
    public double expected;
    public boolean possible;
}
