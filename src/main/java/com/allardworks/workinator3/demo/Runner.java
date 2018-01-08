package com.allardworks.workinator3.demo;

import com.allardworks.workinator3.WorkinatorAdmin;
import com.allardworks.workinator3.consumer.WorkinatorConsumer;
import com.allardworks.workinator3.consumer.WorkinatorConsumerFactory;
import com.allardworks.workinator3.contracts.*;
import com.allardworks.workinator3.core.MapNavigator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.out;

@Service
@RequiredArgsConstructor
public class Runner implements CommandLineRunner {

    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, WorkinatorConsumer> consumers = new HashMap<>();

    @Autowired
    private final WorkinatorAdmin admin;

    @Autowired
    private final WorkinatorConsumerFactory consumerFactory;

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
        admin.createPartition(partition);
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

    private boolean showConsumerStatus(final CommandLine command) throws JsonProcessingException {
        if (!command.hasOption("sc")) {
            return false;
        }

        for (val c : consumers.values()) {
            //out.println(mapper.writeValueAsString(c.getInfo()));
            val info = c.getInfo();
            val consumerId = (ConsumerId) info.get("consumerId");
            out.println(consumerId.getName());
            val executors = (List<Map<String, Object>>) info.get("executors");
            for (val e : executors) {
                val eid = (ExecutorId) e.get("executorId");
                val assignment = (Assignment)e.get("currentAssignment");
                out.print("\t" + eid.getExecutorNumber() + ", Assignment=");
                if (assignment == null) {
                    out.println();
                    continue;
                }
                out.println(assignment.getPartition().getPartitionKey());
            }
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

    private boolean showPartitions(final CommandLine command) throws JsonProcessingException {
        if (!command.hasOption("sp")) {
            return false;
        }

        val partitions = admin.getPartitions();
        for (val partition : partitions) {
            out.println("Partition Key=" + partition.getPartitionKey() + ", Max Worker Count=" + partition.getMaxWorkerCount().getValue() + ", Last Check Complete=" + partition.getLastCheckEnd().getValue());
        }
        //out.println(mapper.writerWithDefaultPrettyPrnter().writeValueAsString(partitions));
        return true;
    }

    @Override
    public void run(String... strings) throws Exception {
        // TODO: refactor this... should just be a list of stuff that works itself out
        val parser = new DefaultParser();
        val options = new Options();
        options.addOption(new Option("cc", "createconsumer", true, "Create a consumer"));
        options.addOption(new Option("cp", "createpartition", true, "Create a partition"));
        options.addOption(new Option("sc", "showconsumers", false, "Display Consumers"));
        options.addOption(new Option("help", "help", false, "print this message"));
        options.addOption(new Option("sp", "showpartitions", false, "show partitions"));
        while (true) {
            try {
                val command = parser.parse(options, getInput());
                val processed =
                        createPartition(command)
                                || createConsumer(command)
                                || showConsumerStatus(command)
                                || showHelp(command, options)
                                || showPartitions(command);

                if (!processed) {
                    showHelp(options);
                }
            } catch (final Exception ex) {
                out.println("  Error: " + ex.getMessage());
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
