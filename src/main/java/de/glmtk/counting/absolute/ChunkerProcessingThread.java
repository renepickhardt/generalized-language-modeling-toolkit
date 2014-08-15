package de.glmtk.counting.absolute;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.glmtk.counting.absolute.Chunker.ReadQueueItem;
import de.glmtk.counting.absolute.Chunker.WriteQueueItem;
import de.glmtk.utils.StatisticalNumberHelper;

public class ChunkerProcessingThread implements Runnable {

    private static final long QUEUE_WAIT_TIME = 1;

    private static final Logger LOGGER = LogManager
            .getLogger(ChunkerProcessingThread.class);

    private Chunker chunker;

    private BlockingQueue<ReadQueueItem> readQueue;

    private BlockingQueue<WriteQueueItem> writeQueue;

    public ChunkerProcessingThread(
            Chunker chunker,
            BlockingQueue<ReadQueueItem> readQueue,
            BlockingQueue<WriteQueueItem> writeQueue) {
        this.chunker = chunker;
        this.readQueue = readQueue;
        this.writeQueue = writeQueue;
    }

    @Override
    public void run() {
        try {
            while (!(chunker.isReadingDone() && readQueue.isEmpty())) {
                ReadQueueItem ritem =
                        readQueue.poll(QUEUE_WAIT_TIME, TimeUnit.MILLISECONDS);
                if (ritem == null) {
                    LOGGER.trace("ChunkerProcessingThread idle, because read queue empty.");
                    StatisticalNumberHelper
                    .count("Idle ChunkerProcessingThread because ReadQueue empty");
                    continue;
                }

                WriteQueueItem witem =
                        new WriteQueueItem(ritem.pattern, ritem.pattern.apply(
                                ritem.words, ritem.poses));
                while (!writeQueue.offer(witem, QUEUE_WAIT_TIME,
                        TimeUnit.MILLISECONDS)) {
                    LOGGER.trace("ChunkerProcessingThread idle, because WriteQueue full.");
                    StatisticalNumberHelper
                    .count("Idle ChunkerProcessingThread cecause WriteQueue full.");
                }

                // To get memory average of WriteQueueItem. Don't forget to:
                // - add import
                // - uncomment classmexer.jar in pom
                // - add javaagent in run.sh to MAVEN_OPTS.
                //StatisticalNumberHelper.average("WriteQueueItem", MemoryUtil
                //        .deepMemoryUsageOf(witem, VisibilityFilter.ALL));
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }

        chunker.processingIsDone();
        LOGGER.debug("ChunkerProcessingThread finished.");
    }

}
