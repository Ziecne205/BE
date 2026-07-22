package com.parking.config.mongo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Bo dem tu tang cho tung collection (thay cho IDENTITY cua SQL Server).
 * _id = ten collection, seq = gia tri hien tai.
 */
@Document(collection = "database_sequences")
@Getter
@Setter
public class DatabaseSequence {

    @Id
    private String id;

    private long seq;
}
