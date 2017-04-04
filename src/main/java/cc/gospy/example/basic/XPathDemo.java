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
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.processor.Processors;

public class XPathDemo {
    public static void main(String[] args) {
        Gospy.custom()
                .addFetcher(Fetchers.HttpFetcher.getDefault())
                .addProcessor(Processors.XPathProcessor.custom()
                        .extract("//*[@id='u1']/a/text()", (task, resultList) -> {
                            System.out.println("Links text:");
                            resultList.forEach(System.out::println);
                            System.out.println();
                            return null;
                        }).extract("//*[@id='u1]/a/@href", (task, resultList) -> {
                            System.out.println("Links target:");
                            resultList.forEach(System.out::println);
                            System.out.println();
                            return null;
                        }).build())
                .build().addTask("http://www.baidu.com/index.php").start();
    }
}
