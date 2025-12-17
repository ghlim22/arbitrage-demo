package org.example.info.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.example.util.Pair;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cycleLog")
public class CycleLog {
    @Id
    private String id;

    @Indexed(unique = true)
    private String path;

    private List<TimeExpectedPair> logs; // time in milliseconds : expected return
}


