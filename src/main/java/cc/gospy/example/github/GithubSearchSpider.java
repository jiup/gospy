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

package cc.gospy.example.github;

import cc.gospy.core.Gospy;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import org.jsoup.select.Elements;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GithubSearchSpider {
    enum Type {Repositories, Code, Commits, Issues, Wikis, Users}

    enum Language {All, C, HTML, XML, Text, JSON, CPP, Java, JupyterNotebook, Makefile, SVG}

    public static void main(String[] args) {
        GithubSearchSpider spider = new GithubSearchSpider();
        spider.getResultLinks("gospy").forEach(System.out::println);
        spider.getResultLinks("zhangjiupeng", Type.Users).forEach(System.out::println);
        spider.getResultLinks("gospy", Language.Java).forEach(System.out::println);
    }

    public Collection<String> getResultLinks(final String keyword) {
        return getResultLinks(keyword, Type.Repositories);
    }

    public Collection<String> getResultLinks(final String keyword, final Type type) {
        return getResultLinks(keyword, type, Language.All);
    }

    public Collection<String> getResultLinks(final String keyword, final Language language) {
        return getResultLinks(keyword, Type.Repositories, language);
    }

    public Collection<String> getResultLinks(final String keyword, final Type type, final Language language) {
        return getResultLinks(keyword, type, language, 1);
    }

    public Collection<String> getResultLinks(final String keyword, final Type type, final Language language, final int pageCount) {
        if (pageCount < 1)
            throw new IllegalArgumentException(pageCount + "<" + 1);

        return getResultLinks(keyword, type, language, 1, 1 + pageCount);
    }

    public Collection<String> getResultLinks(final String keyword, final Type type, final Language language, final int pageFrom, final int pageTo) {
        if (pageFrom < 1)
            throw new IllegalArgumentException(pageFrom + "<" + 1);
        if (pageFrom >= pageTo)
            throw new IllegalArgumentException(pageFrom + ">=" + pageTo);

        String sType = type != null ? type.name() : "Repositories";
        String sLanguage;
        if (language == null) {
            sLanguage = "";
        } else {
            switch (language) {
                case All: sLanguage = ""; break;
                case CPP: sLanguage = "C%2B%2B"; break;
                case JupyterNotebook: sLanguage = "Jupyter+Notebook"; break;
                default: sLanguage = language.name();
            }
        }

        final AtomicInteger currentPage = new AtomicInteger(pageFrom);
        final AtomicBoolean returned = new AtomicBoolean(false);
        final Collection<String> links = new LinkedHashSet<>();
        Gospy githubSearchSpider = Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.custom()
                        .setExitCallback(() -> returned.set(true))
                        .build())
                .addFetcher(Fetchers.HttpFetcher.getDefault())
                .addProcessor(Processors.JsoupProcessor.custom()
                        .setDocumentExtractor((page, document) -> {
                            Elements elements;
                            switch (type) {
                                case Repositories:
                                    elements = document.select(".repo-list-item");
                                    elements.forEach(element -> links.add("https://github.com".concat(element.select("h3 a").attr("href"))));
                                    break;
                                case Code:
                                    elements = document.select(".code-list-item > div.d-inline-block");
                                    elements.forEach(element -> links.add("https://github.com".concat(element.children().get(1).attr("href"))));
                                    break;
                                case Commits:
                                    elements = document.select(".commits-list-item > div.d-inline-flex > div > a");
                                    elements.forEach(element -> links.add("https://github.com".concat(element.attr("href"))));
                                    break;
                                case Issues:
                                    elements = document.select(".issue-list-item h3 > a");
                                    elements.forEach(element -> links.add("https://github.com".concat(element.attr("href"))));
                                    break;
                                case Wikis:
                                    elements = document.select(".wiki-list-item div.mb-2");
                                    elements.forEach(element -> links.add("https://github.com".concat(element.children().get(1).attr("href"))));
                                    break;
                                case Users:
                                    elements = document.select(".user-list-item div.user-list-info > a");
                                    elements.forEach(element -> links.add("https://github.com".concat(element.attr("href"))));
                                    break;
                            }
                            currentPage.incrementAndGet();
                            if (pageFrom <= currentPage.get() && currentPage.get() < pageTo) {
                                return new Result<>(Arrays.asList(new Task(String.format("https://github.com/search?l=%s&p=%d&q=%s&type=%s&utf8=%E2%9C%93", sLanguage, currentPage, keyword, sType))), null);
                            } else {
                                return new Result<>(Arrays.asList());
                            }
                        })
                        .build())
                .build().addTask(String.format("https://github.com/search?l=%s&p=%d&q=%s&type=%s&utf8=✓", sLanguage, pageFrom, keyword, sType));
        githubSearchSpider.start();
        while (!returned.get()) ; // block until spider returned
        githubSearchSpider.stop();
        return links;
    }
}
