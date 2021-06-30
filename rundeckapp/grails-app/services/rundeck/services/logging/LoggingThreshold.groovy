/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.services.logging

import rundeck.services.execution.ThresholdValue
import rundeck.services.execution.ValueHolder
import rundeck.services.execution.ValueWatcher

/**
 * Defines a threshold for some logging statistic
 */
class LoggingThreshold implements ThresholdValue<Long>, ValueWatcher<Long> {
    public static final String TOTAL_LINES     = "lines"
    public static final String LINES_PER_NODE  = "linesPerNode"
    public static final String TOTAL_FILE_SIZE = "size"
    public static final String ACTION_HALT     = "halt"
    public static final String ACTION_TRUNCATE = "truncate"
    /**
     * Warning log file size value in bytes (300MB)
     */
    public static final Long   WARNING_SIZE    = 1024//314572800

    Long maxValue
    ValueHolder<Long> valueHolder
    String action
    String description
    String type

    @Override
    void watch(final ValueHolder<Long> holder) {
        this.valueHolder = holder
    }

    /**
     * Create a logging threshold, which watches for a certain value
     * @param map either [maxSizeBytes: Long] or [maxLines: Long, perNode: boolean]
     * @param action action type to perform
     * @return instance , or null if the map is not valid
     */
    static LoggingThreshold fromMap(Map map, String action) {
        def t = new LoggingThreshold()
        t.action = action
        if (!map) {
            return null
        }
        if (map.maxSizeBytes) {
            t.maxValue = map.maxSizeBytes
            t.description = "Maximum size in Bytes: " + t.maxValue
            t.type = TOTAL_FILE_SIZE
        } else if (map.maxLines) {
            t.maxValue = map.maxLines
            if (map.perNode) {
                t.description = "Maximum node line count: " + t.maxValue
                t.type = LINES_PER_NODE
            } else {
                t.description = "Maximum Line count: " + t.maxValue
                t.type = TOTAL_LINES
            }
        } else {
            return null
        }
        t.description += ", action: " + action
        return t
    }

    /**
     * @param type statistic name
     * @return watcher to receive a holder, or null
     */
    public ValueWatcher<Long> watcherForType(String type) {
        return this.type == type ? this : null
    }

    @Override
    boolean isThresholdExceeded() {
        return getValue() > maxValue
    }

    @Override
    Long getValue() {
        def value = valueHolder?.value
        value != null ? value : -1
    }

    /**
     * @return true if abort action is set
     */
    boolean isHaltOnLimitReached() {
        action == ACTION_HALT
    }

    /**
     * @return true if truncate action is set
     */
    boolean isTruncateOnLimitReached() {
        action == ACTION_TRUNCATE
    }

    /**
     * @return true if log file size is over a warning size alert
     */
    boolean isWarningSizeReached() {
        return getValue() > WARNING_SIZE
    }
}
