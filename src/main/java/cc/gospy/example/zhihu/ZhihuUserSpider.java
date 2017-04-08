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

package cc.gospy.example.zhihu;

import cc.gospy.core.Gospy;
import cc.gospy.core.TaskFilter;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import cc.gospy.core.util.Browser;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.*;

public class ZhihuUserSpider {
    public static void main(String[] args) {
        String startUrl = "selenium://https://www.zhihu.com/people/giantchen/following?page=1";
        String chromeDriverPath = "D:/Program Files/Chrome Driver/chromedriver.exe";

        // a demo of using selenium
        Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.custom()
                        .setPendingTimeInSeconds(300)
                        .setAutoExit(false)
                        .build())
                .addFetcher(Fetchers.TransparentFetcher.custom()
                        .convertHttpTaskToSelenium()
                        .build())
                .addProcessor(Processors.SeleniumProcessor.custom()
                        .setDriver(Browser.Chrome, chromeDriverPath)
                        .setWebDriverExecutor((page, driver) -> {
                            Collection<Task> newTasks = new HashSet<>();
                            boolean visited = false;
                            while (!visited) {
                                try {
                                    WebElement nextPageButton = driver.findElement(By.className("PaginationButton-next"));
                                    while (nextPageButton.isDisplayed()) {
                                        nextPageButton.click();
                                        while (true) {
                                            Thread.sleep(1000);
                                            try {
                                                if (driver.findElement(By.className("UserLink-link")).isEnabled()) {
                                                    visited = true;
                                                    break;
                                                }
                                            } catch (Exception e) {
                                            }
                                        }
                                        Thread.sleep(1000);
                                        List<WebElement> elements = driver.findElements(By.xpath("//*[@id='Profile-following']/div[2]/div/div/div/div[2]/h2/div/span/div/div/a"));
                                        elements.forEach(e -> {
                                            Task newTask = new Task("selenium://" + e.getAttribute("href").concat("/following?page=1"));
                                            newTask.setDepth(page.getTask().getDepth() + 1);
                                            System.out.println("find " + e.getText() + "\t" + e.getAttribute("href"));
                                            newTasks.add(newTask);
                                        });
                                    }
                                } catch (Exception e) {
                                }
                            }
                            User user = new User();
                            user.setDistance(page.getTask().getDepth());
                            user.setUsername(driver.findElement(By.className("ProfileHeader-name")).getText());
                            WebElement detailButton;
                            try {
                                detailButton = driver.findElement(By.xpath("//*[@id='ProfileHeader']/div/div[2]/div/div[2]/div[3]/button"));
                                detailButton.click();
                            } catch (NoSuchElementException e) {
                                return new Result<>(newTasks, user);
                            }
                            while (!driver.findElement(By.className("ProfileHeader-detail")).isDisplayed()) ;
                            for (WebElement e : driver.findElements(By.xpath("//*[@id='ProfileHeader']/div/div[2]/div/div[2]/div[2]/span/div/div/span"))) {
                                switch (e.getText().trim()) {
                                    case "居住地":
                                        user.setResidence(e.findElement(By.xpath("./../div/span")).getText());
                                        break;
                                    case "所在行业":
                                        user.setIndustry(e.findElement(By.xpath("./../div")).getText());
                                        break;
                                    case "职业经历":
                                        StringBuffer buffer = new StringBuffer();
                                        e.findElements(By.xpath("./../div/div")).forEach(webElement -> {
                                            buffer.append(webElement.getText()).append("\t");
                                        });
                                        user.setJobs(buffer.toString());
                                        break;
                                    case "教育经历":
                                        buffer = new StringBuffer();
                                        e.findElements(By.xpath("./../div/div")).forEach(webElement -> {
                                            buffer.append(webElement.getText()).append("\t");
                                        });
                                        user.setEducations(buffer.length() > 1 ? buffer.substring(0, buffer.length() - 1) : null);
                                        break;
                                    case "个人简介":
                                        user.setIntro(e.findElement(By.xpath("./../div")).getText().replaceAll("\n", " "));
                                        break;
                                    default:
                                        System.err.println("unknown detail [" + e.getText() + "]");
                                }
                            }
                            return new Result<>(newTasks, user);
                        })
                        .setTaskFilter(TaskFilter.SELENIUM)
                        .build())
                .setExceptionHandler((throwable, task, page) -> {
                    if (throwable.getCause() != null
                            && (throwable.getCause().getClass() == NoSuchWindowException.class
                            || throwable.getCause().getClass() == WebDriverException.class)) {
                        System.out.println("Browser is closed, exit now!");
                        System.exit(0);
                    }
                    throwable.printStackTrace();
                    if (task != null) {
                        if (!task.getUrl().startsWith("selenium://")) {
                            task.setUrl("selenium://".concat(task.getUrl()));
                            System.err.println("Retry " + task);
                            return Arrays.asList(task);
                        }
                    }
                    return null;
                })
                .addPipeline(Pipelines.ConsolePipeline.getDefault())
                .build().addTask(new Task(startUrl)).start();
    }
}
