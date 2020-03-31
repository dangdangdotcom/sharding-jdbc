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

package org.apache.shardingsphere.shardingjdbc.jdbc.refreh.impl;

import org.apache.shardingsphere.shardingjdbc.jdbc.core.context.ShardingRuntimeContext;
import org.apache.shardingsphere.shardingjdbc.jdbc.refreh.MetaDataRefreshStrategy;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.metadata.table.TableMetaData;
import org.apache.shardingsphere.sql.parser.binder.statement.ddl.DropIndexStatementContext;
import org.apache.shardingsphere.sql.parser.sql.statement.ddl.DropIndexStatement;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Drop index statement meta data refresh strategy.
 */
public final class DropIndexStatementMetaDataRefreshStrategy implements MetaDataRefreshStrategy<DropIndexStatementContext> {
   
    @Override
    public void refreshMetaData(final ShardingRuntimeContext shardingRuntimeContext, final DropIndexStatementContext sqlStatementContext) {
        DropIndexStatement dropIndexStatement = sqlStatementContext.getSqlStatement();
        Collection<String> indexNames = getIndexNames(dropIndexStatement);
        TableMetaData tableMetaData = shardingRuntimeContext.getMetaData().getSchema().get(dropIndexStatement.getTable().getTableName().getIdentifier().getValue());
        if (null != dropIndexStatement.getTable()) {
            for (String each : indexNames) {
                tableMetaData.getIndexes().remove(each);
            }
        }
        for (String each : indexNames) {
            if (findLogicTableName(shardingRuntimeContext.getMetaData().getSchema(), each).isPresent()) {
                tableMetaData.getIndexes().remove(each);
            }
        }
    }
    
    private Collection<String> getIndexNames(final DropIndexStatement dropIndexStatement) {
        return dropIndexStatement.getIndexes().stream().map(each -> each.getIdentifier().getValue()).collect(Collectors.toCollection(LinkedList::new));
    }
    
    private Optional<String> findLogicTableName(final SchemaMetaData schemaMetaData, final String logicIndexName) {
        return schemaMetaData.getAllTableNames().stream().filter(each -> schemaMetaData.get(each).getIndexes().containsKey(logicIndexName)).findFirst();
    }
}
