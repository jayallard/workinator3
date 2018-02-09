package com.allardworks.workinator3.consumer;

import com.allardworks.workinator3.contracts.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Creates an instance of the WorkinatorConsumer.
 * This is a convenience in cases where a single program
 * wants to createPartitions multiple consumers. Usually, that's not how WorkinatorConsumer should
 * be used. But, for tests and demos, you may need multiple.
 * In a usual program, you would just create the beans and inject WorkinatorConsumer. Let spring do the work.
 */
@Component
@RequiredArgsConstructor
public class WorkinatorConsumerFactory {
    @Autowired
    private final ConsumerConfiguration consumerConfiguration;

    @Autowired
    private final Workinator workinator;

    @Autowired
    private final ExecutorFactory executorFactory;

    @Autowired
    private final WorkerFactory workerFactory;

    public WorkinatorConsumer create(final ConsumerId id) {
        return new WorkinatorConsumer(consumerConfiguration, workinator, executorFactory, workerFactory, id);
    }
}
