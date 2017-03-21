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

import cc.gospy.core.entity.Page;
import cc.gospy.core.entity.Task;
import cc.gospy.core.fetcher.FetchException;
import cc.gospy.core.fetcher.Fetcher;

import javax.activation.MimetypesFileTypeMap;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FileFetcher implements Fetcher {
    private MimetypesFileTypeMap mimetypesFileTypeMap;

    private FileFetcher() {
        this.mimetypesFileTypeMap = new MimetypesFileTypeMap();
    }

    public static class Builder {
        public FileFetcher build() {
            return new FileFetcher();
        }
    }

    public static FileFetcher getDefault() {
        return new Builder().build();
    }

    @Override
    public Page fetch(Task task) throws FetchException {
        try {
            String url = task.getUrl().replaceAll("\\\\", "/");
            url = url.startsWith("file://") ? url.substring(7) : url;
            long timer = System.currentTimeMillis();
            byte[] bytes = getBytesFromFile(url);
            timer = System.currentTimeMillis() - timer;
            Page page = new Page();
            page.setTask(task);
            page.setStatusCode(200);
            page.setContent(bytes);
            page.setResponseTime(timer);
            page.setContentType(mimetypesFileTypeMap.getContentType(url));
            return page;
        } catch (Throwable throwable) {
            throw new FetchException(throwable.getMessage(), throwable);
        }
    }

    private byte[] getBytesFromFile(String path) throws Throwable {
        FileChannel channel = new RandomAccessFile(path, "r").getChannel();
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).load();
        byte[] bytes = new byte[(int) channel.size()];
        if (buffer.hasRemaining()) {
            buffer.get(bytes, 0, buffer.remaining());
        }
        channel.close();
        return bytes;
    }

    @Override
    public String[] getAcceptedProtocols() {
        return new String[]{"file"};
    }

    @Override
    public String getUserAgent() {
        return null;
    }
}
