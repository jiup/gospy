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

import cc.gospy.core.TaskFilter;
import cc.gospy.core.entity.Task;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import cc.gospy.core.scheduler.queue.impl.TimingLazyTaskQueue;
import cc.gospy.core.scheduler.remover.DuplicateRemover;
import cc.gospy.core.scheduler.remover.impl.HashDuplicateRemover;
import com.rabbitmq.client.*;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static cc.gospy.core.remote.rabbitmq.TaskQueue.*;

public class RemoteServiceProvider {
    private static final Logger logger = LoggerFactory.getLogger(RemoteServiceProvider.class);

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private String[] specialQueues;
    private TaskDispatcher dispatcher;
    private LazyTaskQueue lazyTaskQueue;
    private DuplicateRemover duplicateRemover;
    private TaskFilter taskFilter;

    private RemoteServiceProvider(DuplicateRemover remover, TaskFilter taskFilter,
                                  TaskDispatcher dispatcher, String host, int port, String virtualHost,
                                  String username, String password, String... specialQueues) {
        this.lazyTaskQueue = new TimingLazyTaskQueue(wakedTask -> {
            try {
                this.publish(wakedTask);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        this.duplicateRemover = remover;
        this.taskFilter = taskFilter;
        this.dispatcher = dispatcher;
        this.factory = new ConnectionFactory();
        this.factory.setHost(host);
        this.factory.setPort(port);
        this.factory.setVirtualHost(virtualHost);
        this.factory.setUsername(username);
        this.factory.setPassword(password);
        this.specialQueues = specialQueues;
    }

    public static RemoteServiceProvider getDefault() {
        return new RemoteServiceProvider(new HashDuplicateRemover(),
                TaskFilter.ALLOW_ALL, task -> DEFAULT, "localhost", -1, "/", "guest", "guest");
    }

    public static Builder custom() {
        return new Builder();
    }

    public void start() {
        try {
            this.init(factory);
            this.declareBase();
            this.declareSpecialQueues(specialQueues);
            this.declareNewTaskConsumer();
            logger.info("Remote provider has successfully started at {}:{}", factory.getHost(), factory.getPort());
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void init(ConnectionFactory factory) throws IOException, TimeoutException {
        connection = factory.newConnection();
        channel = connection.createChannel();
    }

    private void declareBase() throws IOException {
        channel.exchangeDeclare(EXCHANGE, "direct"); // task queue exchanger
        channel.queueDeclare(DEFAULT, true, false, false, null); // default task queue
        channel.queueBind(DEFAULT, EXCHANGE, DEFAULT); // bind queue to exchanger
        channel.queueDeclare(NEW_TASK_QUEUE, true, false, false, null); // feedback queue
        channel.queueDeclare(NEW_LAZY_TASK_QUEUE, true, false, false, null); // feedback queue
    }

    private void declareSpecialQueues(String... specialQueues) throws IOException {
        for (String queueName : specialQueues) {
            channel.queueDeclare(queueName, true, false, false, null);
            channel.queueBind(queueName, EXCHANGE, queueName);
        }
    }

    private void declareNewTaskConsumer() throws IOException {
        channel.basicConsume(NEW_TASK_QUEUE, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                Task newTask = (Task) SerializationUtils.deserialize(body);
                if (newTask.isCheckSkipping() || taskFilter.test(newTask)) {
                    if (duplicateRemover.exists(newTask)) {
                        duplicateRemover.record(newTask);
                    } else {
                        if (newTask.getExpectedVisitInSeconds() == 0) {
                            publish(newTask);
                        } else {
                            lazyTaskQueue.add(newTask);
                        }
                    }
                }
            }
        });
        channel.basicConsume(NEW_LAZY_TASK_QUEUE, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                Task newTask = (Task) SerializationUtils.deserialize(body);
                if (newTask.isCheckSkipping() || taskFilter.test(newTask)) {
                    lazyTaskQueue.add(newTask);
                }
            }
        });
    }

    private void publish(Task task) throws IOException {
        channel.basicPublish(EXCHANGE, dispatcher.dispatch(task), MessageProperties.PERSISTENT_BASIC, SerializationUtils.serialize(task));
        duplicateRemover.record(task);
    }

    public static class Builder {
        private DuplicateRemover remover = new HashDuplicateRemover();
        private TaskFilter filter = TaskFilter.ALLOW_ALL;
        private TaskDispatcher dispatcher = task -> DEFAULT;
        private String host = "localhost";
        private int port = -1;
        private String virtualHost = "/";
        private String username = "guest";
        private String password = "guest";
        private String[] specialQueues = {};

        public Builder setRemover(DuplicateRemover remover) {
            this.remover = remover;
            return this;
        }

        public Builder setTaskFilter(TaskFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder setDispatcher(TaskDispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return this;
        }

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

        public Builder setSpecialQueues(String[] specialQueues) {
            this.specialQueues = specialQueues;
            return this;
        }

        public RemoteServiceProvider build() {
            return new RemoteServiceProvider(remover, filter, dispatcher, host, port, virtualHost, username, password, specialQueues);
        }

    }

}
