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

package org.apache.shardingsphere.core.route.type.defaultdb;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.route.type.RoutingEngine;
import org.apache.shardingsphere.core.route.type.RoutingResult;
import org.apache.shardingsphere.core.route.type.RoutingTable;
import org.apache.shardingsphere.core.route.type.TableUnit;
import org.apache.shardingsphere.core.rule.ShardingRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Default datasource engine.
 * 
 * @author maxiaoguang
 */
@RequiredArgsConstructor
public final class DefaultDatabaseRoutingEngine implements RoutingEngine {
    
    private final ShardingRule shardingRule;
    
    private final Collection<String> logicTables;
    
    @Override
    public RoutingResult route() {
        RoutingResult result = new RoutingResult();
        List<RoutingTable> routingTables = new ArrayList<>(logicTables.size());
        for (String each : logicTables) {
            routingTables.add(new RoutingTable(each, each));
        }
        TableUnit tableUnit = new TableUnit(shardingRule.getShardingDataSourceNames().getDefaultDataSourceName());
        tableUnit.getRoutingTables().addAll(routingTables);
        result.getTableUnits().getTableUnits().add(tableUnit);
        return result;
    }
}
