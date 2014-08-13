package de.glmtk.counting;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.counting.Sequencer.ReadQueueItem;
import de.glmtk.counting.Sequencer.WriteQueueItem;

public class SequencerCalculateTask implements Runnable {

    private static final Logger LOGGER = LogManager
            .getLogger(SequencerCalculateTask.class);

    private Sequencer sequencer;

    private BlockingQueue<ReadQueueItem> readQueue;

    private BlockingQueue<WriteQueueItem> writeQueue;

    public SequencerCalculateTask(
            Sequencer sequencer,
            BlockingQueue<ReadQueueItem> readQueue,
            BlockingQueue<WriteQueueItem> writeQueue) {
        this.sequencer = sequencer;
        this.readQueue = readQueue;
        this.writeQueue = writeQueue;
    }

    @Override
    public void run() {
        try {
            while (!(sequencer.isReadingDone() && readQueue.isEmpty())) {
                ReadQueueItem ritem = readQueue.poll(10, TimeUnit.MILLISECONDS);
                if (ritem != null) {
                    WriteQueueItem witem = new WriteQueueItem();
                    witem.pattern = ritem.pattern;
                    witem.sequence =
                            ritem.pattern.apply(ritem.words, ritem.poses);
                    writeQueue.put(witem);

                    // To get memory average of WriteQueueItem. Don't forget to:
                    // - add import
                    // - uncomment classmexer.jar in pom
                    // - add javaagent in run.sh to MAVEN_OPTS.
                    //StatisticalNumberHelper.add("WriteQueueItem", MemoryUtil
                    //        .deepMemoryUsageOf(witem, VisibilityFilter.ALL));
                }
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        sequencer.calculatingDone();
        LOGGER.debug("Sequencer calculating done.");
    }

}
