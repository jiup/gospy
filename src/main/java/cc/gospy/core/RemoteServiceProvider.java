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

package cc.gospy.core;

import cc.gospy.core.util.StringHelper;
import hprose.server.HproseTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public class RemoteServiceProvider {
    private static Logger logger = LoggerFactory.getLogger(RemoteServiceProvider.class);

    private HproseTcpServer server;
    private String identifier;

    private RemoteServiceProvider(String identifier, String uri) {
        this.identifier = identifier;
        try {
            server = new HproseTcpServer(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        server.add("getIdentifier", this);
        server.add("quit", this);
    }

    public static Builder custom() {
        return new Builder();
    }

    public static class Builder {
        private String identifier;
        private String uri;

        public Builder setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder setOpenUri(String uri) {
            this.uri = uri;
            return this;
        }

        public RemoteServiceProvider build() {
            if (identifier == null) {
                identifier = StringHelper.getRandomIdentifier();
            }
            if (uri == null) {
                throw new RuntimeException("Uri not specified, please check your code.");
            }
            return new RemoteServiceProvider(identifier, uri);
        }
    }

    public void start() throws IOException {
        server.start();
        logger.info("Remote provider has successfully started at {}:{}", server.getHost(), server.getPort());
        System.out.println("Registered Methods:");
        server.getGlobalMethods().getAllNames().forEach(e -> System.out.println("\t" + e));
    }

    public String getIdentifier() {
        return identifier;
    }

    public void quit(String originator) {
        server.stop();
        logger.info("Service terminated by [{}].", originator);
        System.exit(0);
    }

    public <T> RemoteServiceProvider addComponent(T t) {
        server.add(t);
        return this;
    }

    public <T> RemoteServiceProvider addComponent(T t, Class<? extends T> type) {
        server.add(t, type);
        return this;
    }


}
