<p align="center">
  <a href="http://www.gospy.cc/">
     <img src="http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/banner" width="180"><hr>
  </a>
</p>

[![Join Chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Gospy-Dev/Lobby?utm_source=share-link&utm_medium=link&utm_campaign=share-link)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cc.gospy/gospy-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cc.gospy/gospy-core)
[![GitHub release](https://img.shields.io/github/release/zhangjiupeng/gospy.svg)](https://github.com/zhangjiupeng/gospy/releases)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

Gospy is a flexible web crawler framework that allows you to develop complete web crawlers in few minutes.

It's minimalist and unified API can greatly reduce the learning costs of new users. With it, you can better focus on the data itself, rather than implement a complicated web crawler from the beginning. If you are familiar with java and hoping to grab some interesting data, just hold on, you will soon carry out your first crawler in few minutes. Ok, let's start!

## Features

* [x] Portable, Flexible and Modular (you can only use one of the modules, or add your own development module into your Gospy-based crawler)
* [x] Can operate in stand-alone mode (with multi-thread) or distribute mode (RabbitMQ or Hprose) or even the both
* [x] Built in `PhantomJs` and `Selenium`, you can directly call the `WebDriver` to build a browser-kernel based web crawler
* [x] Element extraction based on RegEx, [XPath](https://github.com/code4craft/xsoup/) and [Jsoup](https://jsoup.org/), respectively, apply from simple to complex tasks.
* [x] Support object-oriented processing with annotations
* [x] Practical structural abstraction, from task scheduling to data persistence
* [x] Provide robots.txt [interpreter](https://github.com/BrandwatchLtd/robots/) (easy to use if you need)

## Install
Download jar:

Release Version | JDK Version compatibility | Release Date | Links
-- | -- | -- | --
0.2.1-beta | 1.8+ | 07.04.2017 | [download](https://github.com/ZhangJiupeng/Gospy/releases/tag/v0.2.1)
0.2.2-beta | 1.8+ | 05.21.2017 | [download](https://github.com/ZhangJiupeng/Gospy/releases/tag/v0.2.2)

To add a dependency using Maven, use the following:
```
<dependency>
    <groupId>cc.gospy</groupId>
    <artifactId>gospy-core</artifactId>
    <version>0.2.2</version>
</dependency>
```
To add a dependency using Gradle:
```
compile 'cc.gospy.gospy-core:0.2.2'
```

## Learn about Gospy
Module division:

[![http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/single-infra.jpg](http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/single-infra.jpg?imageView/5/w/500)](http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/single-infra.jpg)

Run in cluster by Hprose:

[![http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/cluster-rpc-infra.jpg](http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/cluster-rpc-infra.jpg?imageView/4/w/500)](http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/cluster-rpc-infra.jpg)

Run in cluster under RabbitMQ-Server runtime:

[![http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/cluster-rabbitmq-infra.jpg](http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/cluster-rabbitmq-infra.jpg?imageView/4/w/450)](http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/cluster-rabbitmq-infra.jpg)

## Quick start

Visit and print the webpage:
```java
Gospy.custom()
        .setScheduler(Schedulers.VerifiableScheduler.getDefault())
        .addFetcher(Fetchers.HttpFetcher.getDefault())
        .addProcessor(Processors.UniversalProcessor.getDefault())
        .addPipeline(Pipelines.ConsolePipeline.custom().bytesToString().build())
        .build().addTask("https://github.com/zhangjiupeng/gospy").start();
```

Custom Fetcher, and set multiple pipelines:
```java
String dir = "D:/"; // you need to specify a valid dir on you os
Gospy.custom()
        .setScheduler(Schedulers.VerifiableScheduler.custom()
                .setTaskQueue(new PriorityTaskQueue()) // specify a priority queue
                .build())
        .addFetcher(Fetchers.HttpFetcher.custom()
                .setAutoKeepAlive(false).before(request -> { // custom request
                    request.setHeader("Accept", "text/html,image/webp,*/*;q=0.8");
                    request.setHeader("Accept-Encoding", "gzip, deflate, sdch");
                    request.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
                    request.setHeader("Cache-Control", "max-age=0");
                    request.setHeader("Connection", "keep-alive");
                    request.setHeader("DNT", "1");
                    request.setHeader("Host", request.getURI().getHost());
                    request.setHeader("User-Agent", UserAgent.DEFAULT);
                })
                .build())
        .addProcessor(Processors.UniversalProcessor.getDefault())
        .addPipeline(Pipelines.ConsolePipeline.getDefault()) // add multiple pipelines
        .addPipeline(Pipelines.SimpleFilePipeline.custom().setDir(dir).build())
        .build()
        .addTask("https://zhangjiupeng.com/logo.png")
        .addTask("https://www.baidu.com/img/bd_logo1.png")
        .addTasks(UrlBundle.parse("https://www.baidu.com/s?wd=gospy&pn={0~90~10}"))
        .start();

```

Save page screenshot by PhantomJS:
```java
String phantomJsPath = "/path/to/phantomjs.exe";
String savePath = "D:/capture.png";
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
                    return new Result<>();
                })
                .build())
        .build().addTask("phantomjs://https://www.taobao.com").start();

```

Crawl by annotated class:
```java
@UrlPattern("http://www.baidu.com/.*\\.php") // 匹配该正则的请求会交付给该类进行处理
public static class BaiduHomepageProcessor extends PageProcessor {
    @ExtractBy.XPath("//*[@id='u1']/a/@href") // 根据元素Xpath将内容填充到集合中
    @ExtractBy.XPath("//*[@id='head']/div/div[4]/div/div[2]/div[1]/div/a/@href")
    public Set<String> topBarLinks;

    @ExtractBy.Regex(value = "id=\"su\" value=\"(.*?)\"", group = 1) 
    public String searchBtnValue;

    @Override
    public void process() { // 填充内容后，在这里指定页面的处理过程
        topBarLinks.forEach(System.out::println);
    }

    @Override
    public Collection<Task> getNewTasks() { // 在这里指定下一轮爬取任务
        return Arrays.asList(new Task("https://www.baidu.com/img/bd_logo1.png"));
    }

    @Override
    public Object[] getResultData() { // 在这里指定需要持久化的数据
        return Arrays.asList(allLinks).stream()
                .filter(s -> s.matches("^https?://((?!javascript:|mailto:| ).)*")).toArray();
    }
}
```
```java
Gospy.custom()
        .setScheduler(Schedulers.VerifiableScheduler.getDefault())
        .addFetcher(Fetchers.HttpFetcher.getDefault())
        .addPageProcessor(BaiduHomepageProcessor.class)
        .addProcessor(Processors.UniversalProcessor.getDefault())
        .addPipeline(Pipelines.ConsolePipeline.getDefault())
        .build().addTask("http://www.baidu.com/index.php").start();
```
[more examples](https://github.com/ZhangJiupeng/Gospy/tree/master/src/main/example)

## Troubleshoot

Common questions will be collected and listed here.

## Cooperate & Contact

Welcome to contribute codes to this project, anyone who had significant contributions will be listed here.

If you are interested in this project, please given stars. If you have any possible questions, you can contact us through the following ways:

[create an issue](https://github.com/zhangjiupeng/gospy/issues/new) | [chat on gitter](https://gitter.im/Gospy-Dev/Lobby) | [send an email](mailto:jiupeng.zhang@gmail.com)

## Thanks
* [code4craft](https://github.com/code4craft) / [webmagic](https://github.com/code4craft/webmagic)
* [code4craft](https://github.com/code4craft) / [xsoup](https://github.com/code4craft/xsoup) <[license](https://github.com/code4craft/xsoup/blob/master/LICENSE)>
* [arimus](https://github.com/arimus) / [jmimemagic](https://github.com/arimus/jmimemagic) <[license](https://github.com/arimus/jmimemagic/blob/master/LICENSE)>

## License

Copyright 2017 ZhangJiupeng

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
