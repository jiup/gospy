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

package cc.gospy.core.remote.hprose;

import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Result;
import cc.gospy.core.entity.Task;
import cc.gospy.core.processor.ProcessException;
import cc.gospy.core.processor.Processor;
import cc.gospy.core.remote.RemoteComponent;
import hprose.client.HproseClient;
import hprose.io.HproseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class RemoteProcessor implements Processor, RemoteComponent, Closeable {
    private static Logger logger = LoggerFactory.getLogger(RemoteProcessor.class);

    private HproseClient client;
    private Processor processor;
    private String identifier;
    private String[] acceptedContentType;

    private RemoteProcessor(String[] uriList) {
        this.init(uriList);
    }

    public static Builder custom() {
        return new Builder();
    }

    private void init(String[] uriList) {
        try {
            logger.info("Connecting to remote processor...");
            this.client = HproseClient.create(uriList, HproseMode.MemberMode);
            this.processor = client.useService(Processor.class);
            this.identifier = String.valueOf(client.invoke("getIdentifier"));
            this.acceptedContentType = processor.getAcceptedContentTypes();
            client.setIdempotent(true);
            client.setRetry(2);
            logger.info("Remote processor [{}] initialized.", identifier);
        } catch (Throwable throwable) {
            logger.error("Remote processor initialization failed ({})", throwable.getMessage());
            this.client.close();
            throwable.printStackTrace();
            throw new RuntimeException(throwable.getMessage());
        }
    }

    @Override
    public <T> Result<T> process(Task task, Page page) throws ProcessException {
        Result<T> result = null;
        if (task != null && page != null) {
            result = processor.process(task, page);
        }
        return result;
    }

    @Override
    public String[] getAcceptedContentTypes() {
        return acceptedContentType;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void shutdownProvider(String originator) {
        try {
            client.invoke("shutdownProvider", new Object[]{originator});
            client.close();
            logger.info("Remote processor [{}] terminated.", identifier);
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

        public RemoteProcessor build() throws Throwable {
            if (uri == null) {
                throw new RuntimeException("Uri list (for remote processor) not specified, please check your code.");
            }
            return new RemoteProcessor(uri);
        }
    }
}
