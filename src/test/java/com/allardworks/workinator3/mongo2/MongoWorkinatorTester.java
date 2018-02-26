package com.allardworks.workinator3.mongo2;

import com.allardworks.workinator3.WorkinatorTester;
import com.allardworks.workinator3.contracts.Workinator;
import lombok.val;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.allardworks.workinator3.mongo2.DocumentUtility.doc;

public class MongoWorkinatorTester implements WorkinatorTester {
    private MongoDal dal;

    @Override
    public Workinator getWorkinator() {
        dal = new MongoDal(MongoConfiguration
                .builder()
                .databaseName("test")
                .build());
        val cache = new PartitionConfigurationCache(dal);
        return new MongoWorkinator(dal, cache, new WhatsNextAssignmentStrategy(dal, cache));
    }

    public void setHasWork(final String partitionKey, final boolean hasWork) {
        val find = doc("partitionKey", partitionKey);
        val update = doc("$set", doc("hasWork", hasWork));
        dal.getPartitionsCollection().findOneAndUpdate(find, update);
    }

    public void setDueDate(final String partitionKey, final Date dueDate) {
        val find = doc("partitionKey", partitionKey);
        val update = doc("$set", doc("dueDate", dueDate));
        dal.getPartitionsCollection().findOneAndUpdate(find, update);
    }

    public void setDueDateFuture(final String partitionKey) {
        val future = new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime();
        setDueDate(partitionKey, future);
    }

    @Override
    public void close() throws Exception {
        dal.getDatabase().drop();
    }
}
