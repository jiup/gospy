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

package cc.gospy.example.google;

import cc.gospy.core.Gospy;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GoogleScholarSpider {

    public static void main(String[] args) {
        new GoogleScholarSpider().getResultLinks("keywords").forEach(System.out::println);
    }

    public Collection<String> getResultLinks(final String keyword) {
        return getResultLinks(keyword, 1);
    }

    public Collection<String> getResultLinks(final String keyword, final int pageCount) {
        if (pageCount < 1)
            throw new IllegalArgumentException(pageCount + "<" + 1);

        return getResultLinks(keyword, 1, 1 + pageCount);
    }

    public Collection<String> getResultLinks(final String keyword, final int pageFrom, final int pageTo) {
        if (pageFrom < 1)
            throw new IllegalArgumentException(pageFrom + "<" + 1);
        if (pageFrom >= pageTo)
            throw new IllegalArgumentException(pageFrom + ">=" + pageTo);

        final AtomicInteger currentPage = new AtomicInteger(pageFrom);
        final AtomicBoolean returned = new AtomicBoolean(false);
        final Collection<String> links = new LinkedHashSet<>();
        Gospy googleScholarSpider = Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.custom()
                        .setExitCallback(() -> returned.set(true))
                        .build())
                .addFetcher(Fetchers.HttpFetcher.custom()
                        .before(request -> request.setConfig(RequestConfig.custom().setProxy(new HttpHost("127.0.0.1", 8118)).build()))
                        .build())
                .addProcessor(Processors.XPathProcessor.custom()
                        .extract("//*[@id='gs_ccl_results']/div/div/h3/a/@href", (task, resultList) -> {
                            links.addAll(resultList);
                            currentPage.incrementAndGet();
                            if (pageFrom <= currentPage.get() && currentPage.get() < pageTo) {
                                return Arrays.asList(new Task(String.format("https://scholar.google.com/scholar?start=%d&q=%s", (currentPage.get() - 1) * 10, keyword)));
                            } else {
                                return Arrays.asList();
                            }
                        })
                        .build())
                .build().addTask(String.format("https://scholar.google.com/scholar?q=%s", keyword));
        googleScholarSpider.start();
        while (!returned.get()) ; // block until spider returned
        googleScholarSpider.stop();
        return links;
    }
}
