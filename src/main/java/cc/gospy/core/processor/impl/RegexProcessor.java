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

import java.nio.charset.Charset;
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

    public static RegexProcessor getDefault() {
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

    @FunctionalInterface
    public interface ResultHandler {
        Collection<Task> handle(Task task, Matcher matcher);
    }

    @Override
    public Result<Collection<Task>> process(Task task, Page page) throws ProcessException {
        try {
            Collection<Task> tasks = new LinkedHashSet<>();
            String content = new String(page.getContent(), Charset.defaultCharset());
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
        return new String[]{"text/*"};
    }
}
