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
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;

import java.util.LinkedHashMap;

public class PostDemo {
    public static void main(String[] args) {
        Task task = new Task("some post-able website (absolutely url)"); // specify by yourself
        new LinkedHashMap() {{
            put("foo", "123");
            put("bar", "abc");
            put("baz", "tada");
            task.getExtra().put("post", this);
        }};
        System.out.println(task.getExtra().get("post"));
        Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.getDefault())
                .addFetcher(Fetchers.HttpFetcher.getDefault())
                .addProcessor(Processors.UniversalProcessor.getDefault())
                .addPipeline(Pipelines.ConsolePipeline.custom().bytesToString().build())
                .build().addTask(task).setVisitGap(1000).start();
    }
}
