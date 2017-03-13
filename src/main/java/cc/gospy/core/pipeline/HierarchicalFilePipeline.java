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

import cc.gospy.core.Result;
import cc.gospy.core.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

public class HierarchicalFilePipeline implements Pipeline {
    private static Logger logger = LoggerFactory.getLogger(HierarchicalFilePipeline.class);

    private String basePath = "/";
    private File baseDir;

    private HierarchicalFilePipeline(String basePath) {
        this.basePath = basePath.endsWith("/") ? basePath : basePath.concat("/");
        this.init();
    }

    private void init() {
        baseDir = new File(basePath);
        if (!baseDir.exists()) {
            logger.warn("Base directory ({}) not exists, auto create.", basePath);
            baseDir.mkdirs();
        }
    }

    public static class Builder {
        private String bp;

        public Builder setBasePath(String basePath) {
            bp = basePath;
            return this;
        }

        public HierarchicalFilePipeline build() {
            return new HierarchicalFilePipeline(bp);
        }
    }

    public static Builder custom() {
        return new Builder();
    }

    @Override
    public void pipe(Task task, Result result) {
        if (!baseDir.exists()) {
            return;
        }
        String[] strings = (String[]) result.getData();
//        System.out.println(Arrays.toString(strings));
    }

    @Override
    public Class getAcceptedType() {
        return String[].class;
    }
}
