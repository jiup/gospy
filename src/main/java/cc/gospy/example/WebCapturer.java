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

package cc.gospy.example;

import cc.gospy.core.Gospy;
import cc.gospy.core.Result;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import cc.gospy.core.util.StringHelper;
import org.jsoup.nodes.Element;

public class WebCapturer {
    public static void main(String[] args) {
        Gospy.custom()
                .setScheduler(Schedulers.GeneralScheduler.getDefault())
                .addFetcher(Fetchers.HttpFetcher.custom().setAutoKeepAlive(false).build())
                .addProcessor(Processors.JSoupProcessor.custom()
                        .setDocumentExtractor((task, document) -> {
                            for (Element element : document.select("a[href]")) {
                                if (element.attr("href").matches("^https?://((?!javascript:|mailto:| ).)*")) {
                                    String rUrl = StringHelper.toRelativeUrl(task.getProtocol(), task.getHost(), task.getUrl(), element.attr("href"));
                                    rUrl = rUrl == null ? element.attr("href") : rUrl;
                                    String name = rUrl.substring(rUrl.lastIndexOf('/') + 1);
                                    rUrl = rUrl.substring(0, rUrl.length() - name.length() + 1);
                                    element.attr("href", rUrl.concat("/").concat(StringHelper.toEscapedFileName(name)).concat(".html"));
                                }
                            }
                            String fileName = StringHelper.toEscapedFileName(StringHelper.cutOffProtocolAndHost(task.getUrl())).concat(".html");
                            Result<String[]> result = new Result<>(null, new String[]{
                                    fileName, document.toString()
                            });
                            return result;
                        }).build())
                .addPipeline(Pipelines.HierarchicalFilePipeline.custom().setBasePath("D:/123/456/").build())
                .build().addTask("http://blog.csdn.net/futureer/article/details/19684981").setVisitGap(500).start(1);
    }
}
