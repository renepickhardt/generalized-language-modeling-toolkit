package de.glmtk.common;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;


public abstract class AbstractWorkerPriorityExecutor<T> extends
                                                    AbstractWorkerExecutor<T> {
    public AbstractWorkerPriorityExecutor(Config config) {
        super(config);
    }

    @Override
    protected BlockingQueue<T> createQueue(Collection<T> objects) {
        return new PriorityBlockingQueue<>(objects);
    }
}
