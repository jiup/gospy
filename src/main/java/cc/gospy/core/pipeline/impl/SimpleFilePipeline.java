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

import cc.gospy.core.pipeline.PipeException;
import cc.gospy.core.pipeline.Pipeline;
import cc.gospy.entity.Page;
import cc.gospy.entity.Result;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class SimpleFilePipeline implements Pipeline {

    public static SimpleFilePipeline getDefault() {
        return new SimpleFilePipeline();
    }

    @Override
    public void pipe(Page page, Result result) throws PipeException {
        String savePath;
        if (page.getExtra() != null && page.getExtra().get("savePath") != null) {
            savePath = page.getExtra().get("savePath").toString();
        } else {
            throw new PipeException("parameter [savePath] cannot found in page.extra, please check your code.");
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
