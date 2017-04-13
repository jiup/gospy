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

package cc.gospy.core.entity;

import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class Task implements Serializable, Comparable<Task> {
    private static final Logger logger = LoggerFactory.getLogger(Task.class);

    // identify a unique task for duplicate remover
    public static final Funnel<Task> DIGEST = (task, primitiveSink) -> {
        primitiveSink.putString(task.url, Charset.defaultCharset());
        task.extra.forEach((k, v) -> primitiveSink.putString(k.concat("=").concat(v.toString().concat("\1")), Charset.defaultCharset()));
    };

    private int priority;
    private String url;
    private String host;
    private String protocol;
    private Map<String, Object> extra;
    private boolean skipCheck;
    private long createTime;
    private long lastVisitTime;
    private int depth;
    private int expectedVisitInSeconds; // 0 to off
    private int visitCount;

    public Task(String url) {
        this(Priority.MEDIUM, url, 0, 0);
    }

    public Task(Priority priority, String url, int depth, int expectedVisitInSeconds) {
        this(priority.getValue(), url, depth, expectedVisitInSeconds);
    }

    public Task(int priority, String url, int depth, int expectedVisitInSeconds) {
        assert url != null;
        this.priority = priority;
        this.url = url;
        this.depth = depth;
        this.createTime = System.currentTimeMillis();
        this.expectedVisitInSeconds = expectedVisitInSeconds;
        this.extra = new HashMap<>();
        this.resolveUrl();
    }

    private void resolveUrl() {
        int prefixIndex = url.indexOf("://");
        if (prefixIndex > 0) {
            protocol = url.substring(0, prefixIndex);
            host = url.substring(prefixIndex + 3);
            host = !host.contains("/") ? host : host.substring(0, host.indexOf('/'));
        } else {
            protocol = null;
            logger.warn("Unresolved protocol, for url [{}]", url);
        }
    }

    public void setUrl(String newUrl) {
        this.url = newUrl;
        this.resolveUrl();
    }

    public void addVisitCount() {
        visitCount++;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setPriority(Priority priority) {
        this.setPriority(priority.getValue());
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setExpectedVisitInSeconds(int expectedVisitInSeconds) {
        this.expectedVisitInSeconds = expectedVisitInSeconds;
    }

    public void setLastVisitTime(long lastVisitTime) {
        this.lastVisitTime = lastVisitTime;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    public void setSkipCheck(boolean skipCheck) {
        this.skipCheck = skipCheck;
    }

    public int getPriority() {
        return priority;
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public String getProtocol() {
        return protocol;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public boolean isCheckSkipping() {
        return skipCheck;
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

    public int getExpectedVisitInSeconds() {
        return expectedVisitInSeconds;
    }

    public int getVisitCount() {
        return visitCount;
    }

    @Override
    public int compareTo(Task task) {
        return this.getPriority() - task.getPriority();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return this.hashCode() == o.hashCode();
    }

    @Override
    public int hashCode() {
        return Hashing.murmur3_32().newHasher().putObject(this, DIGEST).hash().hashCode();
    }

    @Override
    public String toString() {
        return "Task-(" + priority + ")-[" + url + "]";
    }

    public enum Priority {
        EMERGENCY(0), HIGH(10), MEDIUM(20), LOW(30);

        private int value;

        Priority(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }
    }

}
