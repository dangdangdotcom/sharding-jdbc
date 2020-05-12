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

package org.apache.shardingsphere.shardingjdbc.jdbc.core.statement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.core.rule.ShadowRule;
import org.apache.shardingsphere.shadow.rewrite.judgement.ShadowJudgementEngine;
import org.apache.shardingsphere.shadow.rewrite.judgement.impl.SimpleJudgementEngine;
import org.apache.shardingsphere.shardingjdbc.jdbc.adapter.AbstractStatementAdapter;
import org.apache.shardingsphere.shardingjdbc.jdbc.core.connection.ShadowConnection;
import org.apache.shardingsphere.sql.parser.binder.SQLStatementContextFactory;
import org.apache.shardingsphere.sql.parser.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.statement.SQLStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.DMLStatement;
import org.apache.shardingsphere.underlying.common.config.properties.ConfigurationPropertyKey;
import org.apache.shardingsphere.underlying.rewrite.SQLRewriteEntry;
import org.apache.shardingsphere.underlying.rewrite.engine.result.GenericSQLRewriteResult;
import org.apache.shardingsphere.underlying.rewrite.engine.result.SQLRewriteUnit;
import org.apache.shardingsphere.underlying.route.context.RouteContext;
import org.apache.shardingsphere.underlying.route.context.RouteResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Shadow statement.
 */
@Slf4j
public final class ShadowStatement extends AbstractStatementAdapter {
    
    @Getter
    private final ShadowConnection connection;
    
    private final ShadowStatementGenerator shadowStatementGenerator;
    
    private SQLStatementContext sqlStatementContext;
    
    private List<Statement> statements;
    
    private ResultSet resultSet;
    
    private boolean isShadowSQL;
    
    public ShadowStatement(final ShadowConnection connection) {
        this(connection, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }
    
    public ShadowStatement(final ShadowConnection connection, final int resultSetType, final int resultSetConcurrency) {
        this(connection, resultSetType, resultSetConcurrency, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }
    
    public ShadowStatement(final ShadowConnection connection, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
        super(Statement.class);
        this.connection = connection;
        shadowStatementGenerator = new ShadowStatementGenerator(resultSetType, resultSetConcurrency, resultSetHoldability);
    }
    
    @Override
    public ResultSet executeQuery(final String sql) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        for (Statement statement : statementAndReplay) {
            resultSet = statement.executeQuery(rewriteSQL(sql));
        }
        return resultSet;
    }
    
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return statements.get(0).getGeneratedKeys();
    }
    
    @Override
    public int executeUpdate(final String sql) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        int result = 0;
        for (Statement statement : statementAndReplay) {
            result = statement.executeUpdate(rewriteSQL(sql));
        }
        return result;
    }
    
    @Override
    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        int result = 0;
        for (Statement statement : statementAndReplay) {
            result = statement.executeUpdate(rewriteSQL(sql), autoGeneratedKeys);
        }
        return result;
    }
    
    @Override
    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        int result = 0;
        for (Statement statement : statementAndReplay) {
            result = statement.executeUpdate(rewriteSQL(sql), columnIndexes);
        }
        return result;
    }
    
    @Override
    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        int result = 0;
        for (Statement statement : statementAndReplay) {
            result = statement.executeUpdate(rewriteSQL(sql), columnNames);
        }
        return result;
    }
    
    @Override
    public boolean execute(final String sql) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        boolean result = false;
        for (Statement statement : statementAndReplay) {
            result = statement.execute(rewriteSQL(sql));
        }
        resultSet = statements.get(0).getResultSet();
        return result;
    }
    
    @Override
    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        boolean result = false;
        for (Statement statement : statementAndReplay) {
            result = statement.execute(rewriteSQL(sql), autoGeneratedKeys);
        }
        resultSet = statements.get(0).getResultSet();
        return result;
    }
    
    @Override
    public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        boolean result = false;
        for (Statement statement : statementAndReplay) {
            result = statement.execute(rewriteSQL(sql), columnIndexes);
        }
        resultSet = statements.get(0).getResultSet();
        return result;
    }
    
    @Override
    public boolean execute(final String sql, final String[] columnNames) throws SQLException {
        List<Statement> statementAndReplay = getStatementAndReplay(sql);
        boolean result = false;
        for (Statement statement : statementAndReplay) {
            result = statement.execute(rewriteSQL(sql), columnNames);
        }
        resultSet = statements.get(0).getResultSet();
        return result;
    }
    
    @Override
    public ResultSet getResultSet() {
        return resultSet;
    }
    
    @Override
    public int getResultSetConcurrency() {
        return shadowStatementGenerator.resultSetConcurrency;
    }
    
    @Override
    public int getResultSetType() {
        return shadowStatementGenerator.resultSetType;
    }
    
    @Override
    public int getResultSetHoldability() {
        return shadowStatementGenerator.resultSetHoldability;
    }
    
    @Override
    protected boolean isAccumulate() {
        return false;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Statement> getRoutedStatements() {
        return statements;
    }
    
    private List<Statement> getStatementAndReplay(final String sql) throws SQLException {
        List<Statement> result = new ArrayList<>();
        SQLStatement sqlStatement = connection.getRuntimeContext().getSqlParserEngine().parse(sql, false);
        sqlStatementContext = SQLStatementContextFactory.newInstance(connection.getRuntimeContext().getMetaData().getSchema().getSchemaMetaData(), sql, Collections.emptyList(), sqlStatement);
        if (sqlStatement instanceof DMLStatement) {
            ShadowJudgementEngine shadowJudgementEngine = new SimpleJudgementEngine((ShadowRule) connection.getRuntimeContext().getRules().iterator().next(), sqlStatementContext);
            isShadowSQL = shadowJudgementEngine.isShadowSQL();
            result.add(shadowStatementGenerator.createStatement(isShadowSQL));
        } else {
            result.add(shadowStatementGenerator.createStatement(true));
            result.add(shadowStatementGenerator.createStatement(false));
        }
        statements = result;
        return result;
    }
    
    private String rewriteSQL(final String sql) {
        SQLRewriteEntry sqlRewriteEntry = new SQLRewriteEntry(connection.getRuntimeContext().getMetaData().getSchema().getConfiguredSchemaMetaData(),
                connection.getRuntimeContext().getProperties(), connection.getRuntimeContext().getRules());
        SQLRewriteUnit sqlRewriteResult = ((GenericSQLRewriteResult) sqlRewriteEntry.rewrite(sql, Collections.emptyList(),
                new RouteContext(sqlStatementContext, Collections.emptyList(), new RouteResult()))).getSqlRewriteUnit();
        String result = sqlRewriteResult.getSql();
        showSQL(result);
        return result;
    }
    
    private void showSQL(final String sql) {
        if (connection.getRuntimeContext().getProperties().<Boolean>getValue(ConfigurationPropertyKey.SQL_SHOW)) {
            log.info("Rule Type: shadow");
            log.info("SQL: {} ::: IsShadowSQL: {}", sql, isShadowSQL);
        }
    }
    
    @RequiredArgsConstructor
    private final class ShadowStatementGenerator {
        
        private final int resultSetType;
        
        private final int resultSetConcurrency;
        
        private final int resultSetHoldability;
        
        private Statement createStatement(final boolean isShadowSQL) throws SQLException {
            if (-1 != resultSetType && -1 != resultSetConcurrency && -1 != resultSetHoldability) {
                return isShadowSQL ? connection.getShadowConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability)
                        : connection.getActualConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
            }
            if (-1 != resultSetType && -1 != resultSetConcurrency) {
                return isShadowSQL ? connection.getShadowConnection().createStatement(resultSetType, resultSetConcurrency)
                        : connection.getActualConnection().createStatement(resultSetType, resultSetConcurrency);
            }
            return isShadowSQL ? connection.getShadowConnection().createStatement() : connection.getActualConnection().createStatement();
        }
    }
}
