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
import cc.gospy.core.fetcher.UserAgent;
import cc.gospy.core.processor.Processor;
import cc.gospy.core.processor.ProcessorFactory;
import cc.gospy.core.scheduler.Scheduler;
import cc.gospy.core.scheduler.SchedulerFactory;
import cc.gospy.core.util.StringHelper;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Gospy {
    static Logger logger = LoggerFactory.getLogger(Gospy.class);

    private Scheduler scheduler;
    private FetcherFactory fetcherFactory;
    private ProcessorFactory processorFactory;
    private ExecutorService threadPool;
    private ExceptionHandler handler;
    private int visitGapMillis;

    private Gospy(Scheduler scheduler
            , FetcherFactory fetcherFactory
            , ProcessorFactory processorFactory
            , ExceptionHandler handler) {
        this.scheduler = scheduler;
        this.fetcherFactory = fetcherFactory;
        this.processorFactory = processorFactory;
        this.handler = handler;
        this.visitGapMillis = 0;
    }

    public void start() {
        this.start(1);
    }

    public void start(int nThreads) {
        this.threadPool = Executors.newFixedThreadPool(nThreads);
        Task t0;
        while (true) {
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
                        Thread.sleep(visitGapMillis);
                    } catch (Throwable e) {
                        handler.exceptionCaught(e, task, page);
                    }
                });
            }
        }
    }

    public void stop() {
        this.scheduler.stop();
        this.threadPool.shutdown();
    }

    public Gospy addTask(Task task) {
        scheduler.addTask(task);
        return this;
    }

    public Gospy addTask(String url) {
        return addTask(new Task(url));
    }

    public Gospy visitGap(int timeMillis) {
        this.visitGapMillis = timeMillis;
        return this;
    }

    public static Builder custom() {
        return new Builder();
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

        public Builder setExeceptionHandler(ExceptionHandler handler) {
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

    public static void main(String[] args) {
        Gospy spider = Gospy.custom()
                .setScheduler(SchedulerFactory.GeneralScheduler.getDefault())
                .addFetcher(
                        FetcherFactory.HttpFetcher.custom()
                                .before(request -> request.setHeader("User-Agent", UserAgent.Chrome_41_0_2228_0_Win_7_32_bit))
                                .build())
                .addProcessor(
                        ProcessorFactory.JSoupProcessor.custom().setDocumentExtractor((task, document) -> {
                            Set<Task> tasks = new HashSet<>();
                            for (Element element : document.select("a[href]")) {
                                String link = element.attr("href");
                                if (link != null && !link.equals("")) {
                                    tasks.add(new Task(StringHelper.toAbsoluteUrl(task.getProtocol(), task.getHost(), link)));
                                }
                            }
                            String fileName = StringHelper.toEscapedFileName(task.getUrl());
                            fileName = fileName.endsWith(".htm") ? fileName : fileName.concat(".html");
                            File file = new File("D:/hlju/" + fileName);
                            if (!file.exists()) {
                                file.createNewFile();
                                OutputStream out = new FileOutputStream(file);
                                out.write(document.toString().getBytes());
                                out.close();
                            }
                            logger.warn("{}\t\t{}\t{}", task.getUrl(), tasks.size(), tasks);
                            return tasks;
                        }).build())
                .setExeceptionHandler((throwable, task, page) -> {
                    throwable.printStackTrace();
                    return null;
                })
                .build();

        spider.addTask("http://www.hlju.edu.cn/index/zddh.htm").start(20);
    }

}
