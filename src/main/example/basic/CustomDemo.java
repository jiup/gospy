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
import cc.gospy.core.fetcher.UserAgent;
import cc.gospy.core.pipeline.Pipelines;
import cc.gospy.core.processor.Processors;
import cc.gospy.core.scheduler.Schedulers;
import cc.gospy.core.scheduler.queue.impl.PriorityTaskQueue;

public class CustomDemo {
    public static void main(String[] args) {
        String dir = "D:/"; // you need to specify a valid dir on you os
        Gospy.custom()
                .setScheduler(Schedulers.VerifiableScheduler.custom()
                        .setTaskQueue(new PriorityTaskQueue())
                        .build())
                .addFetcher(Fetchers.HttpFetcher.custom()
                        .setAutoKeepAlive(false).before(request -> {
                            request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
                            request.setHeader("Accept-Encoding", "gzip, deflate, sdch");
                            request.setHeader("Accept-Language", "zh-CN,zh;q=0.8");
                            request.setHeader("Cache-Control", "max-age=0");
                            request.setHeader("Connection", "keep-alive");
                            request.setHeader("Cookie", "_zap=03331d32-6eae-46b7-b0ff-fcfe3ba6aa27; d_c0=\"AECCQiqCcAuPTjf86tjmGFNvgXMYz5qcF8U=|1489308875\"; q_c1=50b3e59d5d1a4c109554bb813542bc3b|1489308875000|1489308875000; nweb_qa=heifetz; r_cap_id=\"ZTAwMzI0OGIyN2M4NDFmZTkzYWY4NmJlZTg4YzQ4NGM=|1489564408|976996c981dc98572e08c645841b076ceb67036a\"; cap_id=\"NjhjMmI4NTZjMGYxNGMxZWJmNTk5ZTEyM2QyMDRlNTk=|1489564408|d9130d5194d366d20a0bf7730cd83beaea6c9fa7\"; _xsrf=6c91d57200c3488a59d853841e19b03b; aliyungf_tc=AQAAAMljKH0j4w0AQ9QRPFjQ/zCyLwlA; s-q=new%20age; s-i=1; sid=37e4lsu8; s-t=autocomplete; __utma=51854390.233105827.1490087407.1490355947.1490372530.5; __utmb=51854390.0.10.1490372530; __utmc=51854390; __utmz=51854390.1490344588.3.2.utmcsr=zhihu.com|utmccn=(referral)|utmcmd=referral|utmcct=/question/38963803; __utmv=51854390.100--|2=registration_date=20160715=1^3=entry_date=20160715=1; z_c0=Mi4wQUhCQUxLbFBPd29BUUlKQ0tvSndDeGNBQUFCaEFsVk5fM193V0FCNXdxUDBXbEpwNE91VnpNVmN5cG9jSDN5N2F3|1490372529|55db5ecfe60a1a399d94ce3e055f9490b177f042");
                            request.setHeader("DNT", "1");
                            request.setHeader("Host", request.getURI().getHost());
                            request.setHeader("User-Agent", UserAgent.Chrome_56_0_2924_87_Win_10_64_bit);
                        })
                        .build())
                .addProcessor(Processors.UniversalProcessor.getDefault())
                .addPipeline(Pipelines.ConsolePipeline.getDefault())
                .addPipeline(Pipelines.SimpleFilePipeline.custom().setDir(dir).build())
                .build()
                .addTask("https://zhangjiupeng.com/logo.png")
                .addTask("https://www.baidu.com/img/bd_logo1.png")
                .start();

    }
}
