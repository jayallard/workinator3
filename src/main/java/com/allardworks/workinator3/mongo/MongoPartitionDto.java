package com.allardworks.workinator3.mongo;

import com.allardworks.workinator3.contracts.PartitionDto;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.UUID;

public class MongoPartitionDto extends PartitionDto {
    @BsonId
    public UUID id;
}