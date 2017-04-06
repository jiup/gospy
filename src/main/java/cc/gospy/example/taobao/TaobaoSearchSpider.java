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

package cc.gospy.example.taobao;

import cc.gospy.core.Gospy;
import cc.gospy.core.TaskFilter;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.fetcher.UserAgent;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import cc.gospy.core.util.StringHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TaobaoSearchSpider {

    public static void main(String[] args) {
        String[][] result = new TaobaoSearchSpider().getResultLinks("keyword");
        for (int i = 0; i < result[0].length; i++) {
            System.out.println(String.format("%-70s%s", result[1][i].trim(), result[0][i].trim()));
        }
    }

    public String[][] getResultLinks(final String keyword) {
        return getResultLinks(keyword, 1);
    }

    public String[][] getResultLinks(final String keyword, final int pageCount) {
        if (pageCount < 1)
            throw new IllegalArgumentException(pageCount + "<" + 1);

        return getResultLinks(keyword, 1, 1 + pageCount);
    }

    public String[][] getResultLinks(final String keyword, final int pageFrom, final int pageTo) {
        if (pageFrom < 1)
            throw new IllegalArgumentException(pageFrom + "<" + 1);
        if (pageFrom >= pageTo)
            throw new IllegalArgumentException(pageFrom + ">=" + pageTo);

        final AtomicInteger currentPage = new AtomicInteger(pageFrom);
        final AtomicBoolean returned = new AtomicBoolean(false);
        final List<String> titles = new ArrayList<>();
        final List<String> links = new ArrayList<>();
        Gospy taobaoSearchSpider = Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.custom()
                        .setExitCallback(() -> returned.set(true))
                        .setPendingTimeInSeconds(30)
                        .build())
                .addFetcher(Fetchers.PhantomJSFetcher.custom()
                        .setPhantomJsBinaryPath("D:\\Program Files\\PhantomJS\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe")
                        .setUserAgent(UserAgent.Chrome_56_0_2924_87_Win_10_64_bit)
                        .setLoadImages(false)
                        .build())
                .addProcessor(Processors.XPathProcessor.custom()
                        .extract("/*//*[@id='mainsrp-itemlist']/div/div/div[1]/div/div[2]/div[2]/a/@href", (task, resultList) -> {
                            resultList.forEach(link -> links.add(StringHelper.toAbsoluteUrl(task, link)));
                            return null;
                        })
                        .extract("/*//*[@id='mainsrp-itemlist']/div/div/div[1]/div/div[2]/div[2]/a/allText()", (task, resultList) -> {
                            titles.addAll(resultList);
                            return null;
                        })
                        .extract("/*//*[@id='J_itemlistPersonality']/div/div/div[2]/div[2]/a/@href", (task, resultList) -> {
                            resultList.forEach(link -> links.add(StringHelper.toAbsoluteUrl(task, link)));
                            return null;
                        })
                        .extract("/*//*[@id='J_itemlistPersonality']/div/div/div[2]/div[2]/a/allText()", (task, resultList) -> {
                            titles.addAll(resultList);
                            currentPage.incrementAndGet();
                            if (pageFrom <= currentPage.get() && currentPage.get() < pageTo) {
                                return Arrays.asList(new Task(String.format("phantomjs://https://s.taobao.com/search?q=%s&s=%d", keyword, (currentPage.get() - 1) * 44)));
                            } else {
                                return null;
                            }
                        })
                        .setTaskFilter(TaskFilter.PHANTOMJS)
                        .build())
                .build().addTask(String.format("phantomjs://https://s.taobao.com/search?q=%s&s=%d", keyword, (pageFrom - 1) * 44));

        taobaoSearchSpider.start();
        while (!returned.get()) ; // block until spider returned
        taobaoSearchSpider.stop();

        return new String[][]{titles.toArray(new String[]{}), links.toArray(new String[]{})};
    }
}
