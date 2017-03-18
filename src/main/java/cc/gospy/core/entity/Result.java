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

package cc.gospy.core.entity;

import java.io.Serializable;
import java.util.Collection;

public class Result<T> implements Serializable {
    private Collection<Task> newTasks;
    private T data;
    private Page page;

    public Result() {

    }

    public Result(Collection<Task> newTasks) {
        this.newTasks = newTasks;
    }

    public Result(Collection<Task> newTasks, T data) {
        this.newTasks = newTasks;
        this.data = data;
    }

    public Class getType() {
        return data != null ? data.getClass() : null;
    }

    public Collection<Task> getNewTasks() {
        return newTasks;
    }

    public void setNewTasks(Collection<Task> newTasks) {
        this.newTasks = newTasks;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }
}
