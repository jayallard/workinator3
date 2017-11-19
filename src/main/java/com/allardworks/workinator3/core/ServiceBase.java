package com.allardworks.workinator3.core;

import com.allardworks.workinator3.contracts.Service;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public abstract class ServiceBase implements Service {
    private final ServiceStatus status = new ServiceStatus();
    private final List<Consumer<Service>> startedEvents = new ArrayList<>();
    private final List<Consumer<Service>> stoppedEvents = new ArrayList<>();

    public Status getStatus() {
        return status.getStatus();
    }

    @Override
    public Service start() {
        status.start(() -> {
            startService(() -> {
                status.startComplete();
                executeHandlers(startedEvents);
            });
        });
        return this;
    }

    @Override
    public Service stop() {
        status.stop(() -> {
            stopService(() -> {
                status.stopComplete();
                executeHandlers(stoppedEvents);
            });
        });
        return this;
    }

    @Override
    public Service onStopped(@NonNull Consumer<Service> sender) {
        stoppedEvents.add(sender);
        return this;
    }

    @Override
    public Service onStarted(@NonNull Consumer<Service> sender) {
        startedEvents.add(sender);
        return this;
    }

    @Override
    public Service unsubscribe() {
        stoppedEvents.clear();
        startedEvents.clear();
        return this;
    }

    @Override
    public void close() {
    }

    protected abstract void startService(Runnable onStartComplete);

    protected abstract void stopService(Runnable onStopComplete);

    private void executeHandlers(List<Consumer<Service>> events) {
        // TODO: clone events list
        for (val evt : events) {
            try {
                evt.accept(this);
            } catch (final Exception e) {
                log.error("Error executing event handler", e);
            }
        }
    }
}
