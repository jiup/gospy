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
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

// for ajax rendered pages
public class PhantomJSFetcher implements Fetcher, Closeable {
    private WebDriver webDriver;
    private String userAgent;
    private Collection<Cookie> cookies;

    private PhantomJSFetcher(String phantomJsBinaryPath, int timeout, boolean loadImages, String userAgent, Collection<Cookie> cookies) {
        System.setProperty("phantomjs.binary.path", phantomJsBinaryPath);
        DesiredCapabilities capabilities = DesiredCapabilities.phantomjs();
        capabilities.setCapability("phantomjs.page.settings.resourceTimeout", timeout);
        capabilities.setCapability("phantomjs.page.settings.loadImages", loadImages);
        capabilities.setCapability("phantomjs.page.settings.userAgent", userAgent);
        this.webDriver = new PhantomJSDriver(capabilities);
        this.userAgent = userAgent;
        this.cookies = cookies;
    }

    public static Builder custom() {
        return new Builder();
    }

    @Override
    public Page fetch(Task task) throws FetchException {
        try {
            task.setUrl(task.getUrl().substring("phantomjs://".length()));
            Page page = new Page();
            long timer = System.currentTimeMillis();
            if (cookies.size() > 0) {
                cookies.forEach(cookie -> webDriver.manage().addCookie(cookie));
            }
            webDriver.get(task.getUrl());
            byte[] bytes = webDriver.getPageSource().getBytes();
            page.setResponseTime(System.currentTimeMillis() - timer);
            task.addVisitCount();
            page.setTask(task);
            page.setContent(bytes);
            // webDriver cannot get content-type form selenium :(
            // see https://github.com/seleniumhq/selenium-google-code-issue-archive/issues/141#issuecomment-191404952
            // using magic-match is a compromise, which might cause efficiency problems.
            MagicMatch match = Magic.getMagicMatch(bytes);
            page.setContentType(match.getMimeType());
            return page;
        } catch (Throwable throwable) {
            throw new FetchException(throwable.getMessage(), throwable);
        }
    }

    @Override
    public String[] getAcceptedProtocols() {
        return new String[]{"phantomjs"};
    }

    @Override
    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public void close() throws IOException {
        webDriver.close();
        webDriver.quit();
    }

    public static class Builder {
        private String path = "/path/to/phantomjs";
        private int timeout = 3000;
        private boolean loadImages = false;
        private String userAgent = UserAgent.Default;
        private Collection<Cookie> cookies = new ArrayList<>();

        public Builder setPhantomJsBinaryPath(String phantomJsBinaryPath) {
            path = phantomJsBinaryPath;
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

        public Builder addCookie(Cookie cookie) {
            this.cookies.add(cookie);
            return this;
        }

        public PhantomJSFetcher build() {
            return new PhantomJSFetcher(path, timeout, loadImages, userAgent, cookies);
        }
    }
}
