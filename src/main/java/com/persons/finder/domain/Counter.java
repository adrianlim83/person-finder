package com.persons.finder.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "counters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Counter {
    @Id
    private String id; // e.g., "userId"
    private long seq;
}
