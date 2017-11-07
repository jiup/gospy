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
import cc.gospy.core.scheduler.Observable;
import cc.gospy.core.scheduler.Recoverable;
import cc.gospy.core.scheduler.Scheduler;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import cc.gospy.core.scheduler.queue.TaskQueue;
import cc.gospy.core.scheduler.queue.impl.FIFOTaskQueue;
import cc.gospy.core.scheduler.queue.impl.TimingLazyTaskQueue;
import cc.gospy.core.scheduler.remover.DuplicateRemover;
import cc.gospy.core.scheduler.remover.impl.HashDuplicateRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public class GeneralScheduler implements Scheduler, Observable, Recoverable {
    private static final Logger logger = LoggerFactory.getLogger(GeneralScheduler.class);

    private volatile AtomicBoolean isSuspend;
    private final LongAdder totalTaskInput;
    private final LongAdder totalTaskOutput;
    private long firstVisitTimeMillis;

    TaskQueue taskQueue;
    LazyTaskQueue lazyTaskQueue;
    DuplicateRemover duplicateRemover;
    TaskFilter taskFilter;

    GeneralScheduler(TaskQueue taskQueue
            , LazyTaskQueue lazyTaskQueue
            , DuplicateRemover duplicateRemover
            , TaskFilter filter) {
        this.taskQueue = taskQueue;
        this.lazyTaskQueue = lazyTaskQueue;
        this.duplicateRemover = duplicateRemover;
        this.taskFilter = filter;
        this.totalTaskInput = new LongAdder();
        this.totalTaskOutput = new LongAdder();
        this.isSuspend = new AtomicBoolean();
    }

    @Override
    public Task getTask(String fetcherId) {
        if (isSuspend.get()) {
            return null;
        }
        if (firstVisitTimeMillis == 0) {
            firstVisitTimeMillis = System.currentTimeMillis();
        }
        synchronized (taskQueue) {
            if (taskQueue.size() > 0) {
                final Task task = taskQueue.poll();
                try {
                    task.setLastVisitTimeMillis(System.currentTimeMillis());
                    synchronized (duplicateRemover) {
                        duplicateRemover.record(task); // add to duplicate remover
                    }
                    return task;
                } finally {
                    totalTaskOutput.increment();
                }
            }
        }
        return null;
    }

    private void addTask0(final Task task) {
        if (task.getExpectedVisitInSeconds() == 0) {
            synchronized (taskQueue) {
                taskQueue.add(task);
            }
        } else {
            synchronized (lazyTaskQueue) {
                lazyTaskQueue.add(task);
            }
        }
    }

    @Override
    public void addTask(String executorId, Task task) {
        if (isSuspend.get()) {
            return;
        }
        totalTaskInput.increment();
        if (task.isCheckSkipping()) {
            addTask0(task);
            return;
        }
        if (!taskFilter.test(task)) {
            return;
        }
        if (duplicateRemover.exists(task)) {
            duplicateRemover.record(task); // record a crash
        } else {
            addTask0(task);
        }
    }

    @Override
    public void addLazyTask(String executorId, Task task) {
        if (!isSuspend.get() && (task.isCheckSkipping() || taskFilter.test(task))) {
            lazyTaskQueue.add(task);
        }
    }

    @Override
    public void stop() {
        lazyTaskQueue.stop();
    }

    @Override
    public long getTotalTaskInputCount() {
        return totalTaskInput.sum();
    }

    @Override
    public long getTotalTaskOutputCount() {
        return totalTaskOutput.sum();
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

    @Override
    public synchronized void pause(String dir) throws Throwable {
        if (isSuspend.get()) {
            logger.error("the scheduler has already suspended.");
//            throw new RuntimeException("the scheduler has already suspended.");
        }

        if (duplicateRemover instanceof Recoverable) {
            synchronized (duplicateRemover) {
                ((Recoverable) duplicateRemover).pause(dir);
            }
        } else {
            throw new RuntimeException(duplicateRemover.getClass().getTypeName() + " is not recoverable.");
        }

        File file = new File(dir, this.getClass().getTypeName() + ".tmp");
        logger.info("Writing scheduler data to {}", file.getPath());
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file, false))) {
            outputStream.writeLong(firstVisitTimeMillis);
            outputStream.writeLong(totalTaskInput.sum());
            outputStream.writeLong(totalTaskOutput.sum());

            // notice that the lazy-tasks will not lazy load later, they are
            // directly appended to the rear of the activate-task-queue.
            synchronized (taskQueue) {
                synchronized (lazyTaskQueue) {
                    lazyTaskQueue.dump().forEachRemaining(task -> taskQueue.add(task));
                    lazyTaskQueue.stop();
                }
                outputStream.writeObject(taskQueue);
                taskQueue.clear();
            }
            outputStream.writeObject(taskFilter);
        }

        isSuspend.set(true);
        logger.info("The scheduler is successfully suspended.");
    }

    @Override
    public void resume(String dir) throws Throwable {
//        if (!isSuspend.get()) {
//            throw new RuntimeException("the scheduler has already recovered.");
//        }

        if (duplicateRemover instanceof Recoverable) {
            ((Recoverable) duplicateRemover).resume(dir);
        } else {
            throw new RuntimeException(duplicateRemover.getClass().getTypeName() + " is not recoverable.");
        }

        File file = new File(dir, this.getClass().getTypeName() + ".tmp");
        logger.info("Reading scheduler data from {}", file.getPath());
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
            this.firstVisitTimeMillis = inputStream.readLong();
            this.totalTaskInput.reset();
            this.totalTaskOutput.reset();
            this.totalTaskInput.add(inputStream.readLong());
            this.totalTaskOutput.add(inputStream.readLong());
            this.taskQueue = (TaskQueue) inputStream.readObject();
            this.taskFilter = (TaskFilter) inputStream.readObject();
        }
        isSuspend.set(false);
        logger.info("The scheduler is successfully recovered.");
    }

    public static GeneralScheduler getDefault() {
        return new Builder().build();
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder {
        private GeneralScheduler scheduler;
        private TaskQueue taskQueue = new FIFOTaskQueue();
        private LazyTaskQueue lazyTaskQueue = new TimingLazyTaskQueue(wakedTask -> {
            synchronized (taskQueue) {
                taskQueue.add(wakedTask);
            }
        });
        private DuplicateRemover remover = new HashDuplicateRemover();
        private TaskFilter filter = TaskFilter.ALLOW_ALL;

        public Builder setTaskQueue(TaskQueue taskQueue) {
            this.taskQueue = taskQueue;
            return this;
        }

        public Builder setLazyTaskQueue(LazyTaskQueue lazyTaskQueue) {
            this.lazyTaskQueue = lazyTaskQueue;
            return this;
        }

        public Builder setRemover(DuplicateRemover duplicateRemover) {
            remover = duplicateRemover;
            return this;
        }

        public Builder setTaskFilter(TaskFilter taskFilter) {
            filter = taskFilter;
            return this;
        }

        public GeneralScheduler build() {
            return scheduler = new GeneralScheduler(taskQueue, lazyTaskQueue, remover, filter);
        }
    }

}
