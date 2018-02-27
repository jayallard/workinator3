package com.allardworks.workinator3.mongo2;

import com.allardworks.workinator3.contracts.Assignment;
import com.allardworks.workinator3.contracts.WorkerStatus;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bson.Document;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static com.allardworks.workinator3.core.ConvertUtility.toDate;
import static com.allardworks.workinator3.mongo2.DocumentUtility.doc;
import static com.mongodb.client.model.ReturnDocument.AFTER;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Assignment strategy that determines a worker's assignment.
 */
@RequiredArgsConstructor
public class WhatsNextAssignmentStrategy implements AssignmentStrategy {

    private final FindOneAndUpdateOptions updateOptions = new FindOneAndUpdateOptions()
            .returnDocument(AFTER)
            .sort(doc("lastCheckedDate", 1));

    private final MongoDal dal;
    private final PartitionConfigurationCache partitionConfigurationCache;
    private final FindOneAndUpdateOptions releaseOptions = new FindOneAndUpdateOptions().projection(doc("_id", 1));

    /**
     * The WHERE for rule #3.
     * All documents that have work and capacity for more workers.
     */
    private final Document alreadyBeingWorkedOnFilter = Document.parse("{ $and: [ { hasWork: true }, { $expr :  { $lt: [ \"$workerCount\", \"$maxWorkerCount\" ] } } ] }");

    /**
     * The UPDATE options for rule #3.
     * Order by the worker count and the date.
     */
    private final FindOneAndUpdateOptions alreadyBeingWorkedOnUpdateOptions =  new FindOneAndUpdateOptions()
                    .returnDocument(AFTER)
                    .sort(doc("workerCount", 1, "lastCheckedDate", 1));

    /**
     * The WHERE for rule #4.
     * An partition that doesn't have any workers.
     */
    private final Document noWorkers = doc("workerCount", 0);


    @Override
    public Assignment getAssignment(@NonNull final WorkerStatus status) {
        return new StrategyRunner(this, status).getAssignment();
    }

    @Override
    public void releaseAssignment(final Assignment assignment) {
        // due date is now + maxIdleTimeSeconds.
        val dueDate = toDate(LocalDateTime.now().plus(partitionConfigurationCache.getConfiguration(assignment.getPartitionKey()).getMaxIdleTimeSeconds(), SECONDS));
        val findPartition = doc("partitionKey", assignment.getPartitionKey());
        val removeWorker =
                doc("$pull",
                        doc("workers",
                                doc("id", assignment.getWorkerId().getAssignee())),
                        "$inc", doc("workerCount", -1),
                        "$set", doc("lastCheckedDate", new Date(), "dueDate", dueDate));
        val result = dal.getPartitionsCollection().findOneAndUpdate(findPartition, removeWorker, releaseOptions);
    }

    @RequiredArgsConstructor
    private static class StrategyRunner {
        private final WhatsNextAssignmentStrategy strategy;
        private final WorkerStatus status;

        /**
         * The list of methods to execute to determine the assignment.
         * First one wins.
         */
        private final List<Supplier<Assignment>> rules = Arrays.asList(
                this::due,
                this::ifBusyKeepGoing,
                this::alreadyBeingWorkedOn,
                this::anyPartitionWithoutWorkers);

        /**
         * Creates the update document. Most rules need this.
         *
         * @param ruleName
         * @return
         */
        private Document createWorkerUpdateDocument(final String ruleName) {
            return doc("$push",
                    doc("workers",
                            doc("id", status.getWorkerId().getAssignee(),
                                    "insertDate", new Date(),
                                    "rule", ruleName)),
                    "$inc", doc("workerCount", 1, "assignmentCount", 1),
                    "$set", doc("lastCheckedDate", new Date()));
        }

        /**
         * Converts a partition BSON document to an assignment object.
         * @param partition
         * @param status
         * @param ruleName
         * @return
         */
        private Assignment toAssignment(final Document partition, final WorkerStatus status, final String ruleName) {
            if (partition == null) {
                return null;
            }

            return new Assignment(status.getWorkerId(), partition.getString("partitionKey"), "", ruleName);
        }

        /**
         * RULE 1
         * Get the a partition that is due to be processed, and doesn't have any
         * current workers.
         *
         * This is the highest priority. Partitions have a maxIdleTime setting.
         * This enforces that those partitions that are due will have the highest priority.
         *
         * @return
         */
        private Assignment due() {
            val where = doc("workerCount", 0, "dueDate", doc("$lt", new Date()));
            val update = createWorkerUpdateDocument("Rule 1");
            return toAssignment(strategy.dal.getPartitionsCollection()
                    .findOneAndUpdate(where, update, strategy.updateOptions), status, "Rule 1");
        }

        /**
         * RULE 2
         * If the executor is already hasWork, then let it keep doing what it's doing.
         *
         * @return
         */
        private Assignment ifBusyKeepGoing() {
            if (!status.isHasWork()) {
                return status.getCurrentAssignment();
            }

            return null;
        }

        /**
         * RULE 3
         * Return a partition that is already being worked on.
         * Partitions that are being worked on, but support multiple workers.
         * @return
         */
        private Assignment alreadyBeingWorkedOn() {
            val update = createWorkerUpdateDocument("Rule 3");
            return toAssignment(strategy.dal.getPartitionsCollection().findOneAndUpdate(strategy.alreadyBeingWorkedOnFilter, update, strategy.alreadyBeingWorkedOnUpdateOptions), status, "Rule 3");
        }

        /**
         * Rule 4
         * Find any partition that doesn't have a worker, even if it's not due yet.
         * If we have capacity to do work, might as well even though the partition isn't due.
         */
        private Assignment anyPartitionWithoutWorkers() {
            val update = createWorkerUpdateDocument("Rule 4");
            return toAssignment(strategy.dal.getPartitionsCollection().findOneAndUpdate(strategy.noWorkers, update, strategy.updateOptions), status, "Rule 4");
        }

        /**
         * Determines the assignment for the worker.
         * Iterates a list of rules until one returns an assignment.
         * If no rule returns an assignment, then there's nothing to do.
         * @return
         */
        public Assignment getAssignment() {
            for (val rule : rules) {
                val assignment = rule.get();
                if (assignment != null) {
                    return assignment;
                }
            }

            // nothing to do. capacity exceeds the number of partitions.
            return null;
        }
    }
}
