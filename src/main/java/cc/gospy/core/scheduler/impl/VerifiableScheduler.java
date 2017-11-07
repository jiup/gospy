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
import cc.gospy.core.scheduler.ExitCallback;
import cc.gospy.core.scheduler.Verifiable;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import cc.gospy.core.scheduler.queue.TaskQueue;
import cc.gospy.core.scheduler.queue.impl.FIFOTaskQueue;
import cc.gospy.core.scheduler.queue.impl.TimingLazyTaskQueue;
import cc.gospy.core.scheduler.remover.DuplicateRemover;
import cc.gospy.core.scheduler.remover.impl.HashDuplicateRemover;
import cc.gospy.core.util.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Experimental
public class VerifiableScheduler extends GeneralScheduler implements Verifiable {
    private static final Logger logger = LoggerFactory.getLogger(VerifiableScheduler.class);

    private Set<Task> pendingTasks = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Map<String, Long> totalTaskDistributeCounts = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, Long> pendingTaskDistributeCounts = Collections.synchronizedMap(new LinkedHashMap<>());
    private Thread checkerThread;
    private ExitCallback callback;
    private int pendingTimeInSeconds;
    private boolean checkerRunning;
    private boolean autoExit;

    VerifiableScheduler(TaskQueue taskQueue,
                        LazyTaskQueue lazyTaskQueue,
                        DuplicateRemover duplicateRemover,
                        TaskFilter filter,
                        ExitCallback callback,
                        int pendingTimeInSeconds,
                        int exitThresholdInSeconds,
                        boolean autoExit) {
        super(taskQueue, lazyTaskQueue, duplicateRemover, filter);
        this.pendingTimeInSeconds = pendingTimeInSeconds;
        this.callback = callback;
        this.exitVerifyTimeMillis = TimeUnit.SECONDS.toMillis(exitThresholdInSeconds);
        this.autoExit = autoExit;
    }

    @Override
    public void addTask(String executorId, Task task) {
        super.addTask(executorId, task);
    }

    @Override
    public void addLazyTask(String executorId, Task task) {
        super.addLazyTask(executorId, task);
    }

    @Override
    public Task getTask(String fetcherId) {
        Task task = super.getTask(fetcherId);
        if (task == null) {
            exitTrigger();
            return null;
        }
        checkerTrigger();
        synchronized (pendingTasks) {
            pendingTasks.add(task);
            synchronized (totalTaskDistributeCounts) {
                totalTaskDistributeCounts.put(fetcherId, totalTaskDistributeCounts.getOrDefault(fetcherId, 0L) + 1);
            }
            synchronized (pendingTaskDistributeCounts) {
                pendingTaskDistributeCounts.put(fetcherId, pendingTaskDistributeCounts.getOrDefault(fetcherId, 0L) + 1);
            }
            return task;
        }
    }

    public void checkerTrigger() {
        if (checkerThread == null) {
            checkerThread = new PendingTaskChecker(pendingTimeInSeconds);
            checkerRunning = true;
            checkerThread.start();
        }
    }

    long exitPendingTimeMillis;
    long exitVerifyTimeMillis;

    @Experimental
    public void exitTrigger() {
        if (autoExit && pendingTasks.size() == 0 && taskQueue.size() == 0 && lazyTaskQueue.size() == 0) {
            if (exitPendingTimeMillis == 0) {
                exitPendingTimeMillis = System.currentTimeMillis(); // set pending start time
            } else if (System.currentTimeMillis() - exitPendingTimeMillis > exitVerifyTimeMillis) {
                callback.onExit(); // trigger exit
            }
        } else {
            if (exitPendingTimeMillis != 0) {
                exitPendingTimeMillis = 0; // reset pending time
            }
        }
    }

    @Override
    public void feedback(String fetcherId, Task task) {
        synchronized (pendingTasks) {
            if (!pendingTasks.contains(task)) {
                return; // task has been reported by someone
            }
            pendingTasks.remove(task);
            synchronized (pendingTaskDistributeCounts) {
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

    private class PendingTaskChecker extends Thread {
        private int pendingTimeInSeconds;

        public PendingTaskChecker(int pendingTimeInSeconds) {
            this.pendingTimeInSeconds = pendingTimeInSeconds;
        }

        @Override
        public void run() {
            while (checkerRunning) {
                try {
                    sleep(TimeUnit.SECONDS.toMillis(pendingTimeInSeconds));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (Iterator<Task> iterator = pendingTasks.iterator(); iterator.hasNext(); ) {
                    Task task = iterator.next();
                    if (task == null) {
                        iterator.remove();
                        continue;
                    }
                    if (System.currentTimeMillis() - task.getLastVisitTimeMillis() > TimeUnit.SECONDS.toMillis(pendingTimeInSeconds)) {
                        // tasks are recommended to be inserted into head.
                        // notice that this does not apply to a FIFO queue!
                        task.setPriority(Task.Priority.EMERGENCY);
                        taskQueue.add(task);
                        logger.warn("{} pending timeout, re-add to queue.", task);
                    } else {
                        break;
                    }
                }
            }
            logger.info("Pending task checker is stopped.");
        }
    }

    @Override
    public void stop() {
        checkerRunning = false;
        super.stop();
    }

    @Override
    public void pause(String dir) throws Throwable {
        // abort receiving any feedback, moving the pending tasks back to the task queue
        // and wait for the future suspending.
        synchronized (pendingTasks) {
            pendingTasks.forEach(task -> task.setPriority(Task.Priority.EMERGENCY));
            synchronized (taskQueue) {
                taskQueue.addAll(pendingTasks);
            }
        }
        super.pause(dir);
    }

    public static VerifiableScheduler getDefault() {
        return new Builder().build();
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder extends GeneralScheduler.Builder {
        private TaskQueue taskQueue = new FIFOTaskQueue();
        private LazyTaskQueue lazyTaskQueue = new TimingLazyTaskQueue(wakedTask -> taskQueue.add(wakedTask));
        private DuplicateRemover remover = new HashDuplicateRemover();
        private TaskFilter filter = TaskFilter.ALLOW_ALL;
        private ExitCallback exitCallback = ExitCallback.DEFAULT;
        private int pendingTimeInSeconds = 10;
        private int exitThresholdInSeconds = 5;
        private boolean ae = true;

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

        public Builder setExitCallback(ExitCallback callback) {
            exitCallback = callback;
            return this;
        }

        public Builder setTaskFilter(TaskFilter taskFilter) {
            filter = taskFilter;
            return this;
        }

        public Builder setPendingTimeInSeconds(int pendingTimeInSeconds) {
            this.pendingTimeInSeconds = pendingTimeInSeconds;
            return this;
        }

        public Builder setExitThresholdInSeconds(int exitThresholdInSeconds) {
            this.exitThresholdInSeconds = exitThresholdInSeconds;
            return this;
        }

        @Experimental
        public Builder setAutoExit(boolean autoExit) {
            ae = autoExit;
            return this;
        }

        public VerifiableScheduler build() {
            return new VerifiableScheduler(taskQueue, lazyTaskQueue, remover, filter, exitCallback, pendingTimeInSeconds, exitThresholdInSeconds, ae);
        }
    }

}
