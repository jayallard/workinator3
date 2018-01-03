package com.allardworks.workinator3.demo;

import com.allardworks.workinator3.contracts.ConsumerConfiguration;
import com.allardworks.workinator3.contracts.ConsumerId;
import com.allardworks.workinator3.mongo.MongoConfiguration;
import com.allardworks.workinator3.mongo.MongoDal;
import com.allardworks.workinator3.mongo.RebalanceStrategy;
import com.allardworks.workinator3.mongo.WhatsNextRebalanceStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    public MongoConfiguration getMongoConfiguration() {
        return MongoConfiguration.builder().partitionType("test").build();
    }

    @Bean
    public ConsumerConfiguration getConsumerConfiguration() {
        return ConsumerConfiguration.builder().build();
    }

    @Bean
    @Autowired
    public RebalanceStrategy getRebalanceStrategy(final MongoDal dal) {
        return new WhatsNextRebalanceStrategy(dal);
    }
}
