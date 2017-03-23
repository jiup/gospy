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

import cc.gospy.core.TaskFilter;
import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.UserAgent;
import cc.gospy.core.processor.Extractor;
import cc.gospy.core.processor.ProcessException;
import cc.gospy.core.processor.Processor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * Note:
 * <p>
 * This is a phantomJs implementation of a processor, this is out of the frameworks's
 * origin design because of its self independent on browser actions instead of pure
 * link visiting by our fetchers. Therefore, we offer a SkipHttpFetcher which should
 * be declared when using a PhantomJsProcessor, by using of the SkipHttpFetcher, pages
 * will not be downloaded during the fetching process. Hence the page will be ignored
 * , so that you can deal with that by using the given api from web driver. (the web
 * driver cannot split into two sections like a fetcher and a processor, only in this
 * way can we expose a optional object - driver for users, if you have better solutions
 * , please send pull requests on github <a>https://github.com/zhangjiupeng/gospy/</a>)
 * </p><p>
 * Furthermore, we do not expect you to relying on our scheduler since several page
 * visiting might based on keyboard/mouse events which is only supported by a web driver,
 * We recommend you to manage yourself on a single ajax load page (with some event binding)
 * as a single procedure in <code>process()</code>.</p>
 */
public class PhantomJSProcessor implements Processor {
    private WebDriver webDriver;
    private Extractor<WebDriver, ?> handler;
    private TaskFilter filter;

    private PhantomJSProcessor(String phantomJsBinaryPath, int timeout, boolean loadImages, String userAgent, Extractor<WebDriver, ?> handler, TaskFilter filter) {
        System.setProperty("phantomjs.binary.path", phantomJsBinaryPath);
        DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();
        capabilities.setCapability("phantomjs.page.settings.resourceTimeout", timeout);
        capabilities.setCapability("phantomjs.page.settings.loadImages", loadImages);
        capabilities.setCapability("phantomjs.page.settings.userAgent", userAgent);
        this.webDriver = new PhantomJSDriver(capabilities);
        this.handler = handler;
        this.filter = filter;
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder {
        private String phantomJsBinaryPath = "/path/to/phantomjs";
        private int timeout = 3000;
        private boolean loadImages = false;
        private String userAgent = UserAgent.Default;
        private Extractor<WebDriver, ?> handler;
        private TaskFilter filter = TaskFilter.HTTP_DEFAULT;

        public <T> Builder setWebDriverExecutor(Extractor<WebDriver, T> executor) {
            this.handler = executor;
            return this;
        }

        public Builder setPhantomJsBinaryPath(String path) {
            this.phantomJsBinaryPath = path;
            return this;
        }

        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setLoadImages(boolean loadImages) {
            this.loadImages = loadImages;
            return this;
        }

        public Builder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder setTaskFilter(TaskFilter filter) {
            this.filter = filter;
            return this;
        }

        public PhantomJSProcessor build() {
            if (handler == null) {
                throw new RuntimeException("WebDriverExecutor not specified, please check you code.");
            }
            return new PhantomJSProcessor(
                    phantomJsBinaryPath,
                    timeout,
                    loadImages,
                    userAgent,
                    handler,
                    filter);
        }

    }

    @Override
    public <T> Result<T> process(Task task, Page page) throws ProcessException {
        try {
            webDriver.get(task.getUrl());
            Result result = handler.handle(page, webDriver);
            if (result != null) {
                if (result.getNewTasks() != null) {
                    result.getNewTasks().removeIf(filter.negate());
                }
                if (result.getPage() == null) {
                    result.setPage(page);
                }
            }
            return result;
        } catch (Throwable throwable) {
            throw new ProcessException(throwable.getMessage(), throwable);
        }
    }

    @Override
    public String[] getAcceptedContentType() {
        return new String[]{"phantomjs"};
    }
}
