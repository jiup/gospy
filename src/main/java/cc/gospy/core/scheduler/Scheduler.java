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

package cc.gospy.core.scheduler;

import cc.gospy.core.scheduler.filter.DuplicateRemover;
import cc.gospy.core.scheduler.filter.TaskFilter;
import cc.gospy.core.scheduler.queue.LazyTaskQueue;
import cc.gospy.core.scheduler.queue.TaskQueue;

public abstract class Scheduler {
    private TaskQueue taskQueue;
    private LazyTaskQueue lazyTaskQueue;
    private DuplicateRemover duplicateRemover;
    private TaskFilter taskFilter;
}
