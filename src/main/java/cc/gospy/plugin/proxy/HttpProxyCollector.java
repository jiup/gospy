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

package cc.gospy.plugin.proxy;

import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.Fetcher;
import cc.gospy.core.fetcher.Fetchers;
import cc.gospy.core.processor.Processor;
import cc.gospy.core.processor.Processors;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class HttpProxyCollector {

    enum ProxySource implements Strategy {
        XiCiPuTong {
            @Override
            public List<Task> getTasks(int proxyCount) {
                List<Task> tasks = new ArrayList<>();
                int page = (int) Math.ceil(proxyCount / 100f);
                for (int i = 1; i <= page; i++) {
                    tasks.add(new Task("http://www.xicidaili.com/nt/" + i));
                }
                return tasks;
            }

            @Override
            public Processor getProcessor() {
                return Processors.XPathProcessor.custom()
                        .extract("//*[@id='ip_list']/tbody/tr/td[2]/text()", (task, resultList, returnedData) -> {
                            List<String> ips = new ArrayList<>();
                            ips.addAll(resultList);
                            task.getExtra().put("ips", ips);
                            return null;
                        })
                        .extract("//*[@id='ip_list']/tbody/tr/td[3]/text()", (task, ports, returnedData) -> {
                            List<String> ips = (List<String>) task.getExtra().get("ips");
                            List<InetSocketAddress> addresses = new ArrayList<>();
                            for (int i = 0; i < ports.size(); i++) {
                                addresses.add(new InetSocketAddress(ips.get(i), Integer.parseInt(ports.get(i))));
                            }
                            returnedData.addAll(addresses);
                            return null;
                        }).build();
            }
        },
        XiCiGaoNi {
            @Override
            public List<Task> getTasks(int proxyCount) {
                List<Task> tasks = new ArrayList<>();
                int page = (int) Math.ceil(proxyCount / 100f);
                for (int i = 1; i <= page; i++) {
                    tasks.add(new Task("http://www.xicidaili.com/nn/" + i));
                }
                return tasks;
            }

            @Override
            public Processor getProcessor() {
                return ProxySource.XiCiPuTong.getProcessor();
            }
        },
        XiCiHttp {
            @Override
            public List<Task> getTasks(int proxyCount) {
                List<Task> tasks = new ArrayList<>();
                int page = (int) Math.ceil(proxyCount / 100f);
                for (int i = 1; i <= page; i++) {
                    tasks.add(new Task("http://www.xicidaili.com/wt/" + i));
                }
                return tasks;
            }

            @Override
            public Processor getProcessor() {
                return ProxySource.XiCiPuTong.getProcessor();
            }
        },
        XiCiHttps {
            @Override
            public List<Task> getTasks(int proxyCount) {
                List<Task> tasks = new ArrayList<>();
                int page = (int) Math.ceil(proxyCount / 100f);
                for (int i = 1; i <= page; i++) {
                    tasks.add(new Task("http://www.xicidaili.com/wn/" + i));
                }
                return tasks;
            }

            @Override
            public Processor getProcessor() {
                return ProxySource.XiCiPuTong.getProcessor();
            }
        }
    }

    interface Strategy {
        List<Task> getTasks(int proxyCount);

        Processor getProcessor();
    }

    public static List<InetSocketAddress> getProxyIps(Strategy proxySource, int count) {
        if (count <= 0)
            throw new RuntimeException("ip count must be positive");

        List<InetSocketAddress> addresses = new ArrayList<>();
        Fetcher fetcher = Fetchers.HttpFetcher.getDefault();
        Processor processor = proxySource.getProcessor();
        for (Task task : proxySource.getTasks(count)) {
            try {
                Page page = fetcher.fetch(task);
                for (Object address : ((List) processor.process(task, page).getData())) {
                    if (addresses.size() == count) {
                        return addresses;
                    }
                    addresses.add((InetSocketAddress) address);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return addresses;
    }

}
