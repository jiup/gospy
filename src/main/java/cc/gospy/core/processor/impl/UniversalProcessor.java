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

import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.processor.Extractor;
import cc.gospy.core.processor.ProcessException;
import cc.gospy.core.processor.Processor;

public class UniversalProcessor implements Processor {
    private Extractor<byte[], ?> handler;

    private UniversalProcessor(Extractor<byte[], ?> handler) {
        this.handler = handler;
    }

    public static Builder custom() {
        return new Builder();
    }

    public static UniversalProcessor getDefault() {
        return new Builder().build();
    }

    @Override
    public <T> Result<T> process(Task task, Page page) throws ProcessException {
        try {
            Result result = handler.handle(page, page.getContent());
            if (result.getPage() == null) {
                result.setPage(page);
            }
            return result;
        } catch (Throwable throwable) {
            throw new ProcessException(throwable.getMessage(), throwable);
        }
    }

    @Override
    public String[] getAcceptedContentType() {
        return new String[]{"*/*"};
    }

    public static class Builder {
        private Extractor<byte[], ?> de;

        public <T> Builder setExtractor(Extractor<byte[], T> handler) {
            de = handler;
            return this;
        }

        public UniversalProcessor build() {
            return new UniversalProcessor(de != null ? de :
                    (Extractor<byte[], byte[]>) (page, document) -> new Result<>(null, document));
        }

    }
}
