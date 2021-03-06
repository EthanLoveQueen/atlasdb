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
package com.palantir.nexus.db.monitoring.timer;

import com.palantir.nexus.db.monitoring.profiler.SqlProfilers;
import com.palantir.util.sql.SqlStats;

final public class DurationSqlTimer implements SqlTimer {
    @Override
    public Handle start(String module, final String sqlKey, final String rawSql) {
        final long startNs = System.nanoTime();
        return new Handle() {
            @Override
            public void stop() {
                long durationNs = System.nanoTime() - startNs;
                SqlStats.INSTANCE.updateStats(sqlKey, rawSql, durationNs);
                SqlProfilers.getSqlProfiler().update(sqlKey, rawSql, durationNs);
            }
        };
    }
}
