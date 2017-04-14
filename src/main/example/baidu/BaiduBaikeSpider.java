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

package cc.gospy.example.baidu;

import cc.gospy.core.Gospy;
import cc.gospy.core.entity.Result;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;

public class BaiduBaikeSpider {
    public static void main(String[] args) {
        String urlTemplate = "http://baike.baidu.com/search/word?word=%s&pic=1&sug=1&enc=utf8";
        Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.getDefault())
                .addFetcher(Fetchers.HttpFetcher.custom()
                        .before(request -> {
                        }).build())
                .addProcessor(Processors.JsoupProcessor.custom()
                        .setDocumentExtractor((page, document) -> new Result<>(null, document.select("div.lemma-summary").text()))
                        .build())
                .addPipeline(Pipelines.ConsolePipeline.getDefault())
                .build().addTask(String.format(urlTemplate, "网络爬虫")).start();
    }
}
