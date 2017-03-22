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

package cc.gospy.core.scheduler.impl;

import cc.gospy.core.TaskFilter;
import cc.gospy.core.entity.Task;
import cc.gospy.core.scheduler.Verifiable;
import cc.gospy.core.scheduler.filter.DuplicateRemover;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import cc.gospy.core.scheduler.queue.TaskQueue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class VerifiableScheduler extends GeneralScheduler implements Verifiable {
    private Set<Task> pendingTasks = new ConcurrentSkipListSet<>();
    private Map<String, Long> totalTaskDistributeCounts = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, Long> pendingTaskDistributeCounts = Collections.synchronizedMap(new LinkedHashMap<>());

    VerifiableScheduler(TaskQueue taskQueue, LazyTaskQueue lazyTaskQueue, DuplicateRemover duplicateRemover, TaskFilter filter) {
        super(taskQueue, lazyTaskQueue, duplicateRemover, filter);
    }

    @Override
    public Task getTask(String fetcherId) {
        Task task = super.getTask(fetcherId);
        if (task == null) {
            return null;
        }
        synchronized (pendingTasks) {
            synchronized (totalTaskDistributeCounts) {
                synchronized (pendingTaskDistributeCounts) {
                    pendingTasks.add(task);
                    totalTaskDistributeCounts.put(fetcherId, totalTaskDistributeCounts.getOrDefault(fetcherId, 0L) + 1L);
                    pendingTaskDistributeCounts.put(fetcherId, pendingTaskDistributeCounts.getOrDefault(fetcherId, 0L) + 1);
                    return task;
                }
            }
        }
    }

    @Override
    public void feedback(String fetcherId, Task task) {
        synchronized (pendingTasks) {
            synchronized (pendingTaskDistributeCounts) {
                pendingTasks.remove(task);
                if (pendingTaskDistributeCounts.getOrDefault(fetcherId, 1L) == 1L) {
                    pendingTaskDistributeCounts.remove(fetcherId);
                } else {
                    pendingTaskDistributeCounts.put(fetcherId, pendingTaskDistributeCounts.get(fetcherId) - 1);
                }
            }
        }
    }

    @Override
    public long getPendingTaskSize() {
        return pendingTasks.size();
    }

    @Override
    public Map<String, Long> getTotalTaskDistributeCounts() {
        return totalTaskDistributeCounts;
    }

    @Override
    public Map<String, Long> getPendingTaskDistributeCounts() {
        return pendingTaskDistributeCounts;
    }
}
