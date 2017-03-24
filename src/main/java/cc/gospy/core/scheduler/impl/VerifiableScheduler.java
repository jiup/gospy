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
import cc.gospy.core.scheduler.filter.impl.HashDuplicateRemover;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import cc.gospy.core.scheduler.queue.TaskQueue;
import cc.gospy.core.scheduler.queue.impl.FIFOTaskQueue;
import cc.gospy.core.scheduler.queue.impl.TimingLazyTaskQueue;
import cc.gospy.core.util.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

@Experimental
public class VerifiableScheduler extends GeneralScheduler implements Verifiable {
    private static final Logger logger = LoggerFactory.getLogger(VerifiableScheduler.class);

    private Set<Task> pendingTasks = new ConcurrentSkipListSet<>();
    private Map<String, Long> totalTaskDistributeCounts = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, Long> pendingTaskDistributeCounts = Collections.synchronizedMap(new LinkedHashMap<>());
    private Thread checkerThread;
    private int pendingTimeInSeconds;
    private boolean checkerRunning;
    private boolean autoExit;

    VerifiableScheduler(TaskQueue taskQueue,
                        LazyTaskQueue lazyTaskQueue,
                        DuplicateRemover duplicateRemover,
                        TaskFilter filter,
                        int pendingTimeInSeconds,
                        boolean autoExit) {
        super(taskQueue, lazyTaskQueue, duplicateRemover, filter);
        this.pendingTimeInSeconds = pendingTimeInSeconds;
        this.autoExit = autoExit;
    }

    @Override
    public synchronized void addTask(String executorAddress, Task task) {
        super.addTask(executorAddress, task);
    }

    @Override
    public void addLazyTask(String executorAddress, Task task) {
        super.addLazyTask(executorAddress, task);
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

    public void checkerTrigger() {
        if (checkerThread == null) {
            checkerThread = new PendingTaskChecker(pendingTimeInSeconds);
            checkerRunning = true;
            checkerThread.start();
        }
    }

    @Experimental
    public void exitTrigger() {
        if (autoExit && pendingTasks.size() == 0 && taskQueue.size() == 0 && lazyTaskQueue.size() == 0) {
            // In Gospy, other components' activities are invisible to a scheduler, so this scheduler
            // will shutdown itself in few seconds, which is naturally adapting to distribution
            // environments. However, in standalone programs, this might cause a premature interruption,
            // we cannot ensure other components' (such as a pipeline) functions are finished. Thus,
            // if you are running your program in standalone mode and want to keep the subsequent
            // process completely done (might in minutes), please turn off the auto exit.
            logger.info("All tasks are fed back, thus it will exit in few seconds.");
            try {
                Thread.currentThread().join(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Bye!");
            System.exit(0);
        }
    }

    @Override
    public void feedback(String fetcherId, Task task) {
        synchronized (pendingTasks) {
            synchronized (pendingTaskDistributeCounts) {
                if (!pendingTasks.contains(task)) {
                    return; // task has been reported by someone
                }
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

    private class PendingTaskChecker extends Thread {
        private int pendingTimeInSeconds;

        public PendingTaskChecker(int pendingTimeInSeconds) {
            this.pendingTimeInSeconds = pendingTimeInSeconds;
        }

        @Override
        public void run() {
            while (checkerRunning) {
                try {
                    sleep(pendingTimeInSeconds * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Iterator<Task> iterator = pendingTasks.iterator();
                while (iterator.hasNext()) {
                    Task task = iterator.next();
                    if (System.currentTimeMillis() - task.getLastVisitTime() > pendingTimeInSeconds * 1000) {
                        // tasks are recommended to be inserted into head.
                        // notice that this does not apply to a FIFO queue!
                        task.setPriority(Task.Priority.EMERGENCY);
                        taskQueue.add(task);
                        iterator.remove();
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
    public synchronized void pause(String dir) throws Throwable {
        // abort receiving any feedback, moving the pending tasks back to the task queue
        // and wait for the future suspending.
        pendingTasks.forEach(task -> task.setPriority(Task.Priority.EMERGENCY));
        taskQueue.addAll(pendingTasks);
        super.pause(dir);
    }

    public static VerifiableScheduler getDefault() {
        return new Builder().build();
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder extends GeneralScheduler.Builder {
        private VerifiableScheduler scheduler;
        private TaskQueue tq = new FIFOTaskQueue();
        private LazyTaskQueue ltq = new TimingLazyTaskQueue(wakedTask -> scheduler.addTask(null, wakedTask));
        private DuplicateRemover dr = new HashDuplicateRemover();
        private TaskFilter tf = TaskFilter.HTTP_DEFAULT;
        private int pt = 10;
        private boolean ae = true;

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

        public Builder setPendingTimeInSeconds(int pendingTimeInSeconds) {
            pt = pendingTimeInSeconds;
            return this;
        }

        public Builder setAutoExit(boolean autoExit) {
            ae = autoExit;
            return this;
        }

        public VerifiableScheduler build() {
            return scheduler = new VerifiableScheduler(tq, ltq, dr, tf, pt, ae);
        }
    }

}
