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

package org.apache.shardingsphere.proxy.backend.text.distsql.rdl.impl;

import org.apache.shardingsphere.sharding.distsql.parser.segment.BindingTableRuleSegment;
import org.apache.shardingsphere.sharding.distsql.parser.statement.CreateShardingBindingTableRulesStatement;
import org.apache.shardingsphere.infra.context.metadata.MetaDataContexts;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.exception.DuplicateBindingTablesException;
import org.apache.shardingsphere.proxy.backend.exception.ShardingTableRuleNotExistedException;
import org.apache.shardingsphere.proxy.backend.response.header.ResponseHeader;
import org.apache.shardingsphere.proxy.backend.response.header.update.UpdateResponseHeader;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.transaction.context.TransactionContexts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class CreateShardingBindingTableRulesBackendHandlerTest {
    
    @Mock
    private BackendConnection backendConnection;
    
    @Mock
    private MetaDataContexts metaDataContexts;
    
    @Mock
    private TransactionContexts transactionContexts;
    
    @Mock
    private ShardingSphereMetaData shardingSphereMetaData;
    
    @Mock
    private ShardingSphereRuleMetaData shardingSphereRuleMetaData;
    
    @Before
    public void setUp() throws Exception {
        ProxyContext.getInstance().init(metaDataContexts, transactionContexts);
        when(metaDataContexts.getAllSchemaNames()).thenReturn(Collections.singleton("test"));
        when(metaDataContexts.getMetaData(eq("test"))).thenReturn(shardingSphereMetaData);
        when(shardingSphereMetaData.getRuleMetaData()).thenReturn(shardingSphereRuleMetaData);
    }
    
    @Test
    public void assertExecute() {
        when(shardingSphereRuleMetaData.getConfigurations()).thenReturn(Collections.singleton(buildShardingRuleConfiguration()));
        CreateShardingBindingTableRulesStatement statement = buildShardingTableRuleStatement();
        CreateShardingBindingTableRulesBackendHandler handler = new CreateShardingBindingTableRulesBackendHandler(statement, backendConnection);
        ResponseHeader responseHeader = handler.execute("test", statement);
        assertNotNull(responseHeader);
        assertTrue(responseHeader instanceof UpdateResponseHeader);
    }
    
    @Test(expected = ShardingTableRuleNotExistedException.class)
    public void assertExecuteWithNotExistTableRule() {
        when(shardingSphereRuleMetaData.getConfigurations()).thenReturn(Collections.singleton(new ShardingRuleConfiguration()));
        CreateShardingBindingTableRulesStatement statement = buildShardingTableRuleStatement();
        CreateShardingBindingTableRulesBackendHandler handler = new CreateShardingBindingTableRulesBackendHandler(statement, backendConnection);
        ResponseHeader responseHeader = handler.execute("test", statement);
        assertNotNull(responseHeader);
        assertTrue(responseHeader instanceof UpdateResponseHeader);
    }
    
    @Test(expected = DuplicateBindingTablesException.class)
    public void assertExecuteWithDuplicateTablesInSQL() {
        when(shardingSphereRuleMetaData.getConfigurations()).thenReturn(Collections.singleton(buildShardingRuleConfiguration()));
        CreateShardingBindingTableRulesStatement statement = buildDuplicateShardingTableRuleStatement();
        CreateShardingBindingTableRulesBackendHandler handler = new CreateShardingBindingTableRulesBackendHandler(statement, backendConnection);
        handler.execute("test", statement);
    }
    
    @Test(expected = DuplicateBindingTablesException.class)
    public void assertExecuteWithDuplicateTablesInShardingRule() {
        when(shardingSphereRuleMetaData.getConfigurations()).thenReturn(Collections.singleton(buildShardingBindingTableRuleConfiguration()));
        CreateShardingBindingTableRulesStatement statement = buildShardingTableRuleStatement();
        CreateShardingBindingTableRulesBackendHandler handler = new CreateShardingBindingTableRulesBackendHandler(statement, backendConnection);
        handler.execute("test", statement);
    }
    
    private ShardingRuleConfiguration buildShardingRuleConfiguration() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("t_order"));
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("t_order_item"));
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("t_1"));
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("t_2"));
        return shardingRuleConfig;
    }
    
    private ShardingRuleConfiguration buildShardingBindingTableRuleConfiguration() {
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("t_order"));
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("t_order_item"));
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("t_1"));
        shardingRuleConfig.getTables().add(new ShardingTableRuleConfiguration("t_2"));
        shardingRuleConfig.getBindingTableGroups().add("t_order,t_order_item");
        return shardingRuleConfig;
    }
    
    private CreateShardingBindingTableRulesStatement buildShardingTableRuleStatement() {
        return new CreateShardingBindingTableRulesStatement(Arrays.asList(new BindingTableRuleSegment("t_order,t_order_item"), new BindingTableRuleSegment("t_1,t_2")));
    }
    
    private CreateShardingBindingTableRulesStatement buildDuplicateShardingTableRuleStatement() {
        return new CreateShardingBindingTableRulesStatement(Arrays.asList(new BindingTableRuleSegment("t_order,t_order_item"), new BindingTableRuleSegment("t_order,t_order_item")));
    }
}
