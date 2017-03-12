/*
 * Copyright 2017 ZhangJiupeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.gospy.core;

import cc.gospy.core.fetcher.Fetcher;
import cc.gospy.core.fetcher.FetcherFactory;
import cc.gospy.core.processor.Processor;
import cc.gospy.core.processor.ProcessorFactory;
import cc.gospy.core.scheduler.Scheduler;
import cc.gospy.core.scheduler.SchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Gospy implements Observable {
    private static Logger logger = LoggerFactory.getLogger(Gospy.class);

    private Scheduler scheduler;
    private FetcherFactory fetcherFactory;
    private ProcessorFactory processorFactory;
    private ExecutorService threadPool;
    private ExceptionHandler handler;
    private int visitGapMillis;
    private volatile boolean running;
    private Thread operationChainThread;

    private Gospy(Scheduler scheduler
            , FetcherFactory fetcherFactory
            , ProcessorFactory processorFactory
            , ExceptionHandler handler) {
        this.scheduler = scheduler;
        this.fetcherFactory = fetcherFactory;
        this.processorFactory = processorFactory;
        this.handler = handler;
        this.visitGapMillis = 0;
        this.running = true;
        this.operationChainThread = new Thread(() -> {
            Task t0;
            while (running) {
                while ((t0 = scheduler.getTask()) != null) {
                    Task task = t0;
                    threadPool.execute(() -> {
                        Page page = null;
                        try {
                            Fetcher fetcher = fetcherFactory.get(task.getProtocol());
                            page = fetcher.fetch(task);
                            Processor processor = processorFactory.get(page.getContentType());
                            Collection<Task> tasks = processor.process(task, page);
                            if (tasks != null) {
                                tasks.forEach(e -> scheduler.addTask(e));
                            }
                        } catch (Throwable e) {
                            handler.exceptionCaught(e, task, page);
                        }
                    });
                }
                try {
                    Thread.sleep(visitGapMillis);
                } catch (InterruptedException e) {
                    handler.exceptionCaught(e, null, null);
                }
            }
            logger.info("Operation chain stopped.");
        });
    }

    public void start() {
        this.start(10);
    }

    public void start(int nThreads) {
        if (threadPool != null) {
            throw new RuntimeException("Gospy has already started.");
        }
        this.threadPool = Executors.newFixedThreadPool(nThreads);
        logger.info("Thread pool initialized. [size={}]", nThreads);
        operationChainThread.start();
    }

    public void stop() {
        //stop scheduler
        this.running = false;
        synchronized (this) {
            notifyAll();
        }
        this.scheduler.stop();

        //stop fetchers
        fetcherFactory.getAll().forEach(fetcher -> {
            if (fetcher instanceof Closeable) {
                closeCloseable((Closeable) fetcher);
            }
        });

        // stop processors
        processorFactory.getAll().forEach(processor -> {
            if (processor instanceof Closeable) {
                closeCloseable((Closeable) processor);
            }
        });

        // stop thread pool
        threadPool.shutdownNow();
        while (!threadPool.isTerminated()) {
        }
        logger.info("Thread pool stopped.");
        threadPool = null;
    }

    private void closeCloseable(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public Gospy addTask(Task task) {
        scheduler.addTask(task);
        return this;
    }

    public Gospy addTask(String url) {
        return addTask(new Task(url));
    }

    public Gospy setVisitGap(int timeMillis) {
        this.visitGapMillis = timeMillis;
        return this;
    }

    public static Builder custom() {
        return new Builder();
    }

    @Override
    public long getTotalTaskInputCount() {
        return scheduler instanceof Observable ? ((Observable) scheduler).getTotalTaskInputCount() : -1;
    }

    @Override
    public long getTotalTaskOutputCount() {
        return scheduler instanceof Observable ? ((Observable) scheduler).getTotalTaskOutputCount() : -1;
    }

    @Override
    public long getRecodedTaskSize() {
        return scheduler instanceof Observable ? ((Observable) scheduler).getRecodedTaskSize() : -1;
    }

    @Override
    public long getCurrentTaskQueueSize() {
        return scheduler instanceof Observable ? ((Observable) scheduler).getCurrentTaskQueueSize() : -1;
    }

    @Override
    public long getCurrentLazyTaskQueueSize() {
        return scheduler instanceof Observable ? ((Observable) scheduler).getCurrentLazyTaskQueueSize() : -1;
    }

    @Override
    public long getRunningTimeMillis() {
        return scheduler instanceof Observable ? ((Observable) scheduler).getRunningTimeMillis() : -1;
    }

    public static class Builder {
        private Scheduler sc = SchedulerFactory.GeneralScheduler.getDefault();
        private FetcherFactory ff = new FetcherFactory();
        private ProcessorFactory pf = new ProcessorFactory();
        private ExceptionHandler eh = ExceptionHandler.DEFAULT;

        public Builder setScheduler(Scheduler scheduler) {
            sc = scheduler;
            return this;
        }

        public Builder setExceptionHandler(ExceptionHandler handler) {
            eh = handler;
            return this;
        }

        public Builder addFetcher(Fetcher fetcher) {
            ff.register(fetcher);
            return this;
        }

        public Builder addProcessor(Processor processor) {
            pf.register(processor);
            return this;
        }

        public Gospy build() {
            return new Gospy(sc, ff, pf, eh);
        }

    }

}
