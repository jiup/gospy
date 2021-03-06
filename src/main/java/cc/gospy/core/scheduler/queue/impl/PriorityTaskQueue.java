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
import cc.gospy.core.scheduler.queue.TaskQueue;

import java.util.Iterator;
import java.util.PriorityQueue;

public class PriorityTaskQueue extends TaskQueue {
    private final PriorityQueue<Task> taskQueue;

    public PriorityTaskQueue() {
        this(5);
    }

    public PriorityTaskQueue(int initialCapacity) {
        this.taskQueue = new PriorityQueue<>(initialCapacity);
    }

    @Override
    public Iterator<Task> iterator() {
        return taskQueue.iterator();
    }

    @Override
    public int size() {
        return taskQueue.size();
    }

    @Override
    public Task poll() {
        synchronized (taskQueue) {
            return taskQueue.poll();
        }
    }

    @Override
    public Task peek() {
        return taskQueue.peek();
    }

    @Override
    public boolean add(Task task) {
        synchronized (taskQueue) {
            return taskQueue.add(task);
        }
    }

    @Override
    public boolean offer(Task task) {
        synchronized (taskQueue) {
            return taskQueue.offer(task);
        }
    }
}
