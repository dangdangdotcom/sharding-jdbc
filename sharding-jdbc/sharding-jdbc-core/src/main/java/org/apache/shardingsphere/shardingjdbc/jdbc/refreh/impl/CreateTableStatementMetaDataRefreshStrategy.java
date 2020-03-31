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
import org.apache.shardingsphere.shardingjdbc.jdbc.refreh.AbstractTableStatementMetaData;
import org.apache.shardingsphere.shardingjdbc.jdbc.refreh.MetaDataRefreshStrategy;
import org.apache.shardingsphere.sql.parser.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.statement.ddl.CreateTableStatement;

import java.sql.SQLException;

/**
 * Create table statement meta data refresh strategy.
 */
public final class CreateTableStatementMetaDataRefreshStrategy extends AbstractTableStatementMetaData implements MetaDataRefreshStrategy<CreateTableStatement> {
    
    @Override
    public void refreshMetaData(final ShardingRuntimeContext shardingRuntimeContext, final SQLStatementContext<CreateTableStatement> sqlStatementContext) throws SQLException {
        String tableName = sqlStatementContext.getSqlStatement().getTable().getTableName().getIdentifier().getValue();
        shardingRuntimeContext.getMetaData().getSchema().put(tableName, loadTableMetaData(tableName, shardingRuntimeContext));
    }
}
