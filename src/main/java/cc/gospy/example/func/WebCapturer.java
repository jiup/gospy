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

package cc.gospy.example.func;

import cc.gospy.core.Gospy;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import cc.gospy.core.util.StringHelper;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import org.jsoup.nodes.Element;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class WebCapturer {
    public static void main(String[] args) {
        Gospy.custom()
                .setScheduler(Schedulers.GeneralScheduler.custom().build())
                .addFetcher(Fetchers.HttpFetcher.custom().build())
                .addProcessor(Processors.JsoupProcessor.custom()
                        .setDocumentExtractor((page, document) -> {
                            Task task = page.getTask();
                            if (page.getStatusCode() != 200) {
                                if (page.getStatusCode() == 302 || page.getStatusCode() == 301) {
                                    Collection<Task> tasks = new ArrayList<>();
                                    tasks.add(new Task(page.getExtra().get("Location").toString()));
                                    return new Result<>(tasks);
                                }
                                System.err.println(task + "\t" + page.getStatusCode());
                            }
                            Collection<Task> tasks = new HashSet<>();
                            Collection<Element> elements = document.select("a[href]");
                            elements.addAll(document.select("link[href]"));
                            elements.addAll(document.select("[src]"));
                            for (Element element : elements) {
                                String link = element.hasAttr("href") ? element.attr("href") : element.attr("src");
                                link = link.indexOf('#') != -1 ? link.substring(0, link.indexOf('#')) : link;
                                boolean rootPage = link.endsWith("/");
                                link = rootPage ? link.concat("null") : link;
                                link = StringHelper.toAbsoluteUrl(task.getProtocol(), task.getHost(), task.getUrl(), link);
                                if (link.matches("^https?://((?!javascript:|mailto:| ).)*")) {
                                    String url = link;
                                    String rUrl = StringHelper.toRelativeUrl(task.getProtocol(), task.getHost(), task.getUrl(), url);
                                    if (rUrl == null) {
                                        // crawl without outside pages
                                        continue;
                                    } else {
                                        tasks.add(new Task(rootPage ? link.substring(0, link.length() - 4) : link));
                                    }
                                    String name = rUrl.substring(rUrl.lastIndexOf('/') + 1);
                                    name = StringHelper.toEscapedFileName(name);
                                    if (element.tagName().equals("a")) {
                                        name = name.endsWith(".html") ? name : name.concat(".html");
                                    }
                                    rUrl = rUrl.substring(0, rUrl.lastIndexOf('/') + 1);
                                    String modifiedLink = rUrl.concat(name);
//                                    System.out.println(modifiedLink + " = " + rUrl + " + " + name);
                                    element.attr(element.hasAttr("href") ? "href" : "src", modifiedLink);
                                }
                            }
                            String name = StringHelper.toEscapedFileName(task.getUrl().substring(task.getUrl().lastIndexOf('/') + 1));
                            name = name.endsWith(".html") ? name : name.concat(".html");
                            String dir = StringHelper.cutOffProtocolAndHost(task.getUrl().substring(0, task.getUrl().lastIndexOf('/') + 1));
                            page.getExtra().put("savePath", URLDecoder.decode(dir.concat(name), Charset.defaultCharset().name()));
                            Result<byte[]> result = new Result<>(tasks, document.toString().getBytes());
                            return result;
                        }).build())
                .addProcessor(Processors.UniversalProcessor.custom().build())
//                .addPipeline(Pipelines.ConsolePipeline.getDefault())
                .addPipeline(Pipelines.HierarchicalFilePipeline.custom().setBasePath("D:/Gospy/test.blog.timeliar.date/").build())
                .build().addTask("https://blog.timeliar.date/links/").setVisitGap(1000).start(1);
    }
}
