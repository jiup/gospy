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

package cc.gospy.core.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface ExitCallback {
    Logger logger = LoggerFactory.getLogger(ExitCallback.class);

    ExitCallback DEFAULT = () -> {
        // In Gospy, other components' activities are invisible to a scheduler, so this scheduler
        // will shutdown itself in few seconds, which is naturally adapting to distribution
        // environments. However, in func programs, this might cause a premature interruption,
        // we cannot ensure other components' (such as a pipeline) functions are finished. Thus,
        // if you are running your program in func mode and want to keep the subsequent
        // process completely done (might in minutes), please turn off the auto exit.
        logger.info("All tasks are fed back, terminate in few seconds.");
        try {
            Thread.currentThread().join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("Bye!");
        System.exit(0);
    };

    void onExit();
}
