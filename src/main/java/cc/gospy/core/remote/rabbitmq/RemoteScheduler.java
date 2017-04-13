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

package cc.gospy.core.remote.rabbitmq;

import cc.gospy.core.entity.Task;
import cc.gospy.core.remote.RemoteComponent;
import cc.gospy.core.scheduler.Observable;
import cc.gospy.core.scheduler.Recoverable;
import cc.gospy.core.scheduler.Scheduler;
import cc.gospy.core.scheduler.Verifiable;
import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static cc.gospy.core.remote.rabbitmq.TaskQueue.*;

public class RemoteScheduler implements Scheduler, RemoteComponent, Verifiable, Recoverable, Observable {
    private static final Logger logger = LoggerFactory.getLogger(RemoteScheduler.class);

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String[] targetQueueNames;
    private Map<Task, Long> tasks;
    private ScheduledExecutorService checker;
    private int timeoutInSeconds;

    private Map<Task, Long> pendingTasks = new LinkedHashMap<>();
    private Map<String, Long> totalTaskDistributeCounts = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, Long> pendingTaskDistributeCounts = Collections.synchronizedMap(new LinkedHashMap<>());
    private volatile AtomicLong totalTaskInputCount = new AtomicLong();
    private volatile AtomicLong totalTaskOutputCount = new AtomicLong();
    private volatile AtomicBoolean isSuspend = new AtomicBoolean(false);
    private long firstVisitTimeMillis;

    private RemoteScheduler(String host, int port, String virtualHost, String username, String password,
                            int qos, int timeoutInSeconds, String... targetQueue) {
        this.tasks = new LinkedHashMap<>();
        this.checker = new ScheduledThreadPoolExecutor(qos + 1);
        this.timeoutInSeconds = timeoutInSeconds;
        try {
            this.factory = new ConnectionFactory();
            this.factory.setHost(host);
            this.factory.setPort(port);
            this.factory.setVirtualHost(virtualHost);
            this.factory.setUsername(username);
            this.factory.setPassword(password);
            this.init(factory);
            this.declareBase();
            this.targetQueueNames = targetQueue.length != 0 ? targetQueue : new String[]{DEFAULT};
            this.declareTargets(targetQueueNames);
            this.setQos(qos);
            this.declareConsumers();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static RemoteScheduler getDefault() {
        return new RemoteScheduler("localhost", -1, "/", "guest", "guest", 1, 10);
    }

    public static Builder custom() {
        return new Builder();
    }

    private void init(ConnectionFactory factory) throws IOException, TimeoutException {
        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    private void declareBase() throws IOException {
        channel.exchangeDeclare(EXCHANGE, "direct"); // task queue exchanger
        channel.queueDeclare(NEW_TASK_QUEUE, true, false, false, null); // feedback queue
        channel.queueDeclare(NEW_LAZY_TASK_QUEUE, true, false, false, null); // feedback queue
    }

    // special queues, like the proxy_task_queue
    private void declareTargets(String... queueNames) throws IOException {
        for (String queueName : queueNames) {
            channel.queueDeclare(queueName, true, false, false, null);
        }
    }

    private void setQos(int qos) throws IOException {
        channel.basicQos(qos);
    }

    private void declareConsumers() throws IOException {
        for (String queueName : targetQueueNames) {
            channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(
                        String consumerTag,
                        Envelope envelope,
                        AMQP.BasicProperties properties,
                        byte[] body) throws IOException {
                    if (isSuspend.get()) {
                        channel.basicNack(envelope.getDeliveryTag(), false, true);
                    }
                    Task task = (Task) SerializationUtils.deserialize(body);
                    tasks.put(task, envelope.getDeliveryTag());
                    checker.schedule(() -> {
                        Task task0 = task;
                        synchronized (tasks) {
                            synchronized (pendingTasks) {
                                try {
                                    if (tasks.containsKey(task0)) {
                                        channel.basicNack(tasks.remove(task0), false, true);
                                        logger.warn("Task {} pending timeout (not taken), re-add to default_task_queue.", task0);
                                    } else {
                                        if (pendingTasks.containsKey(task0)) {
                                            channel.basicNack(pendingTasks.remove(task0), false, true);
                                            logger.warn("Task {} pending timeout (no feedback), re-add to default_task_queue.", task0);
                                        } else {
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }, timeoutInSeconds, TimeUnit.SECONDS);
                }
            });
        }
    }

    public String getIdentifier() {
        return "rabbit";
    }

    @Override
    public void shutdownProvider(String originator) {
        throw new RuntimeException("unsupported method (rabbit)");
    }

    @Override
    public Task getTask(String fetcherId) {
        if (isSuspend.get()) {
            return null;
        }
        if (firstVisitTimeMillis == 0) {
            firstVisitTimeMillis = System.currentTimeMillis();
        }
        if (tasks.size() > 0) {
            synchronized (tasks) {
                synchronized (pendingTasks) {
                    Task task = tasks.keySet().iterator().next();
                    pendingTasks.put(task, tasks.remove(task));
                    synchronized (totalTaskDistributeCounts) {
                        totalTaskDistributeCounts.put(fetcherId, totalTaskDistributeCounts.getOrDefault(fetcherId, 0L) + 1);
                    }
                    synchronized (pendingTaskDistributeCounts) {
                        pendingTaskDistributeCounts.put(fetcherId, pendingTaskDistributeCounts.getOrDefault(fetcherId, 0L) + 1);
                    }
                    totalTaskOutputCount.incrementAndGet();
                    return task;
                }
            }
        }
        return null;
    }

    @Override
    public void addTask(String executorId, Task task) {
        if (isSuspend.get()) {
            return;
        }
        try {
            channel.basicPublish("", NEW_TASK_QUEUE, MessageProperties.PERSISTENT_BASIC, SerializationUtils.serialize(task));
            totalTaskInputCount.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addLazyTask(String executorId, Task task) {
        if (isSuspend.get()) {
            return;
        }
        try {
            channel.basicPublish("", NEW_LAZY_TASK_QUEUE, MessageProperties.PERSISTENT_BASIC, SerializationUtils.serialize(task));
            totalTaskInputCount.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long getTotalTaskInputCount() {
        return totalTaskInputCount.get();
    }

    @Override
    public long getTotalTaskOutputCount() {
        return totalTaskOutputCount.get();
    }

    @Override
    public long getRecodedTaskSize() {
        return -1;
    }

    @Override
    public long getCurrentTaskQueueSize() {
        return -1;
    }

    @Override
    public long getCurrentLazyTaskQueueSize() {
        return -1;
    }

    @Override
    public long getRunningTimeMillis() {
        return System.currentTimeMillis() - firstVisitTimeMillis;
    }

    @Override
    public void feedback(String fetcherId, Task task) {
        try {
            Long deliveryTag = pendingTasks.remove(task);
            if (deliveryTag == null) {
                logger.warn("Task {} has been discarded, possibly due to timeout.", task);
                return;
            }
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            e.printStackTrace();
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

    @Deprecated
    @Override
    public void pause(String dir) throws Throwable {
        this.stop();
    }

    @Deprecated
    @Override
    public void resume(String dir) throws Throwable {
        this.init(factory);
    }

    public static class Builder {
        private String host = "localhost";
        private int port = -1;
        private String virtualHost = "/";
        private String username = "guest";
        private String password = "guest";
        private int qos = 1;
        private int timeoutInSeconds = 10;
        private String[] targetQueue = {};

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setVirtualHost(String virtualHost) {
            this.virtualHost = virtualHost;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setQos(int qos) {
            this.qos = qos;
            return this;
        }

        public Builder setTimeoutInSeconds(int timeoutInSeconds) {
            this.timeoutInSeconds = timeoutInSeconds;
            return this;
        }

        public Builder setTargetQueue(String... targetQueue) {
            this.targetQueue = targetQueue;
            return this;
        }

        public RemoteScheduler build() {
            return new RemoteScheduler(host, port, virtualHost, username, password, qos, timeoutInSeconds, targetQueue);
        }
    }
}
