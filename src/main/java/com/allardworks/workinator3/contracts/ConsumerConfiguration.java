package com.allardworks.workinator3.contracts;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Builder
@Getter
public class ConsumerConfiguration {
    @NonNull
    private final Duration minWorkTime = Duration.ofSeconds(30);

    private final int maxExecutorCount;

    public static class ConsumerConfigurationBuilder {
        private int maxExecutorCount = 1;
    }
}