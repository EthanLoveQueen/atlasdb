/*
 * Copyright 2015 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.timestamp;

public interface TimestampBoundStore {
    /**
     * This will be called when the timestamp server is first created.
     *
     * @return the current timestamp upper limit that is persisted
     */
    long getUpperLimit();

    /**
     * Persists a new timestamp upper limit. No timestamps greater than the stored upper limit should ever be
     * handed out.
     *
     * @param limit the new upper limit to be stored
     * @throws MultipleRunningTimestampServiceError if the timestamp limit has changed out from under us
     */
    void storeUpperLimit(long limit) throws MultipleRunningTimestampServiceError;
}
