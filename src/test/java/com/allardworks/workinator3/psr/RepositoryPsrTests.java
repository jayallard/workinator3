package com.allardworks.workinator3.psr;


import com.allardworks.workinator3.contracts.ConsumerId;
import com.allardworks.workinator3.contracts.ConsumerRegistration;
import com.allardworks.workinator3.contracts.ExecutorId;
import com.allardworks.workinator3.testsupport.RepositoryTester;
import com.allardworks.workinator3.testsupport.TimedActivity;
import lombok.val;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public abstract class RepositoryPsrTests {
    public abstract RepositoryTester getRepoTester();

    private final int PartitionCount = 20000;

    @Test
    public void createManyPartitions() throws Exception {
        try (val tester = getRepoTester()) {
            try (val timer = new TimedActivity("createManyPartitions " + PartitionCount)) {
                tester.createPartitions(PartitionCount);
            }
        }
    }

    @Test
    public void lockAll_Rule1() throws Exception {
        try (val tester = getRepoTester()) {
            try (val timer = new TimedActivity("lockAll.createPartitions " + PartitionCount)) {
                tester.createPartitions(PartitionCount);
            }

            val repo = tester.getRepository();
            val workerId = new ExecutorId(new ConsumerRegistration(new ConsumerId("boo"), ""), 1);

            try (val timer = new TimedActivity("lockAll.lock " + PartitionCount)) {
                val keys = new HashSet<String>();
                for (int i = 0; i < PartitionCount; i++) {
                    val assignment = repo.getAssignment(workerId);
                    keys.add(assignment.getPartition().getPartitionKey());
                }
                assertEquals(PartitionCount, keys.size());
            }
        }
    }

    @Test
    public void lockAndReleaseAll_Rule1() throws Exception {
        try (val tester = getRepoTester()) {
            try (val timer = new TimedActivity("lockAndReleaseAll.createPartitions " + PartitionCount)) {
                tester.createPartitions(PartitionCount);
            }

            val repo = tester.getRepository();
            val workerId = new ExecutorId(new ConsumerRegistration(new ConsumerId("boo"), ""), 1);

            try (val timer = new TimedActivity("lockAndReleaseAll.lock " + PartitionCount)) {
                val keys = new HashSet<String>();
                for (int i = 0; i < PartitionCount; i++) {
                    val assignment = repo.getAssignment(workerId);
                    keys.add(assignment.getPartition().getPartitionKey());
                    repo.releaseAssignment(assignment);
                }
                assertEquals(PartitionCount, keys.size());
            }
        }
    }}
