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

import cc.gospy.core.Page;
import cc.gospy.core.Task;
import cc.gospy.core.TaskFilter;
import cc.gospy.core.processor.Processor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import us.codecraft.xsoup.Xsoup;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.*;

public class XPathProcessor implements Processor {
    private Map<String, ResultHandler> handlerChain = new LinkedHashMap<>();
    private TaskFilter filter;

    private XPathProcessor(Map<String, ResultHandler> handlerChain, TaskFilter filter) {
        this.filter = filter;
        this.handlerChain = handlerChain;
    }

    public static Builder custom() {
        return new Builder();
    }

    public static XPathProcessor getDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private Map<String, ResultHandler> hc = new LinkedHashMap<>();
        private TaskFilter fi = TaskFilter.HTTP_DEFAULT;

        public Builder setTaskFilter(TaskFilter filter) {
            fi = filter;
            return this;
        }

        public Builder extract(String xpath, ResultHandler handler) {
            hc.put(xpath, handler);
            return this;
        }

        public XPathProcessor build() {
            if (hc.size() == 0) {
                return extract("//a/@href", (task, resultList) -> {
                    Collection<Task> tasks = new ArrayList<>();
                    resultList.forEach(link -> tasks.add(new Task(link)));
                    return tasks;
                }).build();
            }
            return new XPathProcessor(hc, fi);
        }
    }

    protected String getCharacterEncoding(Page page) {
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

    private Document parse(Page page) throws UnsupportedEncodingException {
        String charsetName = getCharacterEncoding(page);
        String html;
        html = page.getContent().toString(charsetName != null ? charsetName : Charset.defaultCharset().name());
        return Jsoup.parse(html);
    }

    @FunctionalInterface
    public interface ResultHandler {
        Collection<Task> handle(Task task, List<String> resultList);
    }

    @Override
    public Collection<Task> process(Task task, Page page) throws Throwable {
        List<Task> tasks = new ArrayList<>();
        Document document = parse(page);
        handlerChain.forEach((xpath, handler) -> {
            tasks.addAll(handler.handle(task, Xsoup.compile(xpath).evaluate(document).list()));
        });
        return tasks;
    }

    @Override
    public String[] getAcceptedContentType() {
        return new String[]{"text/html", "text/xml"};
    }

}
