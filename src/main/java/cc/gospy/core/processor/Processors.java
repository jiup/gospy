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

import cc.gospy.core.processor.impl.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Processors {
    public static JsoupProcessor JsoupProcessor;
    public static XPathProcessor XPathProcessor;
    public static RegexProcessor RegexProcessor;
    public static UniversalProcessor UniversalProcessor;
    public static PhantomJSProcessor PhantomJSProcessor;
    public static SeleniumProcessor SeleniumProcessor;

    private Map<String, Processor> processors = new HashMap<>();

    public void register(Processor processor) {
        if (processor == null) {
            throw new RuntimeException("processor not initialized, please check your code.");
        }
        for (String contentType : processor.getAcceptedContentType()) {
            processors.put(contentType, processor);
        }
    }

    public Processor get(String contentType) throws ProcessorNotFoundException {
        Processor processor = processors.get(contentType);
        if (processor == null) {
            if (contentType != null && contentType.indexOf('/') != -1) {
                String key = contentType;
                while (processor == null) {
                    if (key.equals("*/*")) {
                        throw new ProcessorNotFoundException(contentType);
                    } else if (key.endsWith("/*")) {
                        key = "*/*";
                    } else {
                        key = contentType.substring(0, contentType.indexOf('/')).concat("/*");
                    }
                    processor = processors.get(key);
                }
            } else {
                throw new ProcessorNotFoundException(contentType);
            }
        }
        return processor;
    }

    public Collection<Processor> getAll() {
        return processors.values();
    }
}
