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

package cc.gospy.core.scheduler.filter.impl;

import cc.gospy.core.entity.Task;
import cc.gospy.core.scheduler.Recoverable;
import cc.gospy.core.scheduler.filter.DuplicateRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HashDuplicateRemover implements DuplicateRemover, Recoverable {
    private static final Logger logger = LoggerFactory.getLogger(HashDuplicateRemover.class);
    private Map<Task, AtomicInteger> tasks = new ConcurrentHashMap<>();

    @Override
    public void record(final Task task) {
        synchronized (this) {
            AtomicInteger counter = tasks.get(task);
            if (counter != null) {
                counter.incrementAndGet();
            } else {
                tasks.put(task, new AtomicInteger(1));
            }
        }
    }

    @Override
    public void delete(final Task task) {
        tasks.remove(task);
    }

    @Override
    public boolean exists(final Task task) {
        return tasks.containsKey(task);
    }

    @Override
    public long size() {
        return tasks.size();
    }

    @Override
    public synchronized void pause(String dir) throws Throwable {
        File file = new File(dir, this.getClass().getTypeName() + ".tmp");
        logger.info("Saving hash filter data to file {}...", file.getPath());
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file, false))) {
            outputStream.writeObject(tasks);
        }
        logger.info("Hash filter data is successfully saved.");
    }

    @Override
    public synchronized void resume(String dir) throws Throwable {
        File file = new File(dir, this.getClass().getTypeName() + ".tmp");
        logger.info("Reading hash filter data from file {}...", file.getPath());
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
            tasks = (Map<Task, AtomicInteger>) inputStream.readObject();
        }
        logger.info("Hash filter data is successfully loaded.");
    }
}
