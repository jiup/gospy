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

It's minimalist and unified API design can greatly reduce the learning costs of new users. With it, you can better focus on the data itself, rather than design the complicated web crawler from the beginning. If you are familiar with java and hoping to crawl some interesting data, just hold on, you will soon carry out your first crawler in few minutes. Ok, let's start!

## Features

* [x] Portable, Flexible and Modular (you can only use one of the modules, or add your own development module into your Gospy-based crawler)
* [x] Can operate in stand-alone (with multi-threaded) and distribute environments (RabbitMQ or HProse)
* [x] Built in `PhantomJs` and `Selenium`, you can directly call the `WebDriver` to write a browser-kernel based web crawler
* [x] Element extraction based on RegEx, [XPath](https://github.com/code4craft/xsoup/) and [Jsoup](https://jsoup.org/), respectively, apply from simple to complex tasks.
* [x] Supports annotation-based programming
* [x] Practical structural abstraction, from scheduler to pipeline
* [x] Provide robots.txt interpreter (easy to use if you need)

## Install

Release Version | JDK Version compatibility | Release Date | Links
-- | -- | -- | --
0.2.1-beta | 1.8+ | 07.04.2017 | [download jar](https://github.com/ZhangJiupeng/Gospy/releases/tag/v0.2.1)

To add a dependency using Maven, use the following:
```
<dependency>
    <groupId>cc.gospy</groupId>
    <artifactId>gospy-core</artifactId>
    <version>0.2.1</version>
</dependency>
```
To add a dependency using Gradle:
```
dependencies {
  compile 'cc.gospy.gospy-core:0.2.1'
}
```

## Learn about Gospy
Module division:

![](http://7xp1jv.com1.z0.glb.clouddn.com/gospy/img/single-infra.jpg?imageView/5/w/500)

Run in cluster:

## Quick start

Visit and print the webpage:
```java
    public static void main(String[] args) {
        Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.getDefault())
                .addFetcher(Fetchers.HttpFetcher.getDefault())
                .addProcessor(Processors.UniversalProcessor.getDefault())
                .addPipeline(Pipelines.ConsolePipeline.custom().bytesToString().build())
                .build().addTask("https://github.com/zhangjiupeng/gospy").start();
    }
```

## Troubleshoot

Common questions will be collected and listed here.

## Cooperate & Contact

Welcome to contribute codes to this project, anyone who had significant contributions will be listed here.

If you are interested in this project, please given stars. If you have any possible questions, you can contact us through the following ways:

[create an issue](https://github.com/zhangjiupeng/gospy/issues/new) | [chat on gitter]() | [send an email](mailto:jiupeng.zhang@gmail.com)

## Thanks

## License

Copyright 2017 ZhangJiupeng

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
