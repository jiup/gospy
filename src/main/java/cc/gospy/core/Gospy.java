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
import cc.gospy.core.processor.*;
import cc.gospy.core.scheduler.Observable;
import cc.gospy.core.scheduler.*;
import cc.gospy.core.util.Experimental;
import cc.gospy.core.util.LoggerHelper;
import cc.gospy.core.util.StringHelper;
import cc.gospy.core.util.TaskBlockedException;
import ch.qos.logback.classic.Level;
import com.brandwatch.robots.RobotsConfig;
import com.brandwatch.robots.RobotsFactory;
import com.brandwatch.robots.RobotsService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.xsoup.Xsoup;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Gospy implements Observable {
    private static final Logger logger = LoggerFactory.getLogger(Gospy.class);

    private final String identifier;

    private Scheduler scheduler;
    private Fetchers fetcherFactory;
    private PageProcessors pageProcessorFactory;
    private Processors processorFactory;
    private Pipelines pipelineFactory;
    private ExecutorService threadPool;
    private ExceptionHandler handler;
    private int visitGapMillis;
    private volatile boolean running;
    private Thread operationChainThread;
    private RobotsService robotsService;

    private Gospy(String identifier, Scheduler scheduler
            , Fetchers fetcherFactory
            , PageProcessors pageProcessorFactory
            , Processors processorFactory
            , Pipelines pipelineFactory
            , ExceptionHandler handler
            , boolean checkForRobots) {
        this.identifier = identifier;
        this.scheduler = scheduler;
        this.fetcherFactory = fetcherFactory;
        this.pageProcessorFactory = pageProcessorFactory;
        this.processorFactory = processorFactory;
        this.pipelineFactory = pipelineFactory;
        this.handler = handler;
        this.visitGapMillis = 0;
        this.running = true;
        this.operationChainThread = newOperationChainThread();

        // introduce from <a>https://github.com/BrandwatchLtd/robots</a>
        if (checkForRobots) {
            RobotsConfig config = new RobotsConfig();
            config.setMaxRedirectHops(3);
            config.setRequestTimeoutMillis(2000);
            config.setReadTimeoutMillis(3000);
            RobotsFactory factory = new RobotsFactory(config);
            this.robotsService = factory.createService();
        }
    }

    public Thread newOperationChainThread() {
        return new Thread(() -> {
            Task t0;
            while (running) {
                while ((t0 = scheduler.getTask(identifier)) != null) {
                    Task task = t0;
                    threadPool.execute(() -> {
                        Page page = null;
                        try {
                            Fetcher fetcher = fetcherFactory.get(task.getProtocol());

                            // check robots.txt
                            if (!(robotsService == null || robotsService.isAllowed(fetcher.getUserAgent(), URI.create(task.getUrl())))) {
                                handler.exceptionCaught(new TaskBlockedException("task blocked by robots.txt"), task, null);
                            }

                            page = fetcher.fetch(task);
                            Result<?> result;
                            try {
                                result = invokePageProcessor(page, pageProcessorFactory.get(page.getTask().getUrl()));
                            } catch (PageProcessorNotFoundException e) {
                                result = processorFactory.get(page.getContentType()).process(task, page);
                            }

                            if (result != null) {
                                if (result.getNewTasks() != null) {
                                    Iterator<Task> taskIterator = result.getNewTasks().iterator();
                                    while (taskIterator.hasNext()) {
                                        scheduler.addTask(identifier, taskIterator.next());
                                    }
                                }
                                // response to the scheduler after declare new tasks
                                if (scheduler instanceof Verifiable) {
                                    ((Verifiable) scheduler).feedback(identifier, task);
                                }
                                if (result.getData() != null) {
                                    Iterator<Pipeline> pipelineIterator = pipelineFactory.get(result.getType()).iterator();
                                    while (pipelineIterator.hasNext()) {
                                        pipelineIterator.next().pipe(result);
                                    }
                                }
                            }
                            Thread.sleep(visitGapMillis);
                        } catch (Throwable e) {
                            Collection<Task> tasks = handler.exceptionCaught(e, task, page);
                            if (tasks != null) {
                                tasks.forEach(t -> scheduler.addTask(identifier, t));
                            }
                        }
                    });
                }
            }
            logger.info("Operation chain stopped.");
        });
    }

    @Experimental
    private Result<?> invokePageProcessor(Page page, Class<? extends PageProcessor> clazz) throws Exception {
        PageProcessor pageProcessor = clazz.newInstance();
        pageProcessor.setTask(page.getTask());

        try {
            byte[] bytes = page.getContent();
            for (Field field : clazz.getFields()) {
                Set<String> results = new LinkedHashSet<>();
                for (Annotation annotation : field.getAnnotations()) {
                    if (annotation.annotationType() == ExtractBy.XPath.class) {
                        Document document = Jsoup.parse(new String(bytes));
                        for (String xpath : ((ExtractBy.XPath) annotation).value()) {
                            results.addAll(Xsoup.compile(xpath).evaluate(document).list());
                        }
                    } else if (annotation.annotationType() == ExtractBy.XPaths.class) {
                        Document document = Jsoup.parse(new String(bytes));
                        for (ExtractBy.XPath a : ((ExtractBy.XPaths) annotation).value()) {
                            for (String xpath : a.value()) {
                                results.addAll(Xsoup.compile(xpath).evaluate(document).list());
                            }
                        }
                    } else if (annotation.annotationType() == ExtractBy.Regex.class) {
                        String document = new String(bytes);
                        String regex = ((ExtractBy.Regex) annotation).value();
                        Matcher matcher = Pattern.compile(regex).matcher(document);
                        if (matcher.find()) {
                            results.add(matcher.group(((ExtractBy.Regex) annotation).group()));
                        }
                    } else if (annotation.annotationType() == ExtractBy.Regexs.class) {
                        String document = new String(bytes);
                        for (ExtractBy.Regex a : ((ExtractBy.Regexs) annotation).value()) {
                            Matcher matcher = Pattern.compile(a.value()).matcher(document);
                            if (matcher.find()) {
                                results.add(matcher.group(a.group()));
                            }
                        }
                    }
                }
                if (results.size() > 0) {
                    if (field.getType().isArray()) {
                        if (field.getType().getComponentType().isPrimitive()) {
                            throw new RuntimeException("We cannot cast a extracted result to a primitive type array, why not trying Object[]?");
                        }
                        Object[] src = results.toArray();
                        Object[] dst = (Object[]) field.getType().cast(Array.newInstance(field.getType().getComponentType(), results.size()));
                        for (int i = 0; i < dst.length; i++) {
                            try {
                                dst[i] = field.getType().getComponentType().cast(src[i]);
                            } catch (ClassCastException e) {
                                throw new RuntimeException(e.getMessage() + ", please change your field:" + field.getName() + " to a castable type.");
                            }
                        }
                        field.set(pageProcessor, dst);
                    } else if (Collection.class.isAssignableFrom(field.getType())) {
                        field.set(pageProcessor, field.getType().cast(results));
                    } else {
                        field.set(pageProcessor, field.getType().cast(results.iterator().next()));
                    }
                }
            }
            pageProcessor.process();
            Result<?> result = new Result<>(pageProcessor.getNewTasks(), pageProcessor.getResultData());
            result.setPage(page);
            return result;
        } catch (Throwable throwable) {
            pageProcessor.onError(throwable);
            return null;
        } finally {
            if (pageProcessor instanceof Closeable) {
                ((Closeable) pageProcessor).close();
            }
        }
    }


    public void start() {
        this.start(1);
    }

    public void start(int nThreads) {
        if (threadPool != null) {
            throw new RuntimeException("Gospy has already started.");
        }
        this.threadPool = Executors.newFixedThreadPool(nThreads);
        logger.info("Thread pool initialized. [size={}]", nThreads);
        operationChainThread.start();
    }

    public void pause(String dir) throws IOException {
        if (scheduler instanceof Recoverable) {
            try {
                ((Recoverable) scheduler).pause(dir);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    public void resume(String dir) throws Throwable {
        if (scheduler instanceof Recoverable) {
            ((Recoverable) scheduler).resume(dir);
        }
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
            // waiting for terminate.
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
        scheduler.addTask(identifier, task);
        return this;
    }

    public Gospy addTask(String url) {
        Task task = new Task(url.contains("://") ? url : "http://".concat(url));
        task.setSkipCheck(true);
        return addTask(task);
    }

    public Gospy addTasks(Task... tasks) {
        for (Task task : tasks) {
            addTask(task);
        }
        return this;
    }

    public Gospy addTasks(String... urls) {
        for (String url : urls) {
            addTask(url);
        }
        return this;
    }

    public Gospy setVisitGap(int timeMillis) {
        this.visitGapMillis = timeMillis;
        return this;
    }

    public void setLogLevel(Level logLevel) {
        LoggerHelper.setLevel("cc.gospy.core", logLevel);
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
        private String id;
        private Scheduler sc = Schedulers.GeneralScheduler.getDefault();
        private Fetchers ff = new Fetchers();
        private PageProcessors ppf = new PageProcessors();
        private Processors pf = new Processors();
        private Pipelines plf = new Pipelines();
        private ExceptionHandler eh = ExceptionHandler.DEFAULT;
        private boolean cfr = false;

        public Builder setIdentifier(String identifier) {
            id = identifier;
            return this;
        }

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

        public Builder addFetcher(Fetcher fetcher, String... acceptedProtocols) {
            ff.register(fetcher, acceptedProtocols);
            logger.info("Custom fetcher binding: {} -> {}", Arrays.toString(acceptedProtocols), fetcher.getClass().getName());
            return this;
        }

        public Builder addPageProcessor(Class<? extends PageProcessor> pageProcessor) {
            ppf.register(pageProcessor);
            return this;
        }

        public Builder addProcessor(Processor processor) {
            pf.register(processor);
            return this;
        }

        public Builder addProcessor(Processor processor, String... acceptedContentTypes) {
            pf.register(processor, acceptedContentTypes);
            logger.info("Custom processor binding: {} -> {}", Arrays.toString(acceptedContentTypes), processor.getClass().getName());
            return this;
        }

        public Builder addPipeline(Pipeline pipeline) {
            plf.register(pipeline);
            return this;
        }

        public Builder checkForRobots() {
            cfr = true;
            return this;
        }

        public Gospy build() {
            if (id == null) {
                id = StringHelper.getRandomIdentifier();
            }
            return new Gospy(id, sc, ff, ppf, pf, plf, eh, cfr);
        }

    }

}
