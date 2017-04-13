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

public class TaskQueue {
    public final static String EXCHANGE = "task_partition";
    public static final String NEW_TASK_QUEUE = "new_task_queue";
    public static final String NEW_LAZY_TASK_QUEUE = "new_lazy_task_queue";
    public static final String DEFAULT = "default_task_queue";
    public static final String PROXT = "proxy_task_queue";
    public static final String EMERGENCY = "emergency_task_queue";
    public static final String HIGH_PRIORITY = "high_priority_task_queue";
    public static final String MEDIUM_PRIORITY = "medium_priority_task_queue";
    public static final String LOW_PRIORITY = "low_priority_proxy_task_queue";
}
