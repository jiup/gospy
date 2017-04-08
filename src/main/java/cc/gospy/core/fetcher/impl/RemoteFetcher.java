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

import cc.gospy.core.remote.rpc.RemoteComponent;
import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.FetchException;
import cc.gospy.core.fetcher.Fetcher;
import hprose.client.HproseClient;
import hprose.io.HproseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class RemoteFetcher implements Fetcher, RemoteComponent, Closeable {
    private static Logger logger = LoggerFactory.getLogger(RemoteFetcher.class);

    private HproseClient client;
    private Fetcher fetcher;
    private String identifier;
    private String[] acceptedProtocols;

    private RemoteFetcher(String[] uriList) throws Throwable {
        this.init(uriList);
    }

    public static Builder custom() {
        return new Builder();
    }

    private void init(String[] uriList) {
        try {
            logger.info("Connecting to remote fetcher...");
            this.client = HproseClient.create(uriList, HproseMode.MemberMode);
            this.fetcher = client.useService(Fetcher.class);
            this.identifier = String.valueOf(client.invoke("getIdentifier"));
            this.acceptedProtocols = fetcher.getAcceptedProtocols();
            client.setIdempotent(true);
            client.setRetry(2);
            logger.info("Remote fetcher [{}] initialized.", identifier);
        } catch (Throwable throwable) {
            logger.error("Remote fetcher initialization failed ({})", throwable.getMessage());
            this.client.close();
            throwable.printStackTrace();
            throw new RuntimeException(throwable.getMessage());
        }
    }

    @Override
    public Page fetch(Task task) throws FetchException {
        Page page = null;
        if (task != null) {
            page = fetcher.fetch(task);
        }
        return page;
    }

    @Override
    public String[] getAcceptedProtocols() {
        return acceptedProtocols;
    }

    @Override
    public String getUserAgent() {
        return fetcher.getUserAgent();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void quit(String originator) {
        try {
            client.invoke("quit", new Object[]{originator});
            client.close();
            logger.info("Remote fetcher [{}] terminated.", identifier);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        try {
            client.invoke("close");
        } catch (Throwable throwable) {
//            throwable.printStackTrace();
        }
    }

    public static class Builder {
        private String[] uri;

        public Builder setUri(String... uri) {
            this.uri = uri;
            return this;
        }

        public RemoteFetcher build() throws Throwable {
            if (uri == null) {
                throw new RuntimeException("Uri list (for remote fetcher) not specified, please check your code.");
            }
            return new RemoteFetcher(uri);
        }
    }
}
