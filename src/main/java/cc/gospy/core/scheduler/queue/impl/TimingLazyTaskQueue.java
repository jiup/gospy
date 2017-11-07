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

package cc.gospy.core.scheduler.queue.impl;

import cc.gospy.core.entity.Task;
import cc.gospy.core.scheduler.queue.LazyTaskHandler;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

public class TimingLazyTaskQueue extends LazyTaskQueue {
    private static final Logger logger = LoggerFactory.getLogger(TimingLazyTaskQueue.class);
    private Thread checkThread = null;
    private int checkPeriodInSeconds = 60;

    public TimingLazyTaskQueue(LazyTaskHandler handler) {
        super(handler);
        this.lazyTaskQueue = new PriorityQueue<>((t0, t1)
                -> (t0.getLastVisitTimeMillis() + TimeUnit.SECONDS.toMillis(t0.getExpectedVisitInSeconds()))
                - (t1.getLastVisitTimeMillis() + TimeUnit.SECONDS.toMillis(t1.getExpectedVisitInSeconds())) < 0 ? -1 : 1);
    }

    class TimingLazyTaskChecker implements Runnable {
        public void run() {
            while (checkPeriodInSeconds != 0) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(checkPeriodInSeconds));
                    while (poll() != null) {
                        if (checkPeriodInSeconds == 0) break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
            checkThread = null;
            checkPeriodInSeconds = 60;
        }
    }

    @Override
    protected boolean ready() {
        Task peakTask = peek();
        return peakTask != null && (peakTask.getExpectedVisitInSeconds() == 0 ||
                (System.currentTimeMillis() - peakTask.getLastVisitTimeMillis() >
                        TimeUnit.SECONDS.toMillis(peakTask.getExpectedVisitInSeconds())));
    }

    @Override
    public void stop() {
        if (checkThread != null) {
            checkThread.interrupt();
            logger.info("Lazy task queue stopped.");
        }
        lazyTaskQueue.clear();
        checkPeriodInSeconds = 0;
        checkThread = null;
    }

    @Override
    public boolean add(Task task) {
        int taskExpectedRecallCycle = task.getExpectedVisitInSeconds();
        if (taskExpectedRecallCycle > 0 && (checkPeriodInSeconds == 0
                || taskExpectedRecallCycle < checkPeriodInSeconds)) {
            checkPeriodInSeconds = taskExpectedRecallCycle; // update ticks
        }
        if (checkThread == null) {
            checkThread = new Thread(new TimingLazyTaskChecker());
            checkThread.start();
        }
        synchronized (this) {
            return super.add(task);
        }
    }

    @Override
    public Task poll() {
        synchronized (this) {
            try {
                // return instantly even its null
                return super.poll();
            } finally {
                if (lazyTaskQueue.size() == 0) {
                    checkPeriodInSeconds = 0;
                }
            }
        }
    }

    @Override
    public Iterator<Task> iterator() {
        return lazyTaskQueue.iterator();
    }

    @Override
    public int size() {
        return lazyTaskQueue.size();
    }

    @Override
    public boolean offer(Task task) {
        return lazyTaskQueue.offer(task);
    }

    @Override
    public Task peek() {
        return lazyTaskQueue.peek();
    }
}
