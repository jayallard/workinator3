package com.allardworks.workinator3.demo;

import com.allardworks.workinator3.commands.CreatePartitionCommand;
import com.allardworks.workinator3.consumer.WorkinatorConsumer;
import com.allardworks.workinator3.consumer.WorkinatorConsumerFactory;
import com.allardworks.workinator3.contracts.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.cli.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

@Service
@RequiredArgsConstructor
public class Runner implements CommandLineRunner {
    private final Workinator workinator;
    private final WorkinatorConsumerFactory consumerFactory;
    private final Map<String, WorkinatorConsumer> consumers = new HashMap<>();

    /**
     * Create a partition.
     *
     * @param command
     * @throws PartitionExistsException
     */
    private boolean createPartition(final CommandLine command) throws PartitionExistsException {
        val partitionName = command.getOptionValue("cp");
        if (partitionName == null) {
            return false;
        }

        val partition = CreatePartitionCommand
                .builder()
                .partitionKey(partitionName)
                .build();
        workinator.createPartition(partition);
        return true;
    }

    private boolean createConsumer(final CommandLine command) {
        val consumerName = command.getOptionValue("cc");
        if (consumerName == null) {
            return false;
        }

        val id = new ConsumerId(consumerName);
        val consumer = consumerFactory.create(id);
        consumer.start();
        consumers.put(consumerName, consumer);
        return true;
    }

    /**
     * Shows the status of the consumers running in process.
     * @param command
     * @return
     * @throws JsonProcessingException
     */
    private boolean showLocalConsumerStatus(final CommandLine command) {
        if (!command.hasOption("scl")) {
            return false;
        }

        for (val c : consumers.values()) {
            val info = c.getInfo();
            val consumerId = (ConsumerId) info.get("consumerId");
            out.println(consumerId.getName());
            val executors = (List<Map<String, Object>>) info.get("executors");
            for (val e : executors) {
                val eid = (WorkerId) e.get("executorId");
                val assignment = (Assignment)e.get("currentAssignment");
                out.print("\t" + eid.getWorkerNumber() + ", Assignment=");
                if (assignment == null) {
                    out.println();
                    continue;
                }

                out.println(assignment.getPartitionKey());
            }
            out.println();
        }
        return true;
    }

    private void showHelp(final Options options) {
        val formatter = new HelpFormatter();
        formatter.printHelp("workinator demo cli", options);
        out.println();
    }

    private boolean showHelp(final CommandLine command, final Options options) {
        if (!command.hasOption("help")) {
            return false;
        }

        showHelp(options);
        return true;
    }

    private boolean showPartitions(final CommandLine command) {
        if (!command.hasOption("sp")) {
            return false;
        }

        val partitions = workinator.getPartitions();
        for (val partition : partitions) {
            out.println("Partition Key=" + partition.getPartitionKey() + ", Max Worker Count=" + partition.getMaxWorkerCount() + ", Last Checked=" + partition.getLastChecked() + ", Current Worker Count=" + partition.getCurrentWorkerCount());
            for (val worker : partition.getWorkers()) {
                out.println("\t" + worker.getAssignee() + ", Rule: " + worker.getRule());
            }
        }
        return true;
    }

    private boolean setPartitionHasWork(final CommandLine command) {
        if (command.hasOption("pwork")) {
            DemoHelper.getHack().setHasWork(command.getOptionValue("pwork"), true);
            out.println("The partition has been set: hasWork=true");
            return true;
        }

        if (command.hasOption("pnwork")) {
            DemoHelper.getHack().setHasWork(command.getOptionValue("pnwork"), false);
            out.println("The partition has been set: hasWork=false");
            return true;
        }

        return false;
    }

    @Override
    public void run(String... strings) {
        val parser = new DefaultParser();
        val options = new Options();
        options.addOption(new Option("cc", "createconsumer", true, "Create a consumer"));
        options.addOption(new Option("cp", "createpartition", true, "Create a partition"));
        options.addOption(new Option("scl", "showconsumerslocal", false, "Display In Process Consumer Information"));
        options.addOption(new Option("help", "help", false, "print this message"));
        options.addOption(new Option("sp", "showpartitions", false, "show partitions"));
        options.addOption(new Option("pwork", "partitionhaswork", true, "for emulation: indicate that a partition has work."));
        options.addOption(new Option("pnwork", "partitionnowork", true, "for emulation: indicate that a partition doesn't have work."));
        while (true) {
            try {
                val command = parser.parse(options, getInput());
                val processed =
                        createPartition(command)
                                || createConsumer(command)
                                || showLocalConsumerStatus(command)
                                || showHelp(command, options)
                                || setPartitionHasWork(command)
                                || showPartitions(command);

                if (!processed) {
                    showHelp(options);
                }
            } catch (final Exception ex) {
                ex.printStackTrace();
                showHelp(options);
            }
        }
    }

    private String[] getInput() {
        try {
            out.print("Workinator> ");
            return new BufferedReader(new InputStreamReader(System.in)).readLine().split(" ");
        } catch (IOException e) {
            return new String[]{};
        }
    }
}
