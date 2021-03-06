/*
 * Copyright 2017 Palantir Technologies
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
package com.palantir.atlasdb.keyvalue.impl;

import org.junit.Rule;

import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.util.AtlasDbMetrics;
import com.palantir.atlasdb.util.MetricsRule;

public class InstrumentedKeyValueServiceTest extends AbstractKeyValueServiceTest {

    private static final String METRIC_PREFIX = "test.instrumented." + KeyValueService.class.getName();

    @Rule
    public MetricsRule metricsRule = new MetricsRule();

    @Override
    protected KeyValueService getKeyValueService() {
        return AtlasDbMetrics.instrument(KeyValueService.class,
                new InMemoryKeyValueService(false),
                METRIC_PREFIX);
    }

    /**
     * Tear down KVS so that it is recreated and captures metrics for each test.
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        tearDownKvs();
    }

}
