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
import lombok.RequiredArgsConstructor;
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
import org.apache.shardingsphere.sql.rewriter.parameterized.jaxb.engine.SQLRewriteEngineTestParametersBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
@RequiredArgsConstructor
public final class EncryptSQLRewriteEngineParameterizedTest {
    
    private static final String PATH = "encrypt";
    
    private final String fileName;
    
    private final String ruleFile;
    
    private final String name;
    
    private final String inputSQL;
    
    private final List<Object> inputParameters;
    
    private final List<String> outputSQLs;
    
    private final List<List<Object>> outputGroupedParameters;
    
    private final String databaseType;
    
    @Parameters(name = "ENCRYPT: {2} -> {0}")
    public static Collection<Object[]> getTestParameters() {
        return SQLRewriteEngineTestParametersBuilder.loadTestParameters(PATH, EncryptSQLRewriteEngineParameterizedTest.class);
    }
    
    @Test
    public void assertRewrite() throws IOException {
        Collection<SQLRewriteResult> actual = getSQLRewriteResults();
        assertThat(actual.size(), is(outputSQLs.size()));
        int count = 0;
        for (SQLRewriteResult each : actual) {
            assertThat(each.getSql(), is(outputSQLs.get(count)));
            assertThat(each.getParameters().size(), is(outputGroupedParameters.get(count).size()));
            for (int i = 0; i < each.getParameters().size(); i++) {
                assertThat(each.getParameters().get(i).toString(), is(outputGroupedParameters.get(count).get(i).toString()));
            }
            count++;
        }
    }
    
    private Collection<SQLRewriteResult> getSQLRewriteResults() throws IOException {
        SQLRewriteContext sqlRewriteContext = getSQLRewriteContext();
        YamlRootEncryptRuleConfiguration ruleConfiguration = createRuleConfiguration();
        EncryptRule encryptRule = new EncryptRule(new EncryptRuleConfigurationYamlSwapper().swap(ruleConfiguration.getEncryptRule()));
        boolean isQueryWithCipherColumn = (boolean) ruleConfiguration.getProps().get("query.with.cipher.column");
        new EncryptSQLRewriteContextDecorator(encryptRule, isQueryWithCipherColumn).decorate(sqlRewriteContext);
        sqlRewriteContext.generateSQLTokens();
        return Collections.singletonList(new DefaultSQLRewriteEngine().rewrite(sqlRewriteContext));
    }
    
    private SQLRewriteContext getSQLRewriteContext() {
        SQLStatement sqlStatement = SQLParseEngineFactory.getSQLParseEngine(null == databaseType ? "SQL92" : databaseType).parse(inputSQL, false);
        SQLStatementContext sqlStatementContext = SQLStatementContextFactory.newInstance(createRelationMetas(), inputSQL, inputParameters, sqlStatement);
        return new SQLRewriteContext(mock(TableMetas.class), sqlStatementContext, inputSQL, inputParameters);
    }
    
    private RelationMetas createRelationMetas() {
        RelationMetas result = mock(RelationMetas.class);
        when(result.getAllColumnNames("t_account")).thenReturn(Arrays.asList("account_id", "certificate_number", "password", "amount", "status"));
        when(result.getAllColumnNames("t_account_bak")).thenReturn(Arrays.asList("account_id", "certificate_number", "password", "amount", "status"));
        return result;
    }
    
    private YamlRootEncryptRuleConfiguration createRuleConfiguration() throws IOException {
        URL url = EncryptSQLRewriteEngineParameterizedTest.class.getClassLoader().getResource(ruleFile);
        Preconditions.checkNotNull(url, "Cannot found rewrite rule yaml configuration.");
        return YamlEngine.unmarshal(new File(url.getFile()), YamlRootEncryptRuleConfiguration.class);
    }
}
