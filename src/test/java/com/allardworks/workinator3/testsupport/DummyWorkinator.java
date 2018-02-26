package com.allardworks.workinator3.testsupport;

import com.allardworks.workinator3.commands.CreatePartitionCommand;
import com.allardworks.workinator3.commands.RegisterConsumerCommand;
import com.allardworks.workinator3.commands.ReleaseAssignmentCommand;
import com.allardworks.workinator3.commands.UpdateWorkerStatusCommand;
import com.allardworks.workinator3.contracts.*;

import java.util.List;

public class DummyWorkinator implements Workinator {
    private Assignment next;

    public void setNextAssignment(final Assignment assignment) {
        next = assignment;
    }

    @Override
    public Assignment getAssignment(WorkerStatus executorId) {
        return next;
    }

    @Override
    public void releaseAssignment(ReleaseAssignmentCommand assignment) {

    }

    @Override
    public ConsumerRegistration registerConsumer(RegisterConsumerCommand command) throws ConsumerExistsException {
        return null;
    }

    @Override
    public void unregisterConsumer(ConsumerRegistration registration) {

    }

    @Override
    public void createPartition(CreatePartitionCommand command) throws PartitionExistsException {

    }

    @Override
    public void updateStatus(UpdateWorkerStatusCommand workerStatus) {

    }

    @Override
    public List<PartitionInfo> getPartitions() {
        return null;
    }

    @Override
    public PartitionConfiguration getPartitionConfiguration(String partitionKey) {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
