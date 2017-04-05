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

package cc.gospy.example.basic;

import cc.gospy.core.Gospy;
import cc.gospy.core.entity.Result;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;

public class SeleniumDemo {
    public static void main(String[] args) {
        String phantomJsPath = "D:/Program Files/PhantomJS/phantomjs-2.1.1-windows/bin/phantomjs.exe";
        String savePath = "D:/screenshot.png";
        Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.custom()
                        .setPendingTimeInSeconds(60).build())
                .addFetcher(Fetchers.TransparentFetcher.getDefault())
                .addProcessor(Processors.PhantomJSProcessor.custom()
                        .setPhantomJsBinaryPath(phantomJsPath)
                        .setWebDriverExecutor((page, webDriver) -> {
                            TakesScreenshot screenshot = (TakesScreenshot) webDriver;
                            File src = screenshot.getScreenshotAs(OutputType.FILE);
                            FileUtils.copyFile(src, new File(savePath));
                            System.out.println("screenshot has been saved to " + savePath);
                            return new Result<>();
                        })
                        .build())
                .build().addTask("phantomjs://http://www.zhihu.com").start();
    }
}
