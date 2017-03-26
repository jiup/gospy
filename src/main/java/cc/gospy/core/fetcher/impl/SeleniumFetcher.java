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
import cc.gospy.core.fetcher.UserAgent;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

// for ajax rendered pages and test flow visualization
public class SeleniumFetcher implements Fetcher, Closeable {
    public enum Kernel {HtmlUnit, Chrome, Firefox, IE}

    private WebDriver driver;
    private String userAgent;
    private Collection<Cookie> cookies;

    private SeleniumFetcher(Kernel browser, String path, String userAgent, Collection<Cookie> cookies) {
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
        this.userAgent = userAgent;
        this.cookies = cookies;
    }

    public static SeleniumFetcher getDefault() {
        return new Builder().build();
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder {
        private Kernel kernel = Kernel.HtmlUnit;
        private String path = "/path/to/" + kernel.name();
        private String userAgent = UserAgent.Default;
        private Collection<Cookie> cookies = new ArrayList<>();

        public Builder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder setKernel(Kernel kernel, String path) {
            this.kernel = kernel;
            this.path = path;
            return this;
        }

        public Builder addCookie(Cookie cookie) {
            this.cookies.add(cookie);
            return this;
        }

        public SeleniumFetcher build() {
            return new SeleniumFetcher(kernel, path, userAgent, cookies);
        }

    }

    @Override
    public Page fetch(Task task) throws FetchException {
        try {
            task.setUrl(task.getUrl().substring("selenium://".length()));
            Page page = new Page();
            long timer = System.currentTimeMillis();
            if (cookies.size() > 0) {
                cookies.forEach(cookie -> driver.manage().addCookie(cookie));
            }
            driver.get(task.getUrl());
            task.addVisitCount();
            page.setTask(task);
            page.setResponseTime(System.currentTimeMillis() - timer);
            byte[] bytes = driver.getPageSource().getBytes();
            page.setContent(bytes);
            // we cannot get content-type form selenium :(
            // see https://github.com/seleniumhq/selenium-google-code-issue-archive/issues/141#issuecomment-191404952
            // using magic-match is a compromise.
            MagicMatch match = Magic.getMagicMatch(bytes);
            page.setContentType(match.getMimeType());
            return page;
        } catch (Throwable throwable) {
            throw new FetchException(throwable.getMessage(), throwable);
        }
    }

    @Override
    public String[] getAcceptedProtocols() {
        return new String[]{"selenium"};
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public void close() throws IOException {
        driver.close();
        driver.quit();
    }
}
