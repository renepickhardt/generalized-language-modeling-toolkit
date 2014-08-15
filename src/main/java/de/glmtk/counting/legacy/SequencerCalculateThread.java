package de.glmtk.counting.legacy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.counting.legacy.Sequencer.ReadQueueItem;
import de.glmtk.counting.legacy.Sequencer.WriteQueueItem;
import de.glmtk.utils.StatisticalNumberHelper;

public class SequencerCalculateThread implements Runnable {

    private static final long QUEUE_WAIT_TIME = 10;

    private static final Logger LOGGER = LogManager
            .getLogger(SequencerCalculateThread.class);

    private Sequencer sequencer;

    private BlockingQueue<ReadQueueItem> readQueue;

    private BlockingQueue<WriteQueueItem> writeQueue;

    public SequencerCalculateThread(
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
                ReadQueueItem ritem =
                        readQueue.poll(QUEUE_WAIT_TIME, TimeUnit.MILLISECONDS);
                if (ritem == null) {
                    if (LOGGER.getLevel().isLessSpecificThan(Level.TRACE)) {
                        LOGGER.trace("SequencerCalculateThread idle.");
                        StatisticalNumberHelper
                        .count("IdleSequencerCalculateThread");
                    }
                } else {
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
        LOGGER.debug("SequencerCalculateThread finished.");
    }

}
