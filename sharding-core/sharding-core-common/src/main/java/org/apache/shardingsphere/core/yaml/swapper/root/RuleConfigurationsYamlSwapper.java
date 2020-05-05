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

package org.apache.shardingsphere.core.yaml.swapper.root;

import org.apache.shardingsphere.api.config.masterslave.MasterSlaveRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.core.yaml.config.masterslave.YamlMasterSlaveRuleConfiguration;
import org.apache.shardingsphere.core.yaml.config.sharding.YamlShardingRuleConfiguration;
import org.apache.shardingsphere.core.yaml.config.sharding.YamlTableRuleConfiguration;
import org.apache.shardingsphere.core.yaml.swapper.KeyGeneratorConfigurationYamlSwapper;
import org.apache.shardingsphere.core.yaml.swapper.MasterSlaveRuleConfigurationYamlSwapper;
import org.apache.shardingsphere.core.yaml.swapper.ShardingStrategyConfigurationYamlSwapper;
import org.apache.shardingsphere.core.yaml.swapper.TableRuleConfigurationYamlSwapper;
import org.apache.shardingsphere.encrypt.api.EncryptRuleConfiguration;
import org.apache.shardingsphere.encrypt.yaml.swapper.EncryptRuleConfigurationYamlSwapper;
import org.apache.shardingsphere.underlying.common.config.RuleConfiguration;
import org.apache.shardingsphere.underlying.common.yaml.swapper.YamlSwapper;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 * Rule configurations YAML swapper.
 * 
 * @deprecated instead of RuleRootConfigurationsYamlSwapper
 */
@Deprecated
public final class RuleConfigurationsYamlSwapper implements YamlSwapper<YamlShardingRuleConfiguration, Collection<RuleConfiguration>> {
    
    private final TableRuleConfigurationYamlSwapper tableRuleConfigurationYamlSwapper = new TableRuleConfigurationYamlSwapper();
    
    private final ShardingStrategyConfigurationYamlSwapper shardingStrategyConfigurationYamlSwapper = new ShardingStrategyConfigurationYamlSwapper();
    
    private final KeyGeneratorConfigurationYamlSwapper keyGeneratorConfigurationYamlSwapper = new KeyGeneratorConfigurationYamlSwapper();
    
    private final MasterSlaveRuleConfigurationYamlSwapper masterSlaveRuleConfigurationYamlSwapper = new MasterSlaveRuleConfigurationYamlSwapper();
    
    private final EncryptRuleConfigurationYamlSwapper encryptRuleConfigurationYamlSwapper = new EncryptRuleConfigurationYamlSwapper();
    
    @Override
    public YamlShardingRuleConfiguration swap(final Collection<RuleConfiguration> data) {
        YamlShardingRuleConfiguration result = new YamlShardingRuleConfiguration();
        for (RuleConfiguration each : data) {
            if (each instanceof ShardingRuleConfiguration) {
                YamlShardingRuleConfiguration configuration = swap((ShardingRuleConfiguration) each);
                result.setTables(configuration.getTables());
                result.setBindingTables(configuration.getBindingTables());
                result.setBroadcastTables(configuration.getBroadcastTables());
                result.setDefaultDatabaseStrategy(configuration.getDefaultDatabaseStrategy());
                result.setDefaultTableStrategy(configuration.getDefaultTableStrategy());
                result.setDefaultKeyGenerator(configuration.getDefaultKeyGenerator());
            } else if (each instanceof MasterSlaveRuleConfiguration) {
                result.getMasterSlaveRules().put(((MasterSlaveRuleConfiguration) each).getName(), masterSlaveRuleConfigurationYamlSwapper.swap((MasterSlaveRuleConfiguration) each));
            } else if (each instanceof EncryptRuleConfiguration && !((EncryptRuleConfiguration) each).getTables().isEmpty()) {
                result.setEncryptRule(encryptRuleConfigurationYamlSwapper.swap((EncryptRuleConfiguration) each));
            }
        }
        return result;
    }
    
    private YamlShardingRuleConfiguration swap(final ShardingRuleConfiguration data) {
        YamlShardingRuleConfiguration result = new YamlShardingRuleConfiguration();
        for (TableRuleConfiguration each : data.getTableRuleConfigs()) {
            result.getTables().put(each.getLogicTable(), tableRuleConfigurationYamlSwapper.swap(each));
        }
        result.getBindingTables().addAll(data.getBindingTableGroups());
        result.getBroadcastTables().addAll(data.getBroadcastTables());
        if (null != data.getDefaultDatabaseShardingStrategyConfig()) {
            result.setDefaultDatabaseStrategy(shardingStrategyConfigurationYamlSwapper.swap(data.getDefaultDatabaseShardingStrategyConfig()));
        }
        if (null != data.getDefaultTableShardingStrategyConfig()) {
            result.setDefaultTableStrategy(shardingStrategyConfigurationYamlSwapper.swap(data.getDefaultTableShardingStrategyConfig()));
        }
        if (null != data.getDefaultKeyGeneratorConfig()) {
            result.setDefaultKeyGenerator(keyGeneratorConfigurationYamlSwapper.swap(data.getDefaultKeyGeneratorConfig()));
        }
        return result;
    }
    
    @Override
    public Collection<RuleConfiguration> swap(final YamlShardingRuleConfiguration yamlConfiguration) {
        Collection<RuleConfiguration> result = new LinkedList<>();
        result.add(createShardingRuleConfiguration(yamlConfiguration));
        for (Entry<String, YamlMasterSlaveRuleConfiguration> entry : yamlConfiguration.getMasterSlaveRules().entrySet()) {
            YamlMasterSlaveRuleConfiguration each = entry.getValue();
            each.setName(entry.getKey());
            result.add(masterSlaveRuleConfigurationYamlSwapper.swap(entry.getValue()));
        }
        if (null != yamlConfiguration.getEncryptRule() && !yamlConfiguration.getEncryptRule().getTables().isEmpty()) {
            result.add(encryptRuleConfigurationYamlSwapper.swap(yamlConfiguration.getEncryptRule()));
        }
        return result;
    }
    
    private ShardingRuleConfiguration createShardingRuleConfiguration(final YamlShardingRuleConfiguration yamlConfiguration) {
        ShardingRuleConfiguration result = new ShardingRuleConfiguration();
        for (Entry<String, YamlTableRuleConfiguration> entry : yamlConfiguration.getTables().entrySet()) {
            YamlTableRuleConfiguration tableRuleConfig = entry.getValue();
            tableRuleConfig.setLogicTable(entry.getKey());
            result.getTableRuleConfigs().add(tableRuleConfigurationYamlSwapper.swap(tableRuleConfig));
        }
        result.getBindingTableGroups().addAll(yamlConfiguration.getBindingTables());
        result.getBroadcastTables().addAll(yamlConfiguration.getBroadcastTables());
        if (null != yamlConfiguration.getDefaultDatabaseStrategy()) {
            result.setDefaultDatabaseShardingStrategyConfig(shardingStrategyConfigurationYamlSwapper.swap(yamlConfiguration.getDefaultDatabaseStrategy()));
        }
        if (null != yamlConfiguration.getDefaultTableStrategy()) {
            result.setDefaultTableShardingStrategyConfig(shardingStrategyConfigurationYamlSwapper.swap(yamlConfiguration.getDefaultTableStrategy()));
        }
        if (null != yamlConfiguration.getDefaultKeyGenerator()) {
            result.setDefaultKeyGeneratorConfig(keyGeneratorConfigurationYamlSwapper.swap(yamlConfiguration.getDefaultKeyGenerator()));
        }
        return result;
    }
}
