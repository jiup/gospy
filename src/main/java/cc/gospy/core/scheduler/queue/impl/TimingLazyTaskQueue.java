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

import cc.gospy.core.Task;
import cc.gospy.core.scheduler.queue.LazyTaskHandler;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.PriorityBlockingQueue;

public class TimingLazyTaskQueue extends LazyTaskQueue {
    private static Logger logger = LoggerFactory.getLogger(TimingLazyTaskQueue.class);
    private Thread checkThread = null;
    private volatile int checkPeriod = 60; // in seconds

    public TimingLazyTaskQueue(LazyTaskHandler handler) {
        this(5, handler);
    }

    public TimingLazyTaskQueue(int initialCapacity, LazyTaskHandler handler) {
        super(handler);
        this.lazyTaskQueue = new PriorityBlockingQueue<>(initialCapacity, (t0, t1)
                -> (int) ((t0.getLastVisitTime() + t0.getExpectedVisitPeriod() * 1000)
                - (t1.getLastVisitTime() + t1.getExpectedVisitPeriod() * 1000)));
    }

    class TimingLazyTaskChecker implements Runnable {
        public void run() {
            while (checkPeriod != 0) {
                try {
                    Thread.sleep(checkPeriod * 1000);
                    while (poll() != null) {
                        if (checkPeriod == 0) break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }
            }
            checkThread = null;
            checkPeriod = 60;
        }
    }

    @Override
    protected boolean ready() {
        Task peakTask = peek();
        return peakTask != null &&
                peakTask.getExpectedVisitPeriod() != 0 &&
                System.currentTimeMillis() - peakTask.getLastVisitTime()
                        > peakTask.getExpectedVisitPeriod() * 1000;
    }

    @Override
    public void stop() {
        if (checkThread != null) {
            checkThread.interrupt();
            logger.info("Lazy task queue stopped.");
        }
        checkPeriod = 0;
    }

    @Override
    public boolean add(Task task) {
        int taskExpectedRecallCycle = task.getExpectedVisitPeriod();
        if (taskExpectedRecallCycle > 0 && taskExpectedRecallCycle < checkPeriod) {
            checkPeriod = taskExpectedRecallCycle;
        }
        if (checkThread == null) {
            checkThread = new Thread(new TimingLazyTaskChecker());
            checkThread.start();
        }
        return super.add(task);
    }

    @Override
    public synchronized Task poll() {
        Task task = super.poll();
        if (lazyTaskQueue.size() == 0) {
            checkPeriod = 0;
        }
        return task;
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
