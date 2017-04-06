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
public class TransparentFetcher implements Fetcher {
    private boolean convertHttpToPhantomJs;
    private boolean convertHttpToSelenium;

    private TransparentFetcher() {
    }

    public static TransparentFetcher getDefault() {
        return new TransparentFetcher();
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder {
        private boolean convertHttpToPhantomJs;
        private boolean convertHttpToSelenium;

        public Builder convertHttpTaskToPhantomJs() {
            convertHttpToPhantomJs = true;
            convertHttpToSelenium = false;
            return this;
        }

        public Builder convertHttpTaskToSelenium() {
            convertHttpToPhantomJs = false;
            convertHttpToSelenium = true;
            return this;
        }

        public TransparentFetcher build() {
            TransparentFetcher fetcher = new TransparentFetcher();
            fetcher.convertHttpToPhantomJs = convertHttpToPhantomJs;
            fetcher.convertHttpToSelenium = convertHttpToSelenium;
            return fetcher;
        }

    }

    @Override
    public Page fetch(Task task) throws FetchException {
        switch (task.getProtocol()) {
            case "http":
            case "https":
                if (convertHttpToPhantomJs) {
                    task.setUrl("phantomjs://".concat(task.getUrl()));
                } else if (convertHttpToSelenium) {
                    task.setUrl("selenium://".concat(task.getUrl()));
                }
            default:
                Page page = new Page();
                task.addVisitCount();
                page.setTask(task);
                page.setContent(new byte[0]);
                page.setContentType(task.getProtocol());
                return page;
        }
    }

    @Override
    public String[] getAcceptedProtocols() {
        if (convertHttpToPhantomJs || convertHttpToSelenium) {
            return new String[]{"phantomjs", "selenium", "http", "https"};
        }
        return new String[]{"phantomjs", "selenium"};
    }

    @Override
    public String getUserAgent() {
        return null;
    }
}
