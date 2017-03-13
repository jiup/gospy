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

package cc.gospy.core.pipeline;

import com.google.common.collect.ArrayListMultimap;

import java.util.Collection;
import java.util.List;

public class Pipelines {
    public static HierarchicalFilePipeline HierarchicalFilePipeline;

    private ArrayListMultimap<Class<?>, Pipeline> pipelines = ArrayListMultimap.create();

    public void register(Pipeline pipeline) {
        pipelines.put(pipeline.getAcceptedType(), pipeline);
    }

    public List<Pipeline> get(Class<?> resultType) throws PipelineNotFoundException {
        return pipelines.get(resultType);
    }

    public Collection<Pipeline> getAll() {
        return pipelines.values();
    }
}
