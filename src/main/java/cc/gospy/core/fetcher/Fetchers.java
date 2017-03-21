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

package cc.gospy.core.fetcher;

import cc.gospy.core.fetcher.impl.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Fetchers {
    public static HttpFetcher HttpFetcher;
    public static FileFetcher FileFetcher;
    public static cc.gospy.core.fetcher.impl.PhantomJSFetcher PhantomJSFetcher;
    public static SeleniumFetcher SeleniumFetcher;
    public static SkipHttpFetcher SkipHttpFetcher;

    private Map<String, Fetcher> fetchers = new HashMap<>();

    public void register(Fetcher fetcher) {
        if (fetcher == null) {
            throw new RuntimeException("fetcher not initialized, please check your code.");
        }
        for (String protocol : fetcher.getAcceptedProtocols()) {
            fetchers.put(protocol, fetcher);
        }
    }

    public Fetcher get(String protocol) throws FetcherNotFoundException {
        Fetcher fetcher = fetchers.get(protocol);
        if (fetcher == null) {
            fetcher = fetchers.get("*");
            if (fetcher == null) {
                throw new FetcherNotFoundException(protocol);
            }
        }
        return fetcher;
    }

    public Collection<Fetcher> getAll() {
        return fetchers.values();
    }
}
