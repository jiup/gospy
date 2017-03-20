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

import cc.gospy.core.Observable;
import cc.gospy.core.Recoverable;
import cc.gospy.core.TaskFilter;
import cc.gospy.core.entity.Task;
import cc.gospy.core.scheduler.Scheduler;
import cc.gospy.core.scheduler.filter.DuplicateRemover;
import cc.gospy.core.scheduler.filter.impl.HashDuplicateRemover;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import cc.gospy.core.scheduler.queue.TaskQueue;
import cc.gospy.core.scheduler.queue.impl.FIFOTaskQueue;
import cc.gospy.core.scheduler.queue.impl.TimingLazyTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class GeneralScheduler implements Scheduler, Observable, Recoverable {
    private static Logger logger = LoggerFactory.getLogger(GeneralScheduler.class);

    private volatile AtomicLong totalTaskInputCount;
    private volatile AtomicLong totalTaskOutputCount;
    private volatile long firstVisitTimeMillis;
    private final TaskQueue taskQueue;
    private final LazyTaskQueue lazyTaskQueue;
    private DuplicateRemover duplicateRemover;
    private TaskFilter taskFilter;

    private GeneralScheduler(TaskQueue taskQueue
            , LazyTaskQueue lazyTaskQueue
            , DuplicateRemover duplicateRemover
            , TaskFilter filter) {
        this.taskQueue = taskQueue;
        this.lazyTaskQueue = lazyTaskQueue;
        this.duplicateRemover = duplicateRemover;
        this.taskFilter = filter;
        this.totalTaskInputCount = new AtomicLong();
        this.totalTaskOutputCount = new AtomicLong();
    }

    @Override
    public Task getTask() {
        if (firstVisitTimeMillis == 0) {
            firstVisitTimeMillis = System.currentTimeMillis();
        }
        if (taskQueue.size() > 0) {
            Task task = taskQueue.poll();
            duplicateRemover.record(task);
            totalTaskOutputCount.getAndIncrement();
            return task;
        }
        return null;
    }

    private void addTask0(Task task) {
        if (task.getExpectedVisitPeriod() == 0) {
            taskQueue.add(task);
        } else {
            lazyTaskQueue.add(task);
        }
    }

    @Override
    public void addTask(Task task) {
        totalTaskInputCount.getAndIncrement();
        if (task.isCheckSkipping()) {
            addTask0(task);
            return;
        }
        if (!taskFilter.test(task)) {
            return;
        }
        if (duplicateRemover.exists(task)) {
            duplicateRemover.record(task);
        } else {
            addTask0(task);
        }
        return;
    }

    @Override
    public void addLazyTask(Task task) {
        lazyTaskQueue.add(task);
    }

    @Override
    public void stop() {
        lazyTaskQueue.stop();
    }

    @Override
    public long getTotalTaskInputCount() {
        return totalTaskInputCount.get();
    }

    @Override
    public long getTotalTaskOutputCount() {
        return totalTaskOutputCount.get();
    }

    @Override
    public long getRecodedTaskSize() {
        return duplicateRemover.size();
    }

    @Override
    public long getCurrentTaskQueueSize() {
        return taskQueue.size();
    }

    @Override
    public long getCurrentLazyTaskQueueSize() {
        return lazyTaskQueue.size();
    }

    @Override
    public long getRunningTimeMillis() {
        return System.currentTimeMillis() - firstVisitTimeMillis;
    }

    public static GeneralScheduler getDefault() {
        return new Builder().build();
    }

    public static Builder custom() {
        return new Builder();
    }

    @Override
    public void pause() {
        // TODO
    }

    @Override
    public void resume() {
        // TODO
    }

    public static class Builder {
        private GeneralScheduler scheduler;
        private TaskQueue tq = new FIFOTaskQueue();
        private LazyTaskQueue ltq = new TimingLazyTaskQueue(wakedTask -> scheduler.addTask(wakedTask));
        private DuplicateRemover dr = new HashDuplicateRemover();
        private TaskFilter tf = TaskFilter.HTTP_DEFAULT;

        public Builder setTaskQueue(TaskQueue taskQueue) {
            tq = taskQueue;
            return this;
        }

        public Builder setLazyTaskQueue(LazyTaskQueue lazyTaskQueue) {
            ltq = lazyTaskQueue;
            return this;
        }

        public Builder setDuplicateRemover(DuplicateRemover duplicateRemover) {
            dr = duplicateRemover;
            return this;
        }

        public Builder setTaskFilter(TaskFilter taskFilter) {
            tf = taskFilter;
            return this;
        }

        public GeneralScheduler build() {
            return scheduler = new GeneralScheduler(tq, ltq, dr, tf);
        }
    }

}
