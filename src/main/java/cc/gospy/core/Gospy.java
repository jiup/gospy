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

import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetcher;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.pipeline.Pipeline;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processor;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Scheduler;
import cc.gospy.core.scheduler.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Gospy implements Observable {
    private static Logger logger = LoggerFactory.getLogger(Gospy.class);

    private Scheduler scheduler;
    private Fetchers fetcherFactory;
    private Processors processorFactory;
    private Pipelines pipelineFactory;
    private ExecutorService threadPool;
    private ExceptionHandler handler;
    private int visitGapMillis;
    private volatile boolean running;
    private Thread operationChainThread;

    private Gospy(Scheduler scheduler
            , Fetchers fetcherFactory
            , Processors processorFactory
            , Pipelines pipelineFactory
            , ExceptionHandler handler) {
        this.scheduler = scheduler;
        this.fetcherFactory = fetcherFactory;
        this.processorFactory = processorFactory;
        this.pipelineFactory = pipelineFactory;
        this.handler = handler;
        this.visitGapMillis = 0;
        this.running = true;
        this.operationChainThread = newOperationChainThread();
    }

    public Thread newOperationChainThread() {
        return new Thread(() -> {
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
                            Result<?> result = processor.process(task, page);
                            if (result != null) {
                                if (result.getNewTasks() != null) {
                                    Iterator<Task> taskIterator = result.getNewTasks().iterator();
                                    while (taskIterator.hasNext()) {
                                        scheduler.addTask(taskIterator.next());
                                    }
                                }
                                if (result.getData() != null) {
                                    Iterator<Pipeline> pipelineIterator = pipelineFactory.get(result.getType()).iterator();
                                    while (pipelineIterator.hasNext()) {
                                        pipelineIterator.next().pipe(page, result);
                                    }
                                }
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

        // stop pipelines
        pipelineFactory.getAll().forEach(pipeline -> {
            if (pipeline instanceof Closeable) {
                closeCloseable((Closeable) pipeline);
            }
        });

        // stop thread pool
        threadPool.shutdownNow();
        while (!threadPool.isTerminated()) {
        }
        logger.info("Thread pool terminated.");
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
        Task task = new Task(url.indexOf("://") != -1 ? url : "http://".concat(url));
        task.setSkipCheck(true);
        return addTask(task);
    }

    public Gospy setVisitGap(int timeMillis) {
        this.visitGapMillis = timeMillis;
        return this;
    }

    public static Builder custom() {
        return new Builder();
    }

    private Observable getObservableScheduler() {
        if (!(scheduler instanceof Observable)) {
            throw new RuntimeException("Scheduler [" + scheduler.getClass() + "] is not observable");
        }
        return (Observable) scheduler;
    }

    public boolean isObservable() {
        return scheduler instanceof Observable;
    }

    @Override
    public long getTotalTaskInputCount() {
        return getObservableScheduler().getTotalTaskInputCount();
    }

    @Override
    public long getTotalTaskOutputCount() {
        return getObservableScheduler().getTotalTaskOutputCount();
    }

    @Override
    public long getRecodedTaskSize() {
        return getObservableScheduler().getRecodedTaskSize();
    }

    @Override
    public long getCurrentTaskQueueSize() {
        return getObservableScheduler().getCurrentTaskQueueSize();
    }

    @Override
    public long getCurrentLazyTaskQueueSize() {
        return getObservableScheduler().getCurrentLazyTaskQueueSize();
    }

    @Override
    public long getRunningTimeMillis() {
        return getObservableScheduler().getRunningTimeMillis();
    }

    public static class Builder {
        private Scheduler sc = Schedulers.GeneralScheduler.getDefault();
        private Fetchers ff = new Fetchers();
        private Processors pf = new Processors();
        private Pipelines plf = new Pipelines();
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

        public Builder addPipeline(Pipeline pipeline) {
            plf.register(pipeline);
            return this;
        }

        public Gospy build() {
            return new Gospy(sc, ff, pf, plf, eh);
        }

    }

}
