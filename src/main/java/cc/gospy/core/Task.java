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

package cc.gospy.core;

import java.io.Serializable;
import java.util.Map;

public class Task implements Serializable, Comparable<Task> {

    public enum Priority {EMERGENCY, HIGH, MEDIUM, LOW}

    private Priority priority;
    private String url;
    private String protocol;
    private Map<String, Object> extra;
    private long createTime;
    private long lastVisitTime;
    private int depth;
    private int expectedVisitPeriod; // in seconds, period should less than 24 days
    private int visitCount;

    public Task(String url) {
        this(Priority.MEDIUM, url, 0, 0);
    }

    public Task(Priority priority, String url, int depth, int expectedVisitPeriod) {
        assert url != null;
        this.priority = priority;
        this.url = url;
        this.depth = depth;
        this.createTime = System.currentTimeMillis();
        this.expectedVisitPeriod = expectedVisitPeriod;
        this.init();
    }

    private void init() {
        int prefixIndex = url.indexOf(":/");
        if (prefixIndex > 0)
            protocol = url.substring(0, prefixIndex);
        else protocol = "undefined";
    }

    @Override
    public int compareTo(Task task) {
        return this.getPriority().ordinal() - task.getPriority().ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Task task = (Task) o;

        return url.concat(extra.toString()).equals(task.url.concat(task.extra.toString()));
    }

    @Override
    public int hashCode() {
        return url.concat(extra.toString()).hashCode();
    }

    @Override
    public String toString() {
        return "Task{" + priority.name().charAt(0) + "-" + url + '}';
    }

    public void addVisitCount() {
        visitCount++;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setExpectedVisitPeriod(int expectedVisitPeriod) {
        this.expectedVisitPeriod = expectedVisitPeriod;
    }

    public void setLastVisitTime(long lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getUrl() {
        return url;
    }

    public String getProtocol() {
        return protocol;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getLastVisitTime() {
        return lastVisitTime;
    }

    public int getDepth() {
        return depth;
    }

    public int getExpectedVisitPeriod() {
        return expectedVisitPeriod;
    }

    public int getVisitCount() {
        return visitCount;
    }
}
