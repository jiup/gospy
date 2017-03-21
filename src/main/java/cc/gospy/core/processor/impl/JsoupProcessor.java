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

package cc.gospy.core.processor.impl;

import cc.gospy.core.TaskFilter;
import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.processor.Extractor;
import cc.gospy.core.processor.ProcessException;
import cc.gospy.core.processor.Processor;
import cc.gospy.core.util.StringHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class JsoupProcessor implements Processor {
    private Extractor<Document, ?> handler;
    private TaskFilter filter;

    private JsoupProcessor(Extractor<Document, ?> handler, TaskFilter filter) {
        this.handler = handler;
        this.filter = filter;
    }

    public static Builder custom() {
        return new Builder();
    }

    public static JsoupProcessor getDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private Extractor<Document, ?> ha;
        private TaskFilter fi = TaskFilter.HTTP_DEFAULT;

        public <T> Builder setDocumentExtractor(Extractor<Document, T> handler) {
            ha = handler;
            return this;
        }

        public Builder setPageLinkDocumentExtractor() {
            return setDocumentExtractor((page, document) -> {
                if (page.getStatusCode() != 200) {
                    return null;
                }
                Task task = page.getTask();
                Collection<Task> tasks = new HashSet<>();
                for (Element element : document.select("a[href]")) {
                    String link = element.attr("href");
                    if (link != null && !link.equals("")) {
                        tasks.add(new Task(StringHelper.toAbsoluteUrl(task.getProtocol(), task.getHost(), task.getUrl(), link)));
                    }
                }
                return new Result<>(tasks, task.toString());
            });
        }

        public Builder setFullLinkDocumentExtractor() {
            return setDocumentExtractor((page, document) -> {
                if (page.getStatusCode() != 200) {
                    return null;
                }
                Task task = page.getTask();
                Collection<Task> tasks = new LinkedHashSet<>();
                for (Element element : document.select("[href]")) {
                    String link = element.attr("href");
                    if (link != null && !link.equals("")) {
                        tasks.add(new Task(StringHelper.toAbsoluteUrl(task.getProtocol(), task.getHost(), task.getUrl(), link)));
                    }
                }
                for (Element element : document.select("[src]")) {
                    String link = element.attr("src");
                    if (link != null && !link.equals("")) {
                        tasks.add(new Task(StringHelper.toAbsoluteUrl(task.getProtocol(), task.getHost(), task.getUrl(), link)));
                    }
                }
                return new Result<>(tasks, task.toString());
            });
        }

        public Builder setTaskFilter(TaskFilter filter) {
            fi = filter;
            return this;
        }

        public JsoupProcessor build() {
            return ha == null ? this.setPageLinkDocumentExtractor().build() : new JsoupProcessor(ha, fi);
        }
    }

    protected static String getCharacterEncoding(Page page) {
        if (page.getExtra() == null || page.getExtra().get("Content-Type") == null) {
            return null;
        }
        for (String kv : page.getExtra().get("Content-Type").toString().split(";")) {
            if (kv.trim().startsWith("charset=")) {
                return kv.trim().substring(8);
            }
        }
        return null;
    }

    public Extractor<Document, ?> getDocumentExtractor() {
        return handler;
    }

    private Document parse(Page page) throws UnsupportedEncodingException {
        String charsetName = getCharacterEncoding(page);
        String html = new String(page.getContent(), charsetName != null ? charsetName : Charset.defaultCharset().name());
        return Jsoup.parse(html);
    }

    @Override
    public <T> Result<T> process(Task task, Page page) throws ProcessException {
        try {
            Result result = handler.handle(page, parse(page));
            if (result != null) {
                if (result.getNewTasks() != null) {
                    result.getNewTasks().removeIf(filter.negate());
                }
                if (result.getPage() == null) {
                    result.setPage(page);
                }
            }
            return result;
        } catch (Throwable throwable) {
            throw new ProcessException(throwable.getMessage(), throwable);
        }
    }

    @Override
    public String[] getAcceptedContentType() {
        return new String[]{"text/html", "text/xml"};
    }

}
