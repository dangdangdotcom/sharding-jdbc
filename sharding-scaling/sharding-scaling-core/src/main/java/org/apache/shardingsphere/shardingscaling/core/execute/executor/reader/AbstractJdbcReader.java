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

package org.apache.shardingsphere.shardingscaling.core.execute.executor.reader;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.shardingscaling.core.config.JdbcDataSourceConfiguration;
import org.apache.shardingsphere.shardingscaling.core.config.RdbmsConfiguration;
import org.apache.shardingsphere.shardingscaling.core.exception.SyncTaskExecuteException;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.AbstractSyncRunner;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.channel.Channel;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.position.NopLogPosition;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.record.Column;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.record.DataRecord;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.record.FinishedRecord;
import org.apache.shardingsphere.shardingscaling.core.execute.executor.record.Record;
import org.apache.shardingsphere.shardingscaling.core.util.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * generic jdbc reader implement.
 *
 * @author avalon566
 * @author yangyi
 */
@Slf4j
public abstract class AbstractJdbcReader extends AbstractSyncRunner implements JdbcReader {
    
    @Getter(AccessLevel.PROTECTED)
    private final RdbmsConfiguration rdbmsConfiguration;
    
    private final DataSourceFactory dataSourceFactory;
    
    @Setter
    private Channel channel;
    
    public AbstractJdbcReader(final RdbmsConfiguration rdbmsConfiguration, final DataSourceFactory dataSourceFactory) {
        if (!JdbcDataSourceConfiguration.class.equals(rdbmsConfiguration.getDataSourceConfiguration().getClass())) {
            throw new UnsupportedOperationException("AbstractJdbcReader only support JdbcDataSourceConfiguration");
        }
        this.rdbmsConfiguration = rdbmsConfiguration;
        this.dataSourceFactory = dataSourceFactory;
    }
    
    @Override
    public final void run() {
        start();
        read(channel);
    }
    
    @Override
    public final void read(final Channel channel) {
        try (Connection conn = dataSourceFactory.getDataSource(rdbmsConfiguration.getDataSourceConfiguration()).getConnection()) {
            String sql = String.format("select * from %s %s", rdbmsConfiguration.getTableName(), rdbmsConfiguration.getWhereCondition());
            PreparedStatement ps = createPreparedStatement(conn, sql);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            while (isRunning() && rs.next()) {
                DataRecord record = new DataRecord(new NopLogPosition(), metaData.getColumnCount());
                record.setType("bootstrap-insert");
                record.setTableName(rdbmsConfiguration.getTableNameMap().get(rdbmsConfiguration.getTableName()));
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    record.addColumn(new Column(readValue(rs, i), true));
                }
                pushRecord(record);
            }
        } catch (SQLException e) {
            throw new SyncTaskExecuteException(e);
        } finally {
            pushRecord(new FinishedRecord(new NopLogPosition()));
        }
    }
    
    protected abstract PreparedStatement createPreparedStatement(Connection conn, String sql) throws SQLException;
    
    /**
     * Read value from {@code ResultSet}.
     *
     * @param resultSet result set
     * @param index of read column
     * @return value
     * @throws SQLException sql exception
     */
    protected Object readValue(final ResultSet resultSet, final int index) throws SQLException {
        return resultSet.getObject(index);
    }
    
    private void pushRecord(final Record record) {
        try {
            channel.pushRecord(record);
        } catch (InterruptedException ignored) {
        }
    }
}
