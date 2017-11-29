package com.allardworks.workinator3.mongo;

import com.allardworks.workinator3.contracts.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MongoRepository implements WorkinatorRepository {
    @NonNull
    private final MongoDal dal;

    @NonNull
    private final RebalanceStrategy strategy;

    @Override
    public Assignment getAssignment(ExecutorId executorId) {
        return strategy.getNextAssignment(executorId);
    }

    @Override
    public void releaseAssignment(Assignment assignment) {
        strategy.releaseAssignment(assignment);
    }

    @Override
    public ConsumerRegistration registerConsumer(ConsumerId id) {
        return null;
    }

    @Override
    public void unregisterConsumer(ConsumerRegistration registration) {
    }
}
