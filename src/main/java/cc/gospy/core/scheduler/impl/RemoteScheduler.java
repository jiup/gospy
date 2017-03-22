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

import cc.gospy.core.entity.Task;
import cc.gospy.core.scheduler.Scheduler;
import cc.gospy.core.scheduler.Verifiable;

import java.util.Map;

public class RemoteScheduler implements Scheduler, Verifiable {
    @Override
    public Task getTask(String fetcherId) {
        return null;
    }

    @Override
    public void addTask(String executorAddress, Task task) {

    }

    @Override
    public void addLazyTask(String executorAddress, Task task) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void feedback(String fetcherId, Task task) {

    }

    @Override
    public long getPendingTaskSize() {
        return 0;
    }

    @Override
    public Map<String, Long> getTotalTaskDistributeCounts() {
        return null;
    }

    @Override
    public Map<String, Long> getPendingTaskDistributeCounts() {
        return null;
    }
}
