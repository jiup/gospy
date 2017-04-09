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

import com.google.common.collect.Lists;

import java.net.InetSocketAddress;
import java.util.LinkedList;

public class HttpProxy implements Comparable<HttpProxy> {
    public static int historyDepth = 5;
    private static int maxResponseTime;
    private static int maxVisitCount;

    InetSocketAddress address;
    LinkedList<Boolean> history;
    int averageResponseTime;
    int visitCount;

    public static HttpProxy create(InetSocketAddress address, int responseTime) {
        return new HttpProxy(address, responseTime);
    }

    private HttpProxy(InetSocketAddress address, int responseTime) {
        this.address = address;
        this.averageResponseTime = responseTime;
        this.visitCount = 1;
        this.history = Lists.newLinkedList();
        for (int i = 0; i < historyDepth; i++) {
            history.addFirst(true);
        }
    }

    public void recordFailure() {
        record(false, -1);
    }

    public void recordSuccess(int responseTime) {
        record(true, responseTime);
    }

    public void record(boolean successVisited, int responseTime) {
        history.addFirst(successVisited);
        history.removeLast();
        if (successVisited) {
            averageResponseTime = (averageResponseTime * visitCount + responseTime) / (visitCount + 1);
            if (averageResponseTime > maxResponseTime) {
                maxResponseTime = averageResponseTime;
            }
        }
        visitCount++;
        if (visitCount > maxVisitCount) {
            maxVisitCount = visitCount;
        }
    }

    public double getSuccessRate() {
        return history.stream().filter(Boolean::booleanValue).count() * 1d / historyDepth;
    }

    public double getScore() {
        double index_0 = .5 * getSuccessRate();
        double index_1 = .3 * (maxResponseTime != 0 ? (1 - averageResponseTime / maxResponseTime) : 1);
        double index_2 = .2 * (maxVisitCount != 0 ? (1 - visitCount / maxVisitCount) : 1);
        return index_0 + index_1 + index_2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpProxy proxy = (HttpProxy) o;

        return address != null ? address.equals(proxy.address) : proxy.address == null;
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }

    @Override
    public int compareTo(HttpProxy o) {
        double differ = this.getScore() - o.getScore();
        if (Math.abs(differ) < .15) {
            return this.visitCount < o.visitCount ? -1 : 1;
        }
        return differ > 0 ? -1 : 1;
    }

    @Override
    public String toString() {
        return "HttpProxy {" + address +
                ", rate=" + getSuccessRate() +
                ", score=" + getScore() +
                ", visitCount=" + visitCount +
                '}';
    }

}
