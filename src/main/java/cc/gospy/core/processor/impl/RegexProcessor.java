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

package cc.gospy.core.processor.impl;

import cc.gospy.entity.Page;
import cc.gospy.entity.Result;
import cc.gospy.entity.Task;
import cc.gospy.core.processor.ProcessException;
import cc.gospy.core.processor.Processor;

public class RegexProcessor implements Processor {
    @Override
    public <T> Result<T> process(Task task, Page page) throws ProcessException {
        return null;
    }

    @Override
    public String[] getAcceptedContentType() {
        return new String[0];
    }
}
