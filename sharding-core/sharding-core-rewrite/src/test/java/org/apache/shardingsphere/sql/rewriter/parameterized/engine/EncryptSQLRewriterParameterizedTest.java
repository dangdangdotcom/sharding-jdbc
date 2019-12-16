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

package org.apache.shardingsphere.sql.rewriter.parameterized.engine;

import com.google.common.base.Preconditions;
import org.apache.shardingsphere.core.metadata.table.TableMetas;
import org.apache.shardingsphere.core.rule.EncryptRule;
import org.apache.shardingsphere.core.yaml.config.encrypt.YamlRootEncryptRuleConfiguration;
import org.apache.shardingsphere.core.yaml.engine.YamlEngine;
import org.apache.shardingsphere.core.yaml.swapper.impl.EncryptRuleConfigurationYamlSwapper;
import org.apache.shardingsphere.sql.parser.SQLParseEngineFactory;
import org.apache.shardingsphere.sql.parser.relation.SQLStatementContextFactory;
import org.apache.shardingsphere.sql.parser.relation.metadata.RelationMetas;
import org.apache.shardingsphere.sql.parser.relation.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.statement.SQLStatement;
import org.apache.shardingsphere.sql.rewriter.context.SQLRewriteContext;
import org.apache.shardingsphere.sql.rewriter.engine.SQLRewriteResult;
import org.apache.shardingsphere.sql.rewriter.engine.impl.DefaultSQLRewriteEngine;
import org.apache.shardingsphere.sql.rewriter.feature.encrypt.context.EncryptSQLRewriteContextDecorator;
import org.apache.shardingsphere.sql.rewriter.parameterized.engine.parameter.SQLRewriteEngineTestParameters;
import org.apache.shardingsphere.sql.rewriter.parameterized.engine.parameter.SQLRewriteEngineTestParametersBuilder;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class EncryptSQLRewriterParameterizedTest extends AbstractSQLRewriterParameterizedTest {
    
    private static final String PATH = "encrypt";
    
    public EncryptSQLRewriterParameterizedTest(final String type, final String name, final String fileName, final SQLRewriteEngineTestParameters testParameters) {
        super(testParameters);
    }
    
    @Parameters(name = "{0}: {1} -> {2}")
    public static Collection<Object[]> loadTestParameters() {
        return SQLRewriteEngineTestParametersBuilder.loadTestParameters(PATH.toUpperCase(), PATH, EncryptSQLRewriterParameterizedTest.class);
    }
    
    @Override
    protected Collection<SQLRewriteResult> createSQLRewriteResults() throws IOException {
        YamlRootEncryptRuleConfiguration ruleConfiguration = createRuleConfiguration();
        EncryptRule encryptRule = new EncryptRule(new EncryptRuleConfigurationYamlSwapper().swap(ruleConfiguration.getEncryptRule()));
        boolean isQueryWithCipherColumn = (boolean) ruleConfiguration.getProps().get("query.with.cipher.column");
        SQLRewriteContext sqlRewriteContext = createSQLRewriteContext();
        new EncryptSQLRewriteContextDecorator(encryptRule, isQueryWithCipherColumn).decorate(sqlRewriteContext);
        sqlRewriteContext.generateSQLTokens();
        return Collections.singletonList(new DefaultSQLRewriteEngine().rewrite(sqlRewriteContext));
    }
    
    private SQLRewriteContext createSQLRewriteContext() {
        SQLStatement sqlStatement = SQLParseEngineFactory.getSQLParseEngine(
                null == getTestParameters().getDatabaseType() ? "SQL92" : getTestParameters().getDatabaseType()).parse(getTestParameters().getInputSQL(), false);
        SQLStatementContext sqlStatementContext = SQLStatementContextFactory.newInstance(
                createRelationMetas(), getTestParameters().getInputSQL(), getTestParameters().getInputParameters(), sqlStatement);
        return new SQLRewriteContext(mock(TableMetas.class), sqlStatementContext, getTestParameters().getInputSQL(), getTestParameters().getInputParameters());
    }
    
    private RelationMetas createRelationMetas() {
        RelationMetas result = mock(RelationMetas.class);
        when(result.getAllColumnNames("t_account")).thenReturn(Arrays.asList("account_id", "certificate_number", "password", "amount", "status"));
        when(result.getAllColumnNames("t_account_bak")).thenReturn(Arrays.asList("account_id", "certificate_number", "password", "amount", "status"));
        return result;
    }
    
    private YamlRootEncryptRuleConfiguration createRuleConfiguration() throws IOException {
        URL url = EncryptSQLRewriterParameterizedTest.class.getClassLoader().getResource(getTestParameters().getRuleFile());
        Preconditions.checkNotNull(url, "Cannot found rewrite rule yaml configuration.");
        return YamlEngine.unmarshal(new File(url.getFile()), YamlRootEncryptRuleConfiguration.class);
    }
}
