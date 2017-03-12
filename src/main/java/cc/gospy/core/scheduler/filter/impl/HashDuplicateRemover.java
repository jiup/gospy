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

import cc.gospy.core.Task;
import cc.gospy.core.scheduler.filter.DuplicateRemover;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HashDuplicateRemover implements DuplicateRemover {
    private Map<Task, AtomicInteger> tasks = new ConcurrentHashMap<>();

    @Override
    public void record(final Task task) {
        synchronized (this) {
            AtomicInteger counter = tasks.get(task);
            if (counter != null) {
                counter.getAndIncrement();
            } else {
                tasks.put(task, new AtomicInteger(1));
            }
        }
    }

    @Override
    public void delete(final Task task) {
        tasks.remove(task);
    }

    @Override
    public boolean exists(final Task task) {
        return tasks.containsKey(task);
    }

    @Override
    public long size() {
        return tasks.size();
    }

}
