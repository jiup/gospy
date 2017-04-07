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

package cc.gospy.core.pipeline.impl;

import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Result;
import cc.gospy.core.pipeline.PipeException;
import cc.gospy.core.pipeline.Pipeline;
import cc.gospy.core.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class HierarchicalFilePipeline implements Pipeline {
    private static final Logger logger = LoggerFactory.getLogger(HierarchicalFilePipeline.class);

    private String basePath = "/";
    private File baseDir;

    private HierarchicalFilePipeline(String basePath) {
        this.basePath = basePath.endsWith("/") ? basePath : basePath.concat("/");
        this.init();
    }

    private void init() {
        baseDir = new File(basePath);
        if (!baseDir.exists()) {
            logger.warn("Base directory ({}) not exists, create new one.", basePath);
            baseDir.mkdirs();
        }
    }

    public static Builder custom() {
        return new Builder();
    }

    @Override
    public void pipe(Result result) throws PipeException {
        if (!baseDir.exists()) {
            throw new PipeException("Base directory not exists");
        }
        Page page = result.getPage();
        String savePath, saveType = "";
        if (page.getExtra() != null && page.getExtra().get("savePath") != null) {
            savePath = page.getExtra().get("savePath").toString();
        } else {
            String url = page.getTask().getUrl();
            String name = StringHelper.toEscapedFileName(url.substring(url.lastIndexOf('/') + 1));
            String dir = StringHelper.cutOffProtocolAndHost(url.substring(0, url.lastIndexOf('/') + 1));
            savePath = dir.concat(StringHelper.toEscapedFileName(name));
            saveType = page.getExtra().get("saveType") == null ? "" : page.getExtra().get("saveType").toString();
        }
        if (!saveType.equals("")) {
            savePath = savePath.endsWith(".".concat(saveType)) ? savePath : savePath.concat(".").concat(saveType);
        }
        savePath = savePath.startsWith("/") ? savePath.substring(1) : savePath;
        if (savePath.indexOf('/') != -1) {
            new File(basePath + savePath.substring(0, savePath.lastIndexOf('/') + 1)).mkdirs();
        }
        File file = new File(basePath + savePath);
        try {
            file.createNewFile();
            FileChannel channel = new FileOutputStream(file).getChannel();
            channel.write(ByteBuffer.wrap((byte[]) result.getData()));
            channel.close();
        } catch (IOException e) {
            throw new PipeException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public Class getAcceptedDataType() {
        return byte[].class;
    }

    public static class Builder {
        private String bp;

        public Builder setBasePath(String basePath) {
            bp = basePath;
            return this;
        }

        public HierarchicalFilePipeline build() {
            return new HierarchicalFilePipeline(bp);
        }
    }
}
