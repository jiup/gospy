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

package cc.gospy.core;

import cc.gospy.core.fetcher.Fetcher;
import cc.gospy.core.processor.Processor;

import java.util.HashMap;
import java.util.Map;

public class Gospy {
    private Scheduler scheduler;
    private Map<String, Fetcher> fetcherFactory;
    private Map<String, Processor> processorFactory;

    private Gospy(Scheduler scheduler
            , Map<String, Fetcher> fetcherFactory
            , Map<String, Processor> processorFactory) {
        this.scheduler = scheduler;
        this.fetcherFactory = fetcherFactory;
        this.processorFactory = processorFactory;
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder {
        private Scheduler sc;
        private Map<String, Fetcher> ff = new HashMap<>();
        private Map<String, Processor> pf = new HashMap<>();

        public void setScheduler(Scheduler scheduler) {
            sc = scheduler;
        }

        public void addFetcher(String protocol, Fetcher fetcher) {
            ff.put(protocol, fetcher);
        }

        public void addProcessor(String contentType, Processor processor) {
            pf.put(contentType, processor);
        }

        public Gospy build() {
            return new Gospy(sc, ff, pf);
        }

    }

}
