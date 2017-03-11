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

package cc.gospy.core.scheduler;

import cc.gospy.core.Task;
import cc.gospy.core.TaskFilter;
import cc.gospy.core.scheduler.filter.DuplicateRemover;
import cc.gospy.core.scheduler.filter.impl.HashDuplicateRemover;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import cc.gospy.core.scheduler.queue.TaskQueue;
import cc.gospy.core.scheduler.queue.impl.FIFOTaskQueue;
import cc.gospy.core.scheduler.queue.impl.TimingLazyTaskQueue;

public class GeneralScheduler implements Scheduler {
    private TaskQueue taskQueue;
    private LazyTaskQueue lazyTaskQueue;
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
    }

    @Override
    public Task getTask() {
        if (taskQueue.size() > 0) {
            Task task = taskQueue.poll();
            duplicateRemover.record(task);
            return task;
        }
        return null;
    }

    @Override
    public Scheduler addTask(Task task) {
        if (!taskFilter.test(task)) {
            return this;
        }
        if (duplicateRemover.exists(task)) {
            duplicateRemover.record(task);
        } else {
            taskQueue.add(task);
        }
        return this;
    }

    @Override
    public Scheduler addLazyTask(Task task) {
        lazyTaskQueue.add(task);
        return this;
    }

    @Override
    public void stop() {
        lazyTaskQueue.stop();
    }


    public static GeneralScheduler getDefault() {
        return new Builder().Build();
    }

    public static Builder custom() {
        return new Builder();
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

        public GeneralScheduler Build() {
            return scheduler = new GeneralScheduler(tq, ltq, dr, tf);
        }
    }

}
