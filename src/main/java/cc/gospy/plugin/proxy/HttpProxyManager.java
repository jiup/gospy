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

import cc.gospy.core.util.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

@Experimental
public class HttpProxyManager {
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyManager.class);

    public static int validateTimeout = 3000;
    public static HttpProxyCollector.Strategy strategy = HttpProxyCollector.ProxySource.XiCiGaoNi;

    private PriorityQueue<HttpProxy> proxies;
    private Set<InetSocketAddress> addressSet;
    private int updateThreshold;
    private int poolSize;

    public HttpProxyManager(int poolSize, double outRate) {
        if (poolSize < 1)
            throw new ArithmeticException("invalid poolSize");

        if (outRate >= 1)
            throw new ArithmeticException("outRate larger than one");

        this.poolSize = poolSize;
        this.proxies = new PriorityQueue<>(poolSize);
        this.addressSet = new HashSet<>(poolSize * 2);
        this.updateThreshold = (int) (poolSize * (1 - outRate));
        this.fill(HttpProxyCollector.getProxyIps(strategy, poolSize * 2));
    }

    private void fill(List<InetSocketAddress> addresses) {
        logger.info("Expanding http proxy pool...");
        for (InetSocketAddress address : addresses) {
            if (!addressSet.contains(address)) {
                int result = validateConnection(address);
                if (result != -1) {
                    addressSet.add(address);
                    proxies.add(HttpProxy.create(address, result));
                }
                if (proxies.size() == poolSize) {
                    break;
                }
            }
        }
        logger.info("Http proxy pool filled, ratio={}/{}, threshold={}.", proxies.size(), poolSize, updateThreshold);
    }

    private int validateConnection(InetSocketAddress address) {
        long startTime = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(address, validateTimeout);
        } catch (Throwable throwable) {
            logger.warn("Http proxy [{}] is not available", address);
            return -1;
        }
        return Math.toIntExact(System.currentTimeMillis() - startTime);
    }

    public void updateProxyPool() {
        PriorityQueue<HttpProxy> temp = new PriorityQueue<>(poolSize);
        addressSet.clear();
        while (proxies.size() > updateThreshold) {
            addressSet.add(proxies.peek().address);
            temp.offer(proxies.poll());
        }
        proxies.clear();
        proxies = temp;
        this.fill(HttpProxyCollector.getProxyIps(strategy, (poolSize - temp.size()) * 2));
    }

    public void reset() {
        proxies.clear();
        addressSet.clear();
        logger.info("Http proxy pool cleared!");
        this.proxies = new PriorityQueue<>(poolSize);
        this.addressSet = new HashSet<>(poolSize * 2);
        this.fill(HttpProxyCollector.getProxyIps(strategy, poolSize * 2));
    }

    public HttpProxy getHttpProxy() {
        int result;
        HttpProxy proxy;
        do {
            proxy = proxies.poll();
            result = validateConnection(proxy.address);
            if (result == -1) {
                proxy.recordFailure();
                if (proxy.getSuccessRate() < .2) {
                    logger.warn("Http proxy [{}] expired.", proxy.address);
                    if (proxies.size() < updateThreshold) {
                        this.fill(HttpProxyCollector.getProxyIps(strategy, (poolSize - updateThreshold) * 2));
                    }
                    continue;
                }
            } else {
                proxy.recordSuccess(result);
            }
            proxies.offer(proxy);
        } while (result == -1);
        return proxy;
    }

    public void reportFailure(HttpProxy proxy) {
        if (proxies.contains(proxy)) {
            proxy.recordFailure();
        } else {
            throw new RuntimeException("proxy [" + proxy.address + "] not exists");
        }
    }

}
