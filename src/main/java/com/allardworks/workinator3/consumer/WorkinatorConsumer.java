package com.allardworks.workinator3.consumer;

import com.allardworks.workinator3.commands.RegisterConsumerCommand;
import com.allardworks.workinator3.commands.UpdateWorkersStatusCommand;
import com.allardworks.workinator3.contracts.*;
import com.allardworks.workinator3.core.ServiceBase;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
@Slf4j
public class WorkinatorConsumer extends ServiceBase {
    /**
     * Configuration for this consumer.
     */
    @NonNull
    private final ConsumerConfiguration configuration;

    /**
     * The workinator. Provides the partition assignments per worker.
     */
    @NonNull
    private final Workinator workinator;

    /**
     * Creates the executors.
     */
    @NonNull
    private final ExecutorFactory executorFactory;

    /**
     * Creates the workers.
     */
    @NonNull
    private final WorkerFactory workerFactory;

    /**
     * The ID of this consumer.
     */
    @NonNull
    private final ConsumerId consumerId;

    /**
     * The consumer's registration. Returned by the workinator's register method.
     */
    private ConsumerRegistration registration;

    /**
     * One executor per worker.
     */
    private List<ExecutorAsync> executors;

    /**
     * Tracks how many executors have stopped. When 0, all done. Fire the stopped event.
     */
    private CountDownLatch startCount;

    /**
     * Tracks how many executors have started. When 0, all done. Fire the started event.
     */
    private CountDownLatch stopCount;

    private ScheduledTaskThread maintenanceThread;

    @Override
    public void start() {
        getServiceStatus().initialize(s -> {
            s.getEventHandlers().onPostStarting(t -> {
                // setup and run the maintenance thread.
                // when the maintenance thread stops, set it's reference to null.
                maintenanceThread = new ScheduledTaskThread(Duration.ofSeconds(25), this::updateWorkerStatuses);
                maintenanceThread.getTransitionEventHandlers().onPostStopped(maintenanceTransition -> maintenanceThread = null);
                maintenanceThread.start();

                startCount = new CountDownLatch(configuration.getMaxExecutorCount());
                stopCount = new CountDownLatch(configuration.getMaxExecutorCount());
                try {
                    // TODO: if consumer already exists, or other exception,
                    // then the wc is stuck in starting. need to work out proper error handling.
                    setupConsumer();
                    setupAndStartExecutors();
                } catch (final ConsumerExistsException e) {
                    log.error("Consumer Exists", e);
                }
            });

            s.getEventHandlers().onPostStopping(t -> {
                maintenanceThread.stop();
                maintenanceThread = null;
                for (val executor : executors) {
                    executor.stop();
                }
            });
        });
        super.start();
    }

    private void updateWorkerStatuses() {
        val ex = executors;
        if (ex == null) {
            // occurs during startup.
            // timer fires before executors have been initialized.
            return;
        }

        val statuses = ex.stream().map(ExecutorAsync::getWorkerStatus).collect(toList());
        workinator.updateStatus(new UpdateWorkersStatusCommand(statuses));
        System.out.println("\n\n" + LocalDateTime.now() + " updated worker statues " + statuses.size());
    }

    public Map<String, Object> getInfo() {
        val map = new HashMap<String, Object>();
        map.put("consumerId", consumerId);
        map.put("serviceStatus", getStatus().toString());

        val executorInfo = executors.stream().map(Service::getInfo).collect(toList());
        map.put("executors", executorInfo);
        return map;
    }

    /**
     * Create an executor for each worker.
     */
    private void setupAndStartExecutors() {
        // createPartitions the worker ids
        val executorIds = IntStream
                .range(0, configuration.getMaxExecutorCount())
                .mapToObj(i -> new WorkerId(registration, i))
                .collect(toList());

        executors = new ArrayList<>();
        for (val ex : executorIds) {
            val executor = executorFactory.createExecutor(ex, workerFactory);
            executors.add(executor);

            // createPartitions start and stop events
            executor.getTransitionEventHandlers().onPostStarted(t -> onExecutorStarted());
            executor.getTransitionEventHandlers().onPostStopped(t -> onExecutorStopped());

            // start the executor
            executor.start();
        }
    }

    /**
     * Register this consumer with the workinator.
     */
    private void setupConsumer() throws ConsumerExistsException {
        val command = RegisterConsumerCommand.builder().id(consumerId).build();
        registration = workinator.registerConsumer(command);
        if (registration == null) {
            throw new RuntimeException("Critical problem. Registration came back null. Doh!");
        }
    }

    /**
     * Event handler for executor.started.
     */
    private void onExecutorStarted() {
        startCount.countDown();
        if (startCount.getCount() == 0) {
            // when all executors are started, then the service is started.
            // until then, it is starting.
            getServiceStatus().started();
        }
    }

    /**
     * Event handler for executor.stopped.
     */
    private void onExecutorStopped() {
        stopCount.countDown();
        if (stopCount.getCount() == 0) {
            // when all executors are stopped, then the service is stopped.
            // until then, it is stopping.
            cleanupExecutors();
            getServiceStatus().stopped();
        }
    }

    /**
     * close the executors.
     * Clear the list when done.
     */
    private void cleanupExecutors() {
        for (val e : executors) {
            try {
                e.close();
            } catch (Exception ex) {
                log.error("Closing executor", ex);
            }
        }
        executors.clear();
    }
}
