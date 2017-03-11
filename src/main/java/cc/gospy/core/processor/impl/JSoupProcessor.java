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

import cc.gospy.core.ExceptionHandler;
import cc.gospy.core.Page;
import cc.gospy.core.Task;
import cc.gospy.core.TaskFilter;
import cc.gospy.core.processor.DocumentExtractor;
import cc.gospy.core.processor.Processor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class JSoupProcessor implements Processor, ExceptionHandler {
    private DocumentExtractor handler;
    private TaskFilter filter;

    private JSoupProcessor(DocumentExtractor handler, TaskFilter filter) {
        this.handler = handler;
        this.filter = filter;
    }

    public static Builder custom() {
        return new Builder();
    }

    public static JSoupProcessor getDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private DocumentExtractor ha = (task, document) -> {
            Set<Task> tasks = new HashSet<>();
            for (Element element : document.select("a[href]")) {
                String link = element.attr("href");
                if (link != null && !link.equals("")) {
                    tasks.add(new Task(toAbsoluteUrl(task.getProtocol(), task.getHost(), link)));
                }
            }
            return tasks;
        };

        private TaskFilter fi = TaskFilter.DEFAULT;

        public Builder setDocumentExtractor(DocumentExtractor handler) {
            ha = handler;
            return this;
        }

        public Builder setFullLinkDocumentExtractor() {
            ha = (task, document) -> {
                Set<Task> tasks = new HashSet<>();
                for (Element element : document.select("[href]")) {
                    String link = element.attr("href");
                    if (link != null && !link.equals("")) {
                        tasks.add(new Task(toAbsoluteUrl(task.getProtocol(), task.getHost(), link)));
                    }
                }
                for (Element element : document.select("[src]")) {
                    String link = element.attr("src");
                    if (link != null && !link.equals("")) {
                        tasks.add(new Task(toAbsoluteUrl(task.getProtocol(), task.getHost(), link)));
                    }
                }
                return tasks;
            };
            return this;
        }

        private String toAbsoluteUrl(String protocol, String host, String anyUrl) {
            String res;
            if (anyUrl.matches("^https?://.*")) {
                res = anyUrl;
            } else if (anyUrl.startsWith("//")) {
                res = "http:".concat(anyUrl);
            } else if (anyUrl.startsWith("/")) {
                res = protocol.concat("://").concat(host).concat(anyUrl);
            } else {
                res = protocol.concat("://").concat(host).concat("/").concat(anyUrl);
            }
            res = res.indexOf('#') != -1 ? res.substring(0, res.indexOf('#')) : res; // remove local jump
            res = res.endsWith("/") ? res.substring(0, res.length() - 1) : res; // avoid duplicate links
            return res;
        }

        public Builder setTaskFilter(TaskFilter filter) {
            fi = filter;
            return this;
        }

        public JSoupProcessor build() {
            return new JSoupProcessor(ha, fi);
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
        try {
            html = page.getContent().toString(charsetName != null ? charsetName : "UTF-8");
        } catch (UnsupportedEncodingException e) {
            html = page.getContent().toString("UTF-8");
        }
        return Jsoup.parse(html);
    }

    private Collection<Task> process0(Task task, Page page) {
        Document document;
        try {
            document = parse(page);
        } catch (UnsupportedEncodingException e) {
            return exceptionCaught(e, task, page);
        }
        return handler.handle(task, document).stream().filter(filter).collect(Collectors.toSet());
    }


    @Override
    public Collection<Task> process(Task task, Page page) {
        try {
            return process0(task, page);
        } catch (Throwable throwable) {
            return exceptionCaught(throwable, task, page);
        }
    }

    @Override
    public String[] getAcceptedContentType() {
        return new String[]{null, "text/plain", "text/html"};
    }

    @Override
    public Collection<Task> exceptionCaught(Throwable throwable, Task task, Page page) {
        throwable.printStackTrace();
        return null;
    }
}
