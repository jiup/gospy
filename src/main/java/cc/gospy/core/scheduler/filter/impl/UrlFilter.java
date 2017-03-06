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

package cc.gospy.core.scheduler.filter.impl;

import cc.gospy.core.scheduler.Task;
import cc.gospy.core.scheduler.filter.TaskFilter;

import java.util.regex.Pattern;

public class UrlFilter implements TaskFilter {
    private Pattern pattern;

    public UrlFilter(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    @Override
    public boolean validate(Task task) {
        assert task.getUrl() != null;
        return pattern.matcher(task.getUrl()).matches();
    }

    public static void main(String[] args) {
        // TODO TEST
        TaskFilter filter = new UrlFilter("1*2");
        System.out.println(filter.validate(new Task("11112")));
    }
}
