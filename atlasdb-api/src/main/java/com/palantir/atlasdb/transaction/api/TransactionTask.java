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
package com.palantir.atlasdb.transaction.api;

/**
 * [TO COME: Interface Introduction].
 * <p>
 * It is important that the {@link TransactionTask} does not modify any of its input state
 * in any non-idempotent way.  If this task gets retried, and if you modified your input, then the
 * second try might not do the right thing.  For example: if you are passed a list of objects
 * and at the end of the {@link TransactionTask}, you clear the list.  If your task gets retried
 * it will have no work to do, because the list was cleared.
 */
public interface TransactionTask<T, E extends Exception> {
    T execute(Transaction transaction) throws E;
}
