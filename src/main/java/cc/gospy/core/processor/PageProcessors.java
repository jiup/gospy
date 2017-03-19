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

package cc.gospy.core.processor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PageProcessors {
    private Map<String, Class<? extends PageProcessor>> pageProcessors = new HashMap<>();

    public void register(PageProcessor processor) {
        if (processor == null) {
            throw new RuntimeException("processor not initialized, please check your code.");
        }
        for (String contentType : processor.getAcceptedContentType()) {
            pageProcessors.put(contentType, processor.getClass());
        }
    }

    public Class<? extends PageProcessor> get(String contentType) throws PageProcessorNotFoundException {
        Class<? extends PageProcessor> pageProcessor = pageProcessors.get(contentType);
        if (pageProcessor == null) {
            if (contentType != null && contentType.indexOf('/') != -1) {
                String key = contentType;
                while (pageProcessor == null) {
                    if (key.equals("*/*")) {
                        throw new PageProcessorNotFoundException(contentType);
                    } else if (key.endsWith("/*")) {
                        key = "*/*";
                    } else {
                        key = contentType.substring(0, contentType.indexOf('/')).concat("/*");
                    }
                    pageProcessor = pageProcessors.get(key);
                }
            } else {
                throw new PageProcessorNotFoundException(contentType);
            }
        }
        return pageProcessor;
    }

    public Collection<Class<? extends PageProcessor>> getAll() {
        return pageProcessors.values();
    }
}
