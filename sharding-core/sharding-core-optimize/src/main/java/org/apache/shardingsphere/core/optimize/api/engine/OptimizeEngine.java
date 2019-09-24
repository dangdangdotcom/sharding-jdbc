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

package org.apache.shardingsphere.core.optimize.api.engine;

import org.apache.shardingsphere.core.metadata.table.TableMetas;
import org.apache.shardingsphere.core.optimize.api.statement.OptimizedStatement;
import org.apache.shardingsphere.core.parse.sql.statement.SQLStatement;

import java.util.List;

/**
 * Optimize engine.
 *
 * @author maxiaoguang
 * @author panjuan
 * 
 * @param <T> type of SQL statement
 */
public interface OptimizeEngine<T extends SQLStatement> {
    
    /**
     * Optimize.
     * 
     * @param tableMetas table metas
     * @param sql SQL
     * @param parameters SQL parameters
     * @param sqlStatement SQL statement
     * @return optimized statement
     */
    OptimizedStatement optimize(TableMetas tableMetas, String sql, List<Object> parameters, T sqlStatement);
}
