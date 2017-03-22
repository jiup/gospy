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

package cc.gospy.core.fetcher.impl;

import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.FetchException;
import cc.gospy.core.fetcher.Fetcher;

/**
 * This Fetcher is only reserve for a 3-party processor,
 * eg: PhantomJSProcessor, SeleniumProcessor, etc.
 */
public class SkipHttpFetcher implements Fetcher {
    @Override
    public Page fetch(Task task) throws FetchException {
        Page page = new Page();
        task.addVisitCount();
        page.setTask(task);
        page.setContent(new byte[0]);
        page.setContentType("skip");
        return page;
    }

    @Override
    public String[] getAcceptedProtocols() {
        return new String[]{"http", "https"};
    }

    @Override
    public String getUserAgent() {
        return null;
    }
}
