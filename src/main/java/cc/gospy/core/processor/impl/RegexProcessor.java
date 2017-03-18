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
import cc.gospy.core.processor.ProcessException;
import cc.gospy.core.processor.Processor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexProcessor implements Processor {
    private Map<String, ResultHandler> handlerChain;
    private TaskFilter filter;

    private RegexProcessor(Map<String, ResultHandler> handlerChain, TaskFilter filter) {
        this.handlerChain = handlerChain;
        this.filter = filter;
    }

    public static Builder custom() {
        return new Builder();
    }

    public RegexProcessor getDefault() {
        return new Builder().build();
    }

    public static class Builder {
        private Map<String, ResultHandler> hc = new LinkedHashMap<>();
        private TaskFilter fi = TaskFilter.HTTP_DEFAULT;

        public Builder setTaskFilter(TaskFilter filter) {
            fi = filter;
            return this;
        }

        public Builder extract(String regex, ResultHandler handler) {
            hc.put(regex, handler);
            return this;
        }


        public RegexProcessor build() {
            if (hc.size() == 0) {
                return extract("href\\s*=\\s*((\"(.*?)\")|('(.*?)'))", (task, matcher) -> {
                    Collection<Task> tasks = new ArrayList<>();
                    while (matcher.find()) {
                        tasks.add(new Task(matcher.group(3)));
                    }
                    return tasks;
                }).build();
            }
            return new RegexProcessor(hc, fi);
        }
    }

    public static void main(String[] args) {
        Pattern pattern = Pattern.compile("\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))");
        pattern = Pattern.compile("href\\s*=\\s*((\"(.*?)\")|('(.*?)'))");
//        pattern = Pattern.compile("href\\s*=\\s*'(.*?)'|href\\s*=\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher("<a href=\"http://stats.stackexchange.com/questions/268126/why-are-survival-times-assumed-to-be-exponentially-distributed\" class=\"js-gps-track\" data-gps-track=\"site.switch({ item_type:11, target_site:65 }); posts_hot_network.click({ item_type:2, location:11 })\">\n" +
                "                    Why are survival times assumed to be exponentially distributed?\n" +
                "                </a>");
        while (matcher.find()) {
            System.out.println("-> " + matcher.group(3));
        }

    }

    @FunctionalInterface
    public interface ResultHandler {
        Collection<Task> handle(Task task, Matcher matcher);
    }

    @Override
    public Result<Collection<Task>> process(Task task, Page page) throws ProcessException {
        try {
            Collection<Task> tasks = new LinkedHashSet<>();
            String content = page.getContent().toString();
            handlerChain.forEach((regex, resultHandler) -> {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(content);
                tasks.addAll(resultHandler.handle(task, matcher));
            });
            Result<Collection<Task>> result = new Result<>(tasks, tasks);
            result.setPage(page);
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
