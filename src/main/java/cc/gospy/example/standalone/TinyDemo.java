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

package cc.gospy.example.standalone;

import cc.gospy.core.Gospy;
import cc.gospy.core.GospyEventListener;
import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import cc.gospy.core.util.StringHelper;

import java.util.ArrayList;
import java.util.Collection;

public class TinyDemo {
    public static void main(String[] args) {
        Gospy.custom()
                .setScheduler(Schedulers.GeneralScheduler.getDefault())
                .addFetcher(Fetchers.HttpFetcher.getDefault())
                .addFetcher(Fetchers.FileFetcher.getDefault())
                .addProcessor(Processors.XPathProcessor.custom()
                        .extract("//a/@href", (task, resultList) -> {
                            Collection<Task> tasks = new ArrayList<>();
                            for (String url : resultList) {
                                tasks.add(new Task(url));
                            }
                            return tasks;
                        }).build())
                .addPipeline(Pipelines.ConsolePipeline.getDefault())
                .addPipeline(Pipelines.SimpleFilePipeline.getDefault())
                .build()
                .addTask("https://blog.timeliar.date/links/")
                .addTask("https://www.zhangjiupeng.com/")
                .setVisitGap(1000).start(20);

    }

    class Test extends GospyEventListener {

        @Override
        public Collection<Task> onInit() {
            return null;
        }

        @Override
        public Collection<Task> process(Page page, Result result) {
            return null;
        }

        @Override
        public Collection<Task> onError(Throwable throwable, Task task, Page page) {
            return null;
        }

    }
}
