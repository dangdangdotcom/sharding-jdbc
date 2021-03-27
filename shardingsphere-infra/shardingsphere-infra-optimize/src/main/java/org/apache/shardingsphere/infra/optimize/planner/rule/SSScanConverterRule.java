/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.optimize.planner.rule;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.shardingsphere.infra.optimize.planner.ShardingSphereConvention;
import org.apache.shardingsphere.infra.optimize.rel.logical.LogicalScan;
import org.apache.shardingsphere.infra.optimize.rel.physical.SSScan;

public final class SSScanConverterRule extends ConverterRule {
    
    public static final Config DEFAULT_CONFIG = Config.INSTANCE
            .withConversion(LogicalScan.class, Convention.NONE,
                    ShardingSphereConvention.INSTANCE, SSScanConverterRule.class.getName())
            .withRuleFactory(SSScanConverterRule::new);
    
    protected SSScanConverterRule(final Config config) {
        super(config);
    }
    
    
    @Override
    public RelNode convert(final RelNode rel) {
        LogicalScan logicalScan = (LogicalScan) rel;
        RelNode input = logicalScan.peek();
        SSScan scan = SSScan.create(logicalScan.getCluster(), logicalScan.getTraitSet(), input);
        return convert(scan, logicalScan.getTraitSet().replace(ShardingSphereConvention.INSTANCE));
    }
}
