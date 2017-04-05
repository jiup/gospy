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
import cc.gospy.core.processor.ExtractBy;
import cc.gospy.core.processor.PageProcessor;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.processor.UrlPattern;
import cc.gospy.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class AnnotationDemo {
    public static void main(String[] args) {
        String dir = "D:/"; // you need to specify a valid dir on you os
        Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.getDefault())
                .addFetcher(Fetchers.HttpFetcher.getDefault())
                .addPageProcessor(BaiduHomepageProcessor.class)
                .addProcessor(Processors.UniversalProcessor.getDefault())
                .addPipeline(Pipelines.ConsolePipeline.getDefault())
                .addPipeline(Pipelines.SimpleFilePipeline.custom().setDir(dir).build())
                .build().addTask("http://www.baidu.com/index.php").start();
    }

    @UrlPattern("http://www.baidu.com/.*\\.php")
    public static class BaiduHomepageProcessor extends PageProcessor {
        @ExtractBy.XPath("/html/head/title/text()")
        public String title;

        @ExtractBy.XPath("//*[@id='u1']/a/@href")
        @ExtractBy.XPath("//*[@id='head']/div/div[4]/div/div[2]/div[1]/div/a/@href")
        public Set<String> topBarLinks;

        @ExtractBy.Regex(value = "id=\"su\" value=\"(.*?)\"", group = 1)
        public String searchBtnValue;

        @ExtractBy.XPath
        public String[] allLinks;

        @Override
        public void process() {
            System.out.println("Task url      :" + task.getUrl());
            System.out.println("Title         :" + title);
            System.out.println("Search slogan :" + searchBtnValue);
            System.out.println("Top bar links :");
            topBarLinks.forEach(System.out::println);
        }

        @Override
        public Collection<Task> getNewTasks() {
            return Arrays.asList(new Task("https://www.baidu.com/img/bd_logo1.png"));
        }

        @Override
        public Object[] getResultData() {
            return Arrays.asList(allLinks).stream().filter(s -> s.matches("^https?://((?!javascript:|mailto:| ).)*")).toArray();
        }
    }
}
