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

package cc.gospy.core.util.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Cleaner;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class FileMappedQueue<T extends Serializable> extends AbstractQueue<T> {

    private static Logger logger = LoggerFactory.getLogger(FileMappedQueue.class);

    public static final int PAGE_CAPACITY = 4 * 1024 * 1024; // 4MB
    public static final String FILE_PREFIX = "fmq";
    public static final String PAGE_SUFFIX = ".page";
    public static final String INDEX_SUFFIX = ".index";

    private static final int INDEX_SIZE = 6 * Integer.BYTES;
    private static final int EN_NUM_OFFSET = 0 * Integer.BYTES;
    private static final int EN_POS_OFFSET = 1 * Integer.BYTES;
    private static final int EN_CNT_OFFSET = 2 * Integer.BYTES;
    private static final int DE_NUM_OFFSET = 3 * Integer.BYTES;
    private static final int DE_POS_OFFSET = 4 * Integer.BYTES;
    private static final int DE_CNT_OFFSET = 5 * Integer.BYTES;

    private volatile int enqueuePageNumber;
    private volatile int enqueuePosition;
    private volatile int enqueueCount;
    private volatile int dequeuePageNumber;
    private volatile int dequeuePosition;
    private volatile int dequeueCount;

    private AtomicInteger size;
    private ReentrantLock enqueueLock;
    private ReentrantLock dequeueLock;

    private File dir;
    private RandomAccessFile indexFile;
    private RandomAccessFile enqueuePageFile;
    private RandomAccessFile dequeuePageFile;
    private FileChannel indexFileChannel;
    private FileChannel enqueuePageChannel;
    private FileChannel dequeuePageChannel;
    private MappedByteBuffer enqueueIndex;
    private MappedByteBuffer dequeueIndex;
    private MappedByteBuffer enqueuePage;
    private MappedByteBuffer dequeuePage;
    private ByteBuffer enqueueBuf;
    private ByteBuffer dequeueBuf;

    public FileMappedQueue(String dir) throws IOException {
        if (dir == null || dir.trim().length() == 0) {
            throw new IllegalArgumentException("dir can not be null.");
        }
        File base = new File(dir.endsWith(File.separator) ? dir : dir + File.separator);
        if (!base.exists()) {
            logger.warn("Directory [{}] not exists, creating now...", base.getPath());
            base.mkdirs();
        }
        this.dir = base;
        this.size = new AtomicInteger();
        this.enqueueLock = new ReentrantLock();
        this.dequeueLock = new ReentrantLock();
        if (new File(dir, FILE_PREFIX.concat(INDEX_SUFFIX)).exists()) {
            this.load();
        } else {
            this.init();
        }
    }

    public void load() throws IOException {
        logger.info("Loading page index...");
        File idxFile = new File(dir, FILE_PREFIX.concat(INDEX_SUFFIX));
        this.indexFile = new RandomAccessFile(idxFile, "rw");
        this.indexFileChannel = indexFile.getChannel();
        this.enqueueIndex = indexFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, INDEX_SIZE);
        this.dequeueIndex = (MappedByteBuffer) enqueueIndex.duplicate();
        enqueueIndex.position(EN_NUM_OFFSET);
        this.enqueuePageNumber = enqueueIndex.getInt();
        enqueueIndex.position(EN_POS_OFFSET);
        this.enqueuePosition = enqueueIndex.getInt();
        enqueueIndex.position(EN_CNT_OFFSET);
        this.enqueueCount = enqueueIndex.getInt();
        dequeueIndex.position(DE_NUM_OFFSET);
        this.dequeuePageNumber = enqueueIndex.getInt();
        dequeueIndex.position(DE_POS_OFFSET);
        this.dequeuePosition = enqueueIndex.getInt();
        dequeueIndex.position(DE_CNT_OFFSET);
        this.dequeueCount = enqueueIndex.getInt();
        logger.info("Page index [{}] has successfully loaded.", idxFile.getPath());

        // load en/dequeue page file
        loadEnqueuePage(getPageFile(getPagePath(getEnqueuePageNumber())));
        loadDequeuePage(getPageFile(getPagePath(getDequeuePageNumber())));
    }

    public void init() throws IOException {
        logger.info("Creating page index...");
        File idxFile = new File(dir, FILE_PREFIX.concat(INDEX_SUFFIX));
        idxFile.createNewFile();
        assert idxFile.exists();
        this.indexFile = new RandomAccessFile(idxFile, "rw");
        this.indexFileChannel = indexFile.getChannel();
        this.enqueueIndex = indexFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, INDEX_SIZE);
        this.dequeueIndex = (MappedByteBuffer) enqueueIndex.duplicate();
        this.setEnqueuePageNumber(0);
        this.setEnqueuePosition(0);
        this.setEnqueueCount(0);
        this.setDequeuePageNumber(0);
        this.setDequeuePosition(0);
        this.setDequeueCount(0);
        logger.info("Page index [{}] has successfully initialized.", idxFile.getPath());

        // load en/dequeue page file
        File pageFile = getPageFile(getPagePath(0));
        pageFile.createNewFile();
        assert pageFile.exists();
        loadEnqueuePage(pageFile);
        loadDequeuePage(pageFile);
    }

    private String getPagePath(int pageNumber) {
        return String.format("%s$%d%s", FILE_PREFIX, pageNumber, PAGE_SUFFIX);
    }

    private File getPageFile(String filePath) {
        return new File(dir, filePath);
    }

    private void loadEnqueuePage(File pageFile) throws IOException {
        this.enqueuePageFile = new RandomAccessFile(pageFile, "rw");
        this.enqueuePageChannel = enqueuePageFile.getChannel();
        this.enqueuePage = enqueuePageChannel.map(FileChannel.MapMode.READ_WRITE, 0, PAGE_CAPACITY);
        this.enqueueBuf = enqueuePage.load();
    }

    private void loadDequeuePage(File pageFile) throws IOException {
        this.dequeuePageFile = new RandomAccessFile(pageFile, "rw");
        this.dequeuePageChannel = dequeuePageFile.getChannel();
        this.dequeuePage = dequeuePageChannel.map(FileChannel.MapMode.READ_WRITE, 0, PAGE_CAPACITY);
        this.dequeueBuf = dequeuePage.load();
    }

    private void writeEnqueuePageEnd() {
        this.enqueueBuf.position(getEnqueuePosition());
        this.enqueueBuf.putInt(-1); // EOF
    }

    private boolean isEndOfDequeuePage() {
        return dequeuePosition > 0 && dequeueBuf.getInt(dequeuePosition) == -1; // EOF
    }

    private boolean isEnqueueSpaceAvailable(int tSize) {
        int enqueuePosition = getEnqueuePosition();
        return PAGE_CAPACITY >= enqueuePosition + tSize + 2 * Integer.BYTES; // last byte for EOF
    }

    private void shiftEnqueuePage() throws IOException {
        logger.info("Shifting enqueue page...");
        int nextEnqueuePageNumber = enqueuePageNumber == Integer.MAX_VALUE ? 0 : enqueuePageNumber + 1; // rotate if int overflows
        writeEnqueuePageEnd();
        if (enqueuePageNumber == dequeuePageNumber) {
            enqueuePage.force(); // sync to disk whatever
        } else {
            close(enqueuePageFile, enqueuePageChannel, enqueuePage);
            enqueueBuf = null;
        }
        loadEnqueuePage(getPageFile(getPagePath(nextEnqueuePageNumber)));
        setEnqueuePageNumber(nextEnqueuePageNumber);
        setEnqueuePosition(0);
        logger.info("Enqueue page has successfully shifted to [${}].", enqueuePageNumber);
    }

    private void shiftDequeuePage() throws IOException {
        logger.info("Shifting dequeue page...");
        if (dequeuePageNumber == enqueuePageNumber) {
            return;
        }
        int nextDequeuePageNumber = dequeuePageNumber == Integer.MAX_VALUE ? 0 : dequeuePageNumber + 1; // rotate if int overflows
        close(dequeuePageFile, dequeuePageChannel, dequeuePage);
        dequeueBuf = null;
        new Thread(() -> {
            try {
                int target = dequeuePageNumber;
                int retry = 3;
                logger.info("Removing dequeue page [${}]...", target);
                File pastPage = getPageFile(getPagePath(target));
                while (pastPage.exists()) { // TODO file may not be deleted
                    if (retry <= 0) {
                        logger.error("Fail to delete dequeue page [{}].", pastPage.getPath());
                        pastPage.deleteOnExit();
                        return;
                    }
                    if (pastPage.delete()) {
                        logger.info("Past dequeue page [{}] has been removed.", pastPage.getPath());
                        return;
                    } else {
                        logger.error("Fail to delete dequeue page [{}], retrying ({}) ...", pastPage.getPath(), retry);
                        System.err.println("Failure: " + "\t" + pastPage.getPath());
                    }
                    retry--;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        if (nextDequeuePageNumber == enqueuePageNumber) {
            this.dequeuePageFile = new RandomAccessFile(getPageFile(getPagePath(nextDequeuePageNumber)), "rw");
            this.dequeuePageChannel = dequeuePageFile.getChannel();
            this.dequeuePage = dequeuePageChannel.map(FileChannel.MapMode.READ_WRITE, 0, PAGE_CAPACITY);
            this.dequeueBuf = enqueueBuf.duplicate();
        } else {
            loadDequeuePage(getPageFile(getPagePath(nextDequeuePageNumber)));
        }
        setDequeuePageNumber(nextDequeuePageNumber);
        setDequeuePosition(0);
        logger.info("Dequeue page has successfully shifted to [${}].", dequeuePageNumber);
    }

    private <T> T bytesToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        if (bytes == null) {
            return null;
        }
        try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
             ObjectInputStream inputStream = new ObjectInputStream(stream)) {
            return (T) inputStream.readObject();
        }
//        return bytes == null ? null : (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
    }

    private byte[] objectToBytes(T t) throws IOException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             ObjectOutputStream outputStream = new ObjectOutputStream(stream)) {
            outputStream.writeObject(t);
            return stream.toByteArray();
        }
    }

    private T dequeueFromPage() throws Exception {
        byte[] bytes;
        int dequeuePageNumber = getDequeuePageNumber();
        int dequeuePosition = getDequeuePosition();
        int enqueuePageNumber = getEnqueuePageNumber();
        int enqueuePosition = getEnqueuePosition();
        if (dequeuePageNumber == enqueuePageNumber && dequeuePosition >= enqueuePosition) {
            return null;
        }
        dequeueBuf.position(dequeuePosition);
        int length = dequeueBuf.getInt();
        if (length <= 0) {
            return null;
        }
        bytes = new byte[length];
        dequeueBuf.get(bytes);
        setDequeuePosition(dequeuePosition + Integer.BYTES + length);
        setDequeueCount(getDequeueCount() + 1);
        return bytesToObject(bytes);
    }

    private int enqueueToPage(byte[] bytes) {
        int length = bytes.length;
        int increment = Integer.BYTES + length;
        int enqueuePosition = getEnqueuePosition();
        enqueueBuf.position(enqueuePosition);
        enqueueBuf.putInt(length);
        enqueueBuf.put(bytes);
        setEnqueuePosition(enqueuePosition + increment);
        setEnqueueCount(getEnqueueCount() + 1);
        return increment;
    }

    private void close(RandomAccessFile file, Channel channel, MappedByteBuffer mappedFile) {
        if (mappedFile == null) {
            return;
        }
        // sync to disk
        mappedFile.force();
        try {
            // close mappedFile
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                Method cleaner = mappedFile.getClass().getMethod("cleaner");
                cleaner.setAccessible(true);
                ((Cleaner) cleaner.invoke(mappedFile)).clean();
                return null;
            });
            // close channel
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            // close file
            if (file != null) {
                file.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void setEnqueuePageNumber(int enqueuePageNumber) {
        this.enqueueIndex.position(EN_NUM_OFFSET);
        this.enqueueIndex.putInt(enqueuePageNumber);
        this.enqueuePageNumber = enqueuePageNumber;
    }

    private void setEnqueuePosition(int enqueuePosition) {
        this.enqueueIndex.position(EN_POS_OFFSET);
        this.enqueueIndex.putInt(enqueuePosition);
        this.enqueuePosition = enqueuePosition;
    }

    private void setEnqueueCount(int enqueueCount) {
        this.enqueueIndex.position(EN_CNT_OFFSET);
        this.enqueueIndex.putInt(enqueueCount);
        this.enqueueCount = enqueueCount;
    }

    private void setDequeuePageNumber(int dequeuePageNumber) {
        this.dequeueIndex.position(DE_NUM_OFFSET);
        this.dequeueIndex.putInt(dequeuePageNumber);
        this.dequeuePageNumber = dequeuePageNumber;
    }

    private void setDequeuePosition(int dequeuePosition) {
        this.dequeueIndex.position(DE_POS_OFFSET);
        this.dequeueIndex.putInt(dequeuePosition);
        this.dequeuePosition = dequeuePosition;
    }

    private void setDequeueCount(int dequeueCount) {
        this.dequeueIndex.position(DE_CNT_OFFSET);
        this.dequeueIndex.putInt(dequeueCount);
        this.dequeueCount = dequeueCount;
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return Math.toIntExact(size.get());
    }

    @Override
    public boolean offer(T t) {
        if (t == null) {
            return true;
        }
        enqueueLock.lock();
        try {
            byte[] bytes = objectToBytes(t);
            if (!isEnqueueSpaceAvailable(bytes.length)) {
                shiftEnqueuePage();
            }
            enqueueToPage(bytes);
            size.incrementAndGet();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            enqueueLock.unlock();
        }
    }

    @Override
    public T poll() {
        dequeueLock.lock();
        try {
            if (isEndOfDequeuePage()) {
                shiftDequeuePage();
            }
            T t = dequeueFromPage();
            if (t != null) {
                size.incrementAndGet();
            }
            return t;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            dequeueLock.unlock();
        }
    }

    @Override
    public T peek() {
        throw new UnsupportedOperationException();
    }

    private int getEnqueuePageNumber() {
        return enqueuePageNumber;
    }

    private int getEnqueuePosition() {
        return enqueuePosition;
    }

    private int getEnqueueCount() {
        return enqueueCount;
    }

    private int getDequeuePageNumber() {
        return dequeuePageNumber;
    }

    private int getDequeuePosition() {
        return dequeuePosition;
    }

    private int getDequeueCount() {
        return dequeueCount;
    }
}
