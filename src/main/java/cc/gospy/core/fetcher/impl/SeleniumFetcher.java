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
import cc.gospy.core.util.Experimental;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import java.io.Closeable;
import java.io.IOException;

@Experimental
public class SeleniumFetcher implements Fetcher, Closeable {
    public enum Kernel {HtmlUnit, Chrome, Firefox, IE}

    private WebDriver driver;

    private SeleniumFetcher(Kernel browser, String path) {
        switch (browser) {
            case HtmlUnit:
                driver = new HtmlUnitDriver();
                break;
            case Chrome:
                System.setProperty("webdriver.chrome.driver", path);
                driver = new ChromeDriver();
                break;
            case Firefox:
                System.setProperty("webdriver.firefox.bin", path);
                driver = new FirefoxDriver();
                break;
            case IE:
                System.setProperty("webdriver.ie.driver", path);
                driver = new InternetExplorerDriver();
                break;
            default:
                throw new RuntimeException("unsupported browser " + browser);
        }
    }

    public static SeleniumFetcher getDefault() {
        return new SeleniumFetcher(Kernel.HtmlUnit, null);
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder {
        private Kernel kernel = Kernel.HtmlUnit;
        private String path = "/path/to/" + kernel.name();

        public Builder setKernel(Kernel kernel, String path) {
            this.kernel = kernel;
            this.path = path;
            return this;
        }

        public SeleniumFetcher build() {
            return new SeleniumFetcher(kernel, path);
        }

    }

    @Override
    public Page fetch(Task task) throws FetchException {
        try {
            Page page = new Page();
            page.setTask(task);
            driver.get(task.getUrl());
            page.setContent(driver.getPageSource().getBytes());
            return page;
        } catch (Throwable throwable) {
            throw new FetchException(throwable.getMessage(), throwable);
        }
    }

    @Override
    public String[] getAcceptedProtocols() {
        return new String[]{"text/html", "text/xml"};
    }

    @Override
    public void close() throws IOException {
        driver.close();
        driver.quit();
    }
}
