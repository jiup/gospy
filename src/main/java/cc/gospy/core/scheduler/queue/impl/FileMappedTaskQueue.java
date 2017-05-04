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
import cc.gospy.core.util.base.FileMappedQueue;

import java.io.IOException;
import java.util.Iterator;

public class FileMappedTaskQueue extends TaskQueue {
    private FileMappedQueue<Task> tasks;

    public FileMappedTaskQueue(String dir) throws IOException {
        tasks = new FileMappedQueue<>(dir);
    }

    @Override
    public Iterator<Task> iterator() {
        return tasks.iterator();
    }

    @Override
    public int size() {
        return tasks.size();
    }

    @Override
    public boolean offer(Task task) {
        return tasks.offer(task);
    }

    @Override
    public Task poll() {
        return tasks.poll();
    }

    @Override
    public Task peek() {
        return tasks.peek();
    }
}
