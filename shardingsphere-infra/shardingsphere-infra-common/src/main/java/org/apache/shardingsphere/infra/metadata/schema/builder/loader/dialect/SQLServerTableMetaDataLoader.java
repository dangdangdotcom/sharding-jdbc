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

package org.apache.shardingsphere.infra.metadata.schema.builder.loader.dialect;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.shardingsphere.infra.metadata.schema.builder.loader.DataTypeLoader;
import org.apache.shardingsphere.infra.metadata.schema.builder.spi.DialectTableMetaDataLoader;
import org.apache.shardingsphere.infra.metadata.schema.model.ColumnMetaData;
import org.apache.shardingsphere.infra.metadata.schema.model.IndexMetaData;
import org.apache.shardingsphere.infra.metadata.schema.model.TableMetaData;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Table meta data loader for SQLServer.
 * @author totalo
 * @date 20210316
 */
public final class SQLServerTableMetaDataLoader implements DialectTableMetaDataLoader {

    private static final String BASIC_TABLE_META_DATA_SQL = "SELECT obj.name AS TABLE_NAME,col.name AS COLUMN_NAME,t.name AS DATA_TYPE,"
            + "col.collation_name AS COLLATION_NAME, is_identity AS IS_IDENTITY, "
            + "(SELECT top 1 ind.is_primary_key FROM sys.index_columns ic LEFT JOIN sys.indexes ind ON ic.object_id=ind.object_id "
            + "AND ic.index_id=ind.index_id AND ind.name LIKE 'PK_%' where ic.object_id=obj.object_id AND ic.column_id=col.column_id) AS IS_PRIMARY_KEY"
            + "FROM sys.objects obj inner join sys.columns col ON obj.object_id=col.object_id LEFT JOIN sys.types t ON t.user_type_id=col.user_type_id";

    private static final String BASIC_INDEX_META_DATA_SQL = "SELECT a.name AS INDEX_NAME, c.name AS TABLE_NAME FROM sysindexes a "
            + "JOIN sysobjects c ON a.id=c.id WHERE a.indid NOT IN (0, 255) AND c.name IN (%s)";

    private static final String SQL_WITH_EXISTED_TABLES = " AND c.name NOT IN (%s)";

    private static final String SQL_EXISTED_TABLES = " WHERE c.name NOT IN (%s)";

    @Override
    public Map<String, TableMetaData> load(final DataSource dataSource, final Collection<String> existedTables) throws SQLException {
        return loadTableMetaDataMap(dataSource, existedTables);
    }

    private Map<String, TableMetaData> loadTableMetaDataMap(final DataSource dataSource, final Collection<String> existedTables) throws SQLException {
        Map<String, TableMetaData> result = new LinkedHashMap<>();
        Map<String, Collection<ColumnMetaData>> columnMetaDataMap = loadColumnMetaDataMap(dataSource, existedTables);
        if (CollectionUtils.isNotEmpty(columnMetaDataMap.keySet())) {
            Map<String, Collection<IndexMetaData>> indexMetaDataMap = loadIndexMetaData(dataSource, columnMetaDataMap.keySet(), existedTables);
            for (Map.Entry<String, Collection<ColumnMetaData>> entry : columnMetaDataMap.entrySet()) {
                result.put(entry.getKey(), new TableMetaData(entry.getValue(), indexMetaDataMap.get(entry.getKey())));
            }
        }
        return result;
    }

    private Map<String, Collection<IndexMetaData>> loadIndexMetaData(final DataSource dataSource, final Collection<String> tableNames, final Collection<String> existedTables) throws SQLException {
        Map<String, Collection<IndexMetaData>> result = new HashMap<>();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(getIndexMetaDataSQL(tableNames, existedTables))) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String indexName = resultSet.getString("INDEX_NAME");
                    String tableName = resultSet.getString("TABLE_NAME");
                    if (!result.containsKey(tableName)) {
                        result.put(tableName, new LinkedList<>());
                    }
                    result.get(tableName).add(new IndexMetaData(indexName));
                }
            }
        }
        return result;
    }

    private Map<String, Collection<ColumnMetaData>> loadColumnMetaDataMap(final DataSource dataSource, final Collection<String> existedTables) throws SQLException {
        Map<String, Collection<ColumnMetaData>> result = new HashMap<>();
        try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(getTableMetaDataSQL(existedTables))) {
            Map<String, Integer> dataTypes = DataTypeLoader.load(connection.getMetaData());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    ColumnMetaData columnMetaData = loadColumnMetaData(dataTypes, resultSet);
                    if (!result.containsKey(tableName)) {
                        result.put(tableName, new LinkedList<>());
                    }
                    result.get(tableName).add(columnMetaData);
                }
            }
        }
        return result;
    }

    private ColumnMetaData loadColumnMetaData(final Map<String, Integer> dataTypeMap, final ResultSet resultSet) throws SQLException {
        String columnName = resultSet.getString("COLUMN_NAME");
        String dataType = resultSet.getString("DATA_TYPE");
        String collationName = resultSet.getString("COLLATION_NAME");
        boolean primaryKey = "1".equals(resultSet.getString("IS_PRIMARY_KEY"));
        boolean generated = "1".equals(resultSet.getString("IS_IDENTITY"));
        boolean caseSensitive = null != collationName && collationName.indexOf("_CS") > 0;
        return new ColumnMetaData(columnName, dataTypeMap.get(dataType), primaryKey, generated, caseSensitive);
    }

    private String getTableMetaDataSQL(final Collection<String> existedTables) {
        return existedTables.isEmpty() ? BASIC_TABLE_META_DATA_SQL
                : BASIC_TABLE_META_DATA_SQL + String.format(SQL_EXISTED_TABLES, existedTables.stream().map(each -> String.format("'%s'", each)).collect(Collectors.joining(",")));
    }

    private String getIndexMetaDataSQL(final Collection<String> tableNames, final Collection<String> existedTables) {
        String sql = String.format(BASIC_INDEX_META_DATA_SQL, tableNames.stream().map(each -> String.format("'%s'", each)).collect(Collectors.joining(",")));
        return existedTables.isEmpty() ? sql
                : sql + String.format(SQL_WITH_EXISTED_TABLES, existedTables.stream().map(each -> String.format("'%s'", each)).collect(Collectors.joining(",")));
    }

    @Override
    public String getDatabaseType() {
        return "SQLServer";
    }
}
