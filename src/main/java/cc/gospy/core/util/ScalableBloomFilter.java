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

package cc.gospy.core.util;

import cc.gospy.core.entity.Task;
import com.google.common.hash.BloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ScalableBloomFilter implements Serializable {
    public static Logger logger = LoggerFactory.getLogger(ScalableBloomFilter.class);

    private List<BloomFilter<Task>> bloomFilters;
    private BloomFilter<Task> activateBloomFilter;
    private final long expectedInsertions;
    private final double fpp;

    public ScalableBloomFilter() {
        this(10000000L, 0.000001D);
    }

    public ScalableBloomFilter(long expectedInsertions, double fpp) {
        this.bloomFilters = new ArrayList<>();
        this.expectedInsertions = expectedInsertions;
        this.fpp = fpp;
        this.grow();
    }

    private BloomFilter<Task> grow() {
        this.activateBloomFilter = BloomFilter.create(Task.DIGEST, expectedInsertions, fpp);
        this.bloomFilters.add(activateBloomFilter);
        return activateBloomFilter;
    }

    public boolean put(Task task) {
        if (activateBloomFilter.expectedFpp() > fpp) {
            grow();
        }
        return activateBloomFilter.put(task);
    }

    public boolean mightContain(Task task) {
        for (BloomFilter<Task> filter : bloomFilters) {
            if (filter.mightContain(task)) {
                return true;
            }
        }
        return false;
    }

    public void saveToFile(String dir) throws IOException {
        for (int i = 0; i < bloomFilters.size(); i++) {
            File file = new File(dir, this.getClass().getTypeName() + "$" + i + ".tmp");
            logger.info("Saving bloom filter data ${} to file {}...", i, file.getPath());
            FileOutputStream outputStream = new FileOutputStream(file, false);
            bloomFilters.get(i).writeTo(outputStream);
            outputStream.close();
        }
        logger.info("Bloom filter data [$0-${}] is successfully saved.", groupSize() - 1);
    }

    public void readFromFile(String dir) throws IOException {
        File file;
        int p = 0;
        while ((file = new File(dir, this.getClass().getTypeName() + "$" + p + ".tmp")).exists()) {
            logger.info("Reading bloom filter data ${} from file {}...", p, file.getPath());
            FileInputStream inputStream = new FileInputStream(file);
            this.bloomFilters.clear();
            bloomFilters.add(activateBloomFilter = BloomFilter.readFrom(inputStream, Task.DIGEST));
            p++;
        }
        logger.info("Bloom filter data [$0-${}] is successfully loaded.", groupSize() - 1);
    }

    public double expectedFpp() {
        return activateBloomFilter.expectedFpp();
    }

    public int groupSize() {
        return bloomFilters.size();
    }

}
