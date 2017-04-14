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

package cc.gospy.example.basic;

import cc.gospy.core.Gospy;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.util.StringHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

public class JSoupDemo {
    public static void main(String[] args) {
        Gospy.custom()
                .addFetcher(Fetchers.HttpFetcher.getDefault())
                .addProcessor(Processors.JsoupProcessor.custom()
                        .setDocumentExtractor((page, document) -> {
                            Task task = page.getTask();
                            if (task.getDepth() <= 2) {
                                Collection<Task> tasks = new LinkedHashSet<>();
                                document.select("a[href]").forEach(element -> {
                                    String link = element.attr("href");
                                    if (StringHelper.isNotNull(link)) {
                                        Task newTask = new Task(StringHelper.toAbsoluteUrl(task.getProtocol(), task.getHost(), task.getUrl(), link));
                                        newTask.setDepth(task.getDepth() + 1);
                                        tasks.add(newTask);
                                    }
                                });
                                return new Result<>(tasks, String.format("[%s] visited in %d ms, depth=%d, %d new links.", task.getUrl(), page.getResponseTime(), task.getDepth(), tasks.size()));
                            } else {
                                return new Result<>(null, String.format("[%s] visited in %d ms, depth=3, stop current branch.", task.getUrl(), page.getResponseTime()));
                            }
                        })
                        .build())
                .addPipeline(Pipelines.ConsolePipeline.getDefault())
                .setExceptionHandler((throwable, task, page) -> Arrays.asList(task)) // retry if exception caught
                .build().addTask("https://github.com/explore").setVisitGap(3000).start(20);
    }
}