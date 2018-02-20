package com.allardworks.workinator3.mongo2;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Getter
@Builder
public class MongoConfiguration {
    private final String host;
    private final int port;
    private final String databaseName;
    private final String partitionsCollectionName;
    private final String consumersCollectionName;

    public static class MongoConfigurationBuilder {
        private String host = "localhost";
        private int port = 27017;
        private String databaseName = "Workinator";
        private String partitionsCollectionName = "Partitions";
        private String consumersCollectionName = "Consumers";
    }
}
