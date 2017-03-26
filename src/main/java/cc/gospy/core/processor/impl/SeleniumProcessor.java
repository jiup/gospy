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
import cc.gospy.core.processor.Extractor;
import cc.gospy.core.processor.ProcessException;
import cc.gospy.core.processor.Processor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * Note:
 * <p>
 * This is a selenium implementation of a processor, this is out of the frameworks's
 * origin design because of its self independent on browser actions instead of pure
 * link visiting by our fetchers. Therefore, we offer a TransparentFetcher which should
 * be declared when using a SeleniumProcessor, by using of the TransparentFetcher, pages
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
public class SeleniumProcessor implements Processor, Closeable {
    private static Logger logger = LoggerFactory.getLogger(SeleniumProcessor.class);

    public enum Kernel {HtmlUnit, Chrome, Firefox, IE}


    private WebDriver webDriver;
    private Extractor<WebDriver, ?> handler;
    private TaskFilter filter;

    private SeleniumProcessor(Kernel browser, String path, Extractor<WebDriver, ?> handler, TaskFilter filter) {
        switch (browser) {
            case HtmlUnit:
                logger.info("Initializing selenium web driver for HtmlUnit...");
                this.webDriver = new HtmlUnitDriver();
                break;
            case Chrome:
                logger.info("Initializing selenium web driver for Chrome...");
                System.setProperty("webdriver.chrome.driver", path);
                this.webDriver = new ChromeDriver();
                break;
            case Firefox:
                logger.info("Initializing selenium web driver for Firefox...");
                System.setProperty("webdriver.firefox.bin", path);
                this.webDriver = new FirefoxDriver();
                break;
            case IE:
                logger.info("Initializing selenium web driver for Internet Explorer...");
                System.setProperty("webdriver.ie.driver", path);
                this.webDriver = new InternetExplorerDriver();
                break;
            default:
                throw new RuntimeException("unsupported browser " + browser);
        }
        this.handler = handler;
        this.filter = filter;
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder {
        private Kernel kernel = Kernel.HtmlUnit;
        private String kernelPath = "/path/to/" + kernel.name();
        private Extractor<WebDriver, ?> handler;
        private TaskFilter filter = TaskFilter.ALLOW_ALL;

        public Builder setKernel(Kernel kernel, String kernelPath) {
            this.kernel = kernel;
            this.kernelPath = kernelPath;
            return this;
        }

        public <T> Builder setWebDriverExecutor(Extractor<WebDriver, T> executor) {
            this.handler = executor;
            return this;
        }

        public Builder setTaskFilter(TaskFilter filter) {
            this.filter = filter;
            return this;
        }

        public SeleniumProcessor build() {
            if (handler == null) {
                throw new RuntimeException("WebDriverExecutor not specified, please check you code.");
            }
            return new SeleniumProcessor(kernel, kernelPath, handler, filter);
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
    public void close() throws IOException {
        logger.info("Closing web driver...");
        webDriver.close();
        webDriver.quit();
        logger.info("Web driver has successfully closed.");
    }

    @Override
    public String[] getAcceptedContentType() {
        return new String[]{"selenium"};
    }
}
