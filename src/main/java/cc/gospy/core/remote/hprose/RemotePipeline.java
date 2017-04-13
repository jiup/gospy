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

import cc.gospy.core.entity.Result;
import cc.gospy.core.pipeline.PipeException;
import cc.gospy.core.pipeline.Pipeline;
import cc.gospy.core.remote.RemoteComponent;
import hprose.client.HproseClient;
import hprose.io.HproseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class RemotePipeline implements Pipeline, RemoteComponent, Closeable {
    private static Logger logger = LoggerFactory.getLogger(RemotePipeline.class);

    private HproseClient client;
    private Pipeline pipeline;
    private String identifier;
    private Class acceptedDataType;

    private RemotePipeline(String[] uriList) {
        this.init(uriList);
    }

    public static Builder custom() {
        return new Builder();
    }

    private void init(String[] uriList) {
        try {
            logger.info("Connecting to remote pipeline...");
            this.client = HproseClient.create(uriList, HproseMode.MemberMode);
            this.pipeline = client.useService(Pipeline.class);
            this.identifier = String.valueOf(client.invoke("getIdentifier"));
            this.acceptedDataType = pipeline.getAcceptedDataType();
            client.setIdempotent(true);
            client.setRetry(2);
            logger.info("Remote pipeline [{}] initialized.", identifier);
        } catch (Throwable throwable) {
            logger.error("Remote pipeline initialization failed ({})", throwable.getMessage());
            this.client.close();
            throwable.printStackTrace();
            throw new RuntimeException(throwable.getMessage());
        }
    }

    @Override
    public void pipe(Result<?> result) throws PipeException {
        if (result != null) {
            pipeline.pipe(result);
        }
    }

    @Override
    public Class getAcceptedDataType() {
        return acceptedDataType;
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
            logger.info("Remote pipeline [{}] terminated.", identifier);
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

        public RemotePipeline build() throws Throwable {
            if (uri == null) {
                throw new RuntimeException("Uri list (for remote pipeline) not specified, please check your code.");
            }
            return new RemotePipeline(uri);
        }
    }
}
