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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class SimpleFilePipeline implements Pipeline {
    private String dir;

    private SimpleFilePipeline(String dir) {
        this.dir = dir;
    }

    public static SimpleFilePipeline getDefault() {
        return new Builder().build();
    }

    public static Builder custom() {return new Builder();}

    public static class Builder {
        private String dir;

        public Builder setDir(String path) {
            this.dir = path;
            return this;
        }

        public SimpleFilePipeline build() {
            return new SimpleFilePipeline(dir);
        }
    }

    @Override
    public void pipe(Result result) throws PipeException {
        String savePath;
        Page page = result.getPage();
        if (dir == null) {
            if (page.getExtra() != null && page.getExtra().get("savePath") != null) {
                savePath = page.getExtra().get("savePath").toString();
            } else {
                throw new PipeException("runtime config: parameter [savePath] cannot found in page.extra, please check your code.");
            }
        } else {
            savePath = dir + StringHelper.toEscapedFileName(page.getTask().getUrl());
        }
        File file = new File(savePath);
        try {
            file.createNewFile();
            FileChannel channel = new FileOutputStream(file).getChannel();
            channel.write(ByteBuffer.wrap((byte[]) result.getData()));
            channel.close();
        } catch (Throwable throwable) {
            throw new PipeException(throwable.getMessage(), throwable);
        }
    }

    @Override
    public Class getAcceptedType() {
        return byte[].class;
    }
}
