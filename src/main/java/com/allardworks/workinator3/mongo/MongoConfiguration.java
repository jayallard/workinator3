package com.allardworks.workinator3.mongo;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Builder
public class MongoConfiguration {
    @NonNull
    private final String partitionType;
    @NonNull
    private final String host;
    private final int port;
    private final String databaseName;

    public String getPartitionsCollectionName() {
        return "Partitions_" + partitionType;
    }

    public String getWorkersCollectionName() {
        return "Workers_" + partitionType;
    }

    public static class MongoConfigurationBuilder {
        private String databaseName = "Workinator";
        private String host = "localhost";
        private int port = 27017;
    }
}
