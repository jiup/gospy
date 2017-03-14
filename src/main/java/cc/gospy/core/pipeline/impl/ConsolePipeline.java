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

package cc.gospy.core.pipeline.impl;

import cc.gospy.core.pipeline.PipeException;
import cc.gospy.core.pipeline.Pipeline;
import cc.gospy.core.util.StringHelper;
import cc.gospy.entity.Page;
import cc.gospy.entity.Result;

public class ConsolePipeline implements Pipeline {
    public static ConsolePipeline getDefault() {
        return new ConsolePipeline();
    }

    @Override
    public void pipe(Page page, Result result) throws PipeException {
        Object data = result.getData();
        if (data == null) {
            return;
        }
        if (data.getClass().isArray()) {
            if (data.getClass() == boolean[].class) {
                System.out.println(StringHelper.toString((boolean[]) data));
            } else if (data.getClass() == byte[].class) {
                System.out.println(StringHelper.toString((byte[]) data));
            } else if (data.getClass() == short[].class) {
                System.out.println(StringHelper.toString((short[]) data));
            } else if (data.getClass() == int[].class) {
                System.out.println(StringHelper.toString((int[]) data));
            } else if (data.getClass() == float[].class) {
                System.out.println(StringHelper.toString((float[]) data));
            } else if (data.getClass() == double[].class) {
                System.out.println(StringHelper.toString((double[]) data));
            } else if (data.getClass() == long[].class) {
                System.out.println(StringHelper.toString((long[]) data));
            } else if (data.getClass() == char[].class) {
                System.out.println(StringHelper.toString((char[]) data));
            } else {
                StringBuffer buffer = new StringBuffer();
                Object[] value = (Object[]) data;
                buffer.append(data.getClass().getSimpleName()).append("[").append(value.length).append("]");
                for (int i = 0; i < value.length; i++) {
                    buffer.append("\n").append(value);
                }
                System.out.println(buffer.append("\n").toString());
            }
        } else {
            System.out.println(data);
        }
    }

    @Override
    public Class getAcceptedType() {
        return Object.class;
    }
}
