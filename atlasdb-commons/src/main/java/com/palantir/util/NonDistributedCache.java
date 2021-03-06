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
package com.palantir.util;

/**
 * This is a minimal interface for a cache that is not distrubted and thus can support
 * operations that are difficult over multiple boxes.
 *
 * @author dkramer
 */
public interface NonDistributedCache<K, V> extends DistributedCacheMgrCache<K, V>{
    int size();
    void clear();
    boolean containsKey(K key);
}
