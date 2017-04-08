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

import cc.gospy.core.remote.rpc.RemoteComponent;
import cc.gospy.core.entity.Task;
import cc.gospy.core.scheduler.Observable;
import cc.gospy.core.scheduler.Recoverable;
import cc.gospy.core.scheduler.Scheduler;
import cc.gospy.core.scheduler.Verifiable;
import hprose.client.HproseClient;
import hprose.io.HproseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RemoteScheduler implements Scheduler, RemoteComponent, Verifiable, Recoverable, Observable {
    private static Logger logger = LoggerFactory.getLogger(RemoteScheduler.class);

    private HproseClient client;
    private Scheduler scheduler;
    private String identifier;

    private RemoteScheduler(String[] uriList) {
        this.init(uriList);
    }

    public static Builder custom() {
        return new Builder();
    }

    private void init(String[] uriList) {
        try {
            logger.info("Connecting to remote scheduler...");
            this.client = HproseClient.create(uriList, HproseMode.MemberMode);
            this.scheduler = client.useService(Scheduler.class);
            this.identifier = String.valueOf(client.invoke("getIdentifier"));
            client.setIdempotent(true);
            client.setRetry(2);
            logger.info("Remote scheduler [{}] initialized.", identifier);
        } catch (Throwable throwable) {
            logger.error("Remote scheduler initialization failed ({})", throwable.getMessage());
            this.client.close();
            throwable.printStackTrace();
            throw new RuntimeException(throwable.getMessage());
        }
    }

    @Override
    public Task getTask(String fetcherId) {
        if (fetcherId == null) {
            fetcherId = "undefined";
        }
        return scheduler.getTask(fetcherId);
    }

    @Override
    public void addTask(String executorId, Task task) {
        if (task == null) {
            return;
        }
        if (executorId == null) {
            executorId = "undefined";
        }
        scheduler.addTask(executorId, task);
    }

    @Override
    public void addLazyTask(String executorId, Task task) {
        if (task == null) {
            return;
        }
        if (executorId == null) {
            executorId = "undefined";
        }
        scheduler.addLazyTask(executorId, task);
    }

    @Override
    public void pause(String dir) throws Throwable {
        try {
            client.invoke("pause", new Object[]{dir});
        } catch (Throwable throwable) {
            throw new RuntimeException("remote scheduler [" + identifier + "] is not recoverable");
        }
    }

    @Override
    public void resume(String dir) throws Throwable {
        try {
            client.invoke("resume", new Object[]{dir});
        } catch (Throwable throwable) {
            throw new RuntimeException("remote scheduler [" + identifier + "] is not recoverable");
        }
    }

    @Override
    public void stop() {
        try {
            client.invoke("stop");
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable.getMessage());
        }
    }

    @Override
    public void feedback(String fetcherId, Task task) {
        if (task == null) {
            return;
        }
        if (fetcherId == null) {
            fetcherId = "undefined";
        }
        try {
            client.invoke("feedback", new Object[]{fetcherId, task});
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
    }

    @Override
    public long getPendingTaskSize() {
        try {
            return client.invoke("getPendingTaskSize", long.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return -1;
    }

    @Override
    public Map<String, Long> getTotalTaskDistributeCounts() {
        try {
            return client.invoke("getTotalTaskDistributeCounts", Map.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return null;
    }

    @Override
    public Map<String, Long> getPendingTaskDistributeCounts() {
        try {
            return client.invoke("getPendingTaskDistributeCounts", Map.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return null;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void quit(String originator) {
        try {
            client.invoke("quit", new Object[]{originator});
            client.close();
            logger.info("Remote scheduler [{}] terminated.", identifier);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable.getMessage());
        }
    }

    @Override
    public long getTotalTaskInputCount() {
        try {
            return client.invoke("getTotalTaskInputCount", long.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return -1;
    }

    @Override
    public long getTotalTaskOutputCount() {
        try {
            return client.invoke("getTotalTaskOutputCount", long.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return -1;
    }

    @Override
    public long getRecodedTaskSize() {
        try {
            return client.invoke("getRecodedTaskSize", long.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return -1;
    }

    @Override
    public long getCurrentTaskQueueSize() {
        try {
            return client.invoke("getCurrentTaskQueueSize", long.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return -1;
    }

    @Override
    public long getCurrentLazyTaskQueueSize() {
        try {
            return client.invoke("getCurrentLazyTaskQueueSize", long.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return -1;
    }

    @Override
    public long getRunningTimeMillis() {
        try {
            return client.invoke("getRunningTimeMillis", long.class);
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
        return -1;
    }

    public static class Builder {
        private String[] uri;

        public Builder setUri(String... uri) {
            this.uri = uri;
            return this;
        }

        public RemoteScheduler build() throws Throwable {
            if (uri == null) {
                throw new RuntimeException("Uri list (for remote scheduler) not specified, please check your code.");
            }
            return new RemoteScheduler(uri);
        }
    }
}
