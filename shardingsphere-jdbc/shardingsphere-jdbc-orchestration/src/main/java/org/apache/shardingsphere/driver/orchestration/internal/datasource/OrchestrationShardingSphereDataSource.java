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

package org.apache.shardingsphere.driver.orchestration.internal.datasource;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.shardingsphere.cluster.configuration.config.ClusterConfiguration;
import org.apache.shardingsphere.cluster.facade.ClusterFacade;
import org.apache.shardingsphere.cluster.facade.init.ClusterInitFacade;
import org.apache.shardingsphere.cluster.heartbeat.event.HeartbeatDetectNoticeEvent;
import org.apache.shardingsphere.control.panel.spi.FacadeConfiguration;
import org.apache.shardingsphere.control.panel.spi.engine.ControlPanelFacadeEngine;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.apache.shardingsphere.driver.jdbc.unsupported.AbstractUnsupportedOperationDataSource;
import org.apache.shardingsphere.driver.orchestration.internal.circuit.datasource.CircuitBreakerDataSource;
import org.apache.shardingsphere.driver.orchestration.internal.util.DataSourceConverter;
import org.apache.shardingsphere.infra.config.DataSourceConfiguration;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.infra.database.DefaultSchema;
import org.apache.shardingsphere.infra.metadata.ShardingSphereMetaData;
import org.apache.shardingsphere.infra.metadata.schema.RuleSchemaMetaData;
import org.apache.shardingsphere.infra.rule.ShardingSphereRule;
import org.apache.shardingsphere.infra.rule.StatusContainedRule;
import org.apache.shardingsphere.infra.rule.event.impl.DataSourceNameDisabledEvent;
import org.apache.shardingsphere.kernel.context.SchemaContext;
import org.apache.shardingsphere.kernel.context.SchemaContexts;
import org.apache.shardingsphere.kernel.context.schema.ShardingSphereSchema;
import org.apache.shardingsphere.masterslave.rule.MasterSlaveRule;
import org.apache.shardingsphere.orchestration.repository.api.config.OrchestrationConfiguration;
import org.apache.shardingsphere.orchestration.core.common.event.DataSourceChangedEvent;
import org.apache.shardingsphere.orchestration.core.common.event.PropertiesChangedEvent;
import org.apache.shardingsphere.orchestration.core.common.event.RuleConfigurationsChangedEvent;
import org.apache.shardingsphere.orchestration.core.common.eventbus.OrchestrationEventBus;
import org.apache.shardingsphere.orchestration.core.config.ConfigCenter;
import org.apache.shardingsphere.orchestration.core.facade.OrchestrationFacade;
import org.apache.shardingsphere.orchestration.core.metadata.event.MetaDataChangedEvent;
import org.apache.shardingsphere.orchestration.core.registry.event.CircuitStateChangedEvent;
import org.apache.shardingsphere.orchestration.core.registry.event.DisabledStateChangedEvent;
import org.apache.shardingsphere.orchestration.core.registry.schema.OrchestrationSchema;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Orchestration ShardingSphere data source.
 */
public final class OrchestrationShardingSphereDataSource extends AbstractUnsupportedOperationDataSource implements AutoCloseable {
    
    @Getter
    @Setter
    private PrintWriter logWriter = new PrintWriter(System.out);
    
    private final OrchestrationFacade orchestrationFacade = OrchestrationFacade.getInstance();
    
    private final Map<String, DataSourceConfiguration> dataSourceConfigurations = new LinkedHashMap<>();
    
    private ShardingSphereDataSource dataSource;
    
    public OrchestrationShardingSphereDataSource(final OrchestrationConfiguration orchestrationConfig) throws SQLException {
        init(orchestrationConfig);
        dataSource = loadDataSource();
        initWithOrchestrationCenter(null);
        disableDataSources();
        persistMetaData(dataSource.getSchemaContexts().getDefaultSchemaContext().getSchema().getMetaData().getSchema());
        initCluster();
    }
    
    public OrchestrationShardingSphereDataSource(final OrchestrationConfiguration orchestrationConfig, final ClusterConfiguration clusterConfiguration) throws SQLException {
        init(orchestrationConfig);
        dataSource = loadDataSource();
        initWithOrchestrationCenter(clusterConfiguration);
        disableDataSources();
        persistMetaData(dataSource.getSchemaContexts().getDefaultSchemaContext().getSchema().getMetaData().getSchema());
        initCluster();
    }
    
    public OrchestrationShardingSphereDataSource(final ShardingSphereDataSource shardingSphereDataSource, final OrchestrationConfiguration orchestrationConfig) {
        init(orchestrationConfig);
        dataSource = shardingSphereDataSource;
        initWithLocalConfiguration(null);
        disableDataSources();
        persistMetaData(dataSource.getSchemaContexts().getDefaultSchemaContext().getSchema().getMetaData().getSchema());
        initCluster();
    }
    
    public OrchestrationShardingSphereDataSource(final ShardingSphereDataSource shardingSphereDataSource, 
                                                 final OrchestrationConfiguration orchestrationConfig, final ClusterConfiguration clusterConfiguration) {
        init(orchestrationConfig);
        dataSource = shardingSphereDataSource;
        initWithLocalConfiguration(clusterConfiguration);
        disableDataSources();
        persistMetaData(dataSource.getSchemaContexts().getDefaultSchemaContext().getSchema().getMetaData().getSchema());
        initCluster();
    }
    
    private void init(final OrchestrationConfiguration config) {
        orchestrationFacade.init(config, Collections.singletonList(DefaultSchema.LOGIC_NAME));
        OrchestrationEventBus.getInstance().register(this);
    }
    
    private ShardingSphereDataSource loadDataSource() throws SQLException {
        ConfigCenter configService = orchestrationFacade.getConfigCenter();
        Collection<RuleConfiguration> configurations = configService.loadRuleConfigurations(DefaultSchema.LOGIC_NAME);
        Preconditions.checkState(!configurations.isEmpty(), "Missing the sharding rule configuration on registry center");
        Map<String, DataSourceConfiguration> dataSourceConfigurations = configService.loadDataSourceConfigurations(DefaultSchema.LOGIC_NAME);
        return new ShardingSphereDataSource(DataSourceConverter.getDataSourceMap(dataSourceConfigurations), configurations, configService.loadProperties());
    }
    
    private void initWithOrchestrationCenter(final ClusterConfiguration clusterConfiguration) {
        orchestrationFacade.onlineInstance();
        if (null != clusterConfiguration) {
            orchestrationFacade.initClusterConfiguration(clusterConfiguration);
        }
        dataSourceConfigurations.putAll(orchestrationFacade.getConfigCenter().loadDataSourceConfigurations(DefaultSchema.LOGIC_NAME));
    }
    
    private void initWithLocalConfiguration(final ClusterConfiguration clusterConfiguration) {
        Map<String, DataSourceConfiguration> dataSourceConfigurationMap = DataSourceConverter.getDataSourceConfigurationMap(dataSource.getDataSourceMap());
        Collection<RuleConfiguration> ruleConfigurations = dataSource.getSchemaContexts().getDefaultSchemaContext().getSchema().getConfigurations();
        Properties props = dataSource.getSchemaContexts().getProps().getProps();
        orchestrationFacade.onlineInstance(
                Collections.singletonMap(DefaultSchema.LOGIC_NAME, dataSourceConfigurationMap), Collections.singletonMap(DefaultSchema.LOGIC_NAME, ruleConfigurations), null, props);
        if (null != clusterConfiguration) {
            orchestrationFacade.initClusterConfiguration(clusterConfiguration);
        }
        this.dataSourceConfigurations.putAll(dataSourceConfigurationMap);
    }
    
    // TODO decouple masterslave rule
    private void disableDataSources() {
        Collection<String> disabledDataSources = OrchestrationFacade.getInstance().getRegistryCenter().loadDisabledDataSources();
        if (!disabledDataSources.isEmpty()) {
            dataSource.getSchemaContexts().getSchemaContexts().forEach((key, value)
                    -> value.getSchema().getRules().stream().filter(each -> each instanceof MasterSlaveRule).forEach(e -> disableDataSources((MasterSlaveRule) e, disabledDataSources, key)));
        }
    }
    
    private void disableDataSources(final MasterSlaveRule masterSlaveRule, final Collection<String> disabledDataSources, final String schemaName) {
        masterSlaveRule.getSingleDataSourceRule().getSlaveDataSourceNames().forEach(each -> {
            if (disabledDataSources.contains(Joiner.on(".").join(schemaName, each))) {
                masterSlaveRule.updateRuleStatus(new DataSourceNameDisabledEvent(each, true));
            }
        });
    }
    
    private void persistMetaData(final RuleSchemaMetaData ruleSchemaMetaData) {
        OrchestrationFacade.getInstance().getMetaDataCenter().persistMetaDataCenterNode(DefaultSchema.LOGIC_NAME, ruleSchemaMetaData);
    }
    
    private void initCluster() {
        ClusterConfiguration clusterConfiguration = orchestrationFacade.getConfigCenter().loadClusterConfiguration();
        if (null != clusterConfiguration && null != clusterConfiguration.getHeartbeat()) {
            List<FacadeConfiguration> facadeConfigurations = new LinkedList<>();
            facadeConfigurations.add(clusterConfiguration);
            new ControlPanelFacadeEngine().init(facadeConfigurations);
        }
    }
    
    /**
     * Renew meta data of the schema.
     *
     * @param event meta data changed event.
     */
    @Subscribe
    public synchronized void renew(final MetaDataChangedEvent event) {
        if (!event.getSchemaNames().contains(DefaultSchema.LOGIC_NAME)) {
            return;
        }
        Map<String, SchemaContext> schemaContexts = new HashMap<>(dataSource.getSchemaContexts().getSchemaContexts().size());
        SchemaContext oldSchemaContext = dataSource.getSchemaContexts().getSchemaContexts().get(DefaultSchema.LOGIC_NAME);
        schemaContexts.put(DefaultSchema.LOGIC_NAME, new SchemaContext(oldSchemaContext.getName(),
                getChangedSchema(oldSchemaContext.getSchema(), event.getRuleSchemaMetaData()), oldSchemaContext.getRuntimeContext()));
        dataSource = new ShardingSphereDataSource(new SchemaContexts(schemaContexts, dataSource.getSchemaContexts().getProps(), dataSource.getSchemaContexts().getAuthentication()));
    }
    
    /**
     * Renew sharding rule.
     *
     * @param ruleConfigurationsChangedEvent rule configurations changed event
     */
    @Subscribe
    @SneakyThrows
    public synchronized void renew(final RuleConfigurationsChangedEvent ruleConfigurationsChangedEvent) {
        if (!ruleConfigurationsChangedEvent.getShardingSchemaName().contains(DefaultSchema.LOGIC_NAME)) {
            return;
        }
        dataSource = new ShardingSphereDataSource(dataSource.getDataSourceMap(), 
                ruleConfigurationsChangedEvent.getRuleConfigurations(), dataSource.getSchemaContexts().getProps().getProps());
    }
    
    /**
     * Renew sharding data source.
     *
     * @param dataSourceChangedEvent data source changed event
     */
    @Subscribe
    @SneakyThrows
    public synchronized void renew(final DataSourceChangedEvent dataSourceChangedEvent) {
        if (!dataSourceChangedEvent.getShardingSchemaName().contains(DefaultSchema.LOGIC_NAME)) {
            return;
        }
        Map<String, DataSourceConfiguration> dataSourceConfigurations = dataSourceChangedEvent.getDataSourceConfigurations();
        dataSource.close(getDeletedDataSources(dataSourceConfigurations));
        dataSource.close(getModifiedDataSources(dataSourceConfigurations).keySet());
        dataSource = new ShardingSphereDataSource(getChangedDataSources(dataSource.getDataSourceMap(), dataSourceConfigurations), 
                dataSource.getSchemaContexts().getDefaultSchemaContext().getSchema().getConfigurations(), dataSource.getSchemaContexts().getProps().getProps());
        this.dataSourceConfigurations.clear();
        this.dataSourceConfigurations.putAll(dataSourceConfigurations);
    }
    
    /**
     * Renew properties.
     *
     * @param propertiesChangedEvent properties changed event
     */
    @SneakyThrows
    @Subscribe
    public synchronized void renew(final PropertiesChangedEvent propertiesChangedEvent) {
        dataSource = new ShardingSphereDataSource(dataSource.getDataSourceMap(), 
                dataSource.getSchemaContexts().getDefaultSchemaContext().getSchema().getConfigurations(), propertiesChangedEvent.getProps());
    }
    
    /**
     * Renew circuit breaker state.
     *
     * @param event circuit state changed event
     */
    @Subscribe
    public synchronized void renew(final CircuitStateChangedEvent event) {
        SchemaContexts oldSchemaContexts = dataSource.getSchemaContexts();
        SchemaContexts schemaContexts = new SchemaContexts(oldSchemaContexts.getSchemaContexts(), oldSchemaContexts.getProps(), oldSchemaContexts.getAuthentication(), event.isCircuitBreak());
        dataSource = new ShardingSphereDataSource(schemaContexts);
    }
    
    /**
     * Renew disabled data source names.
     *
     * @param disabledStateChangedEvent disabled state changed event
     */
    @Subscribe
    public synchronized void renew(final DisabledStateChangedEvent disabledStateChangedEvent) {
        OrchestrationSchema orchestrationSchema = disabledStateChangedEvent.getOrchestrationSchema();
        if (DefaultSchema.LOGIC_NAME.equals(orchestrationSchema.getSchemaName())) {
            for (ShardingSphereRule each : dataSource.getSchemaContexts().getDefaultSchemaContext().getSchema().getRules()) {
                if (each instanceof StatusContainedRule) {
                    ((StatusContainedRule) each).updateRuleStatus(new DataSourceNameDisabledEvent(orchestrationSchema.getDataSourceName(), disabledStateChangedEvent.isDisabled()));
                }
            }
        }
    }
    
    private synchronized Map<String, DataSource> getChangedDataSources(final Map<String, DataSource> oldDataSources, final Map<String, DataSourceConfiguration> newDataSources) {
        Map<String, DataSource> result = new LinkedHashMap<>(oldDataSources);
        Map<String, DataSourceConfiguration> modifiedDataSources = getModifiedDataSources(newDataSources);
        result.keySet().removeAll(getDeletedDataSources(newDataSources));
        result.keySet().removeAll(modifiedDataSources.keySet());
        result.putAll(DataSourceConverter.getDataSourceMap(modifiedDataSources));
        result.putAll(DataSourceConverter.getDataSourceMap(getAddedDataSources(newDataSources)));
        return result;
    }
    
    private synchronized Map<String, DataSourceConfiguration> getModifiedDataSources(final Map<String, DataSourceConfiguration> dataSourceConfigurations) {
        return dataSourceConfigurations.entrySet().stream().filter(this::isModifiedDataSource).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (key, repeatKey) -> key, LinkedHashMap::new));
    }
    
    private synchronized boolean isModifiedDataSource(final Entry<String, DataSourceConfiguration> dataSourceNameAndConfig) {
        return dataSourceConfigurations.containsKey(dataSourceNameAndConfig.getKey()) && !dataSourceConfigurations.get(dataSourceNameAndConfig.getKey()).equals(dataSourceNameAndConfig.getValue());
    }
    
    private synchronized List<String> getDeletedDataSources(final Map<String, DataSourceConfiguration> dataSourceConfigurations) {
        List<String> result = new LinkedList<>(this.dataSourceConfigurations.keySet());
        result.removeAll(dataSourceConfigurations.keySet());
        return result;
    }
    
    private synchronized Map<String, DataSourceConfiguration> getAddedDataSources(final Map<String, DataSourceConfiguration> dataSourceConfigurations) {
        return Maps.filterEntries(dataSourceConfigurations, input -> !this.dataSourceConfigurations.containsKey(input.getKey()));
    }
    
    private ShardingSphereSchema getChangedSchema(final ShardingSphereSchema oldSchema, final RuleSchemaMetaData newRuleSchemaMetaData) {
        ShardingSphereMetaData metaData = new ShardingSphereMetaData(oldSchema.getMetaData().getDataSources(), newRuleSchemaMetaData);
        return new ShardingSphereSchema(oldSchema.getDatabaseType(), oldSchema.getConfigurations(), oldSchema.getRules(), oldSchema.getDataSources(), metaData);
    }
    
    /**
     * Heart beat detect.
     *
     * @param event heart beat detect notice event
     */
    @Subscribe
    public synchronized void heartbeat(final HeartbeatDetectNoticeEvent event) {
        if (ClusterInitFacade.isEnabled()) {
            ClusterFacade.getInstance().detectHeartbeat(dataSource.getSchemaContexts().getSchemaContexts());
        }
    }
    
    @Override
    public Connection getConnection() {
        return dataSource.getSchemaContexts().isCircuitBreak() ? new CircuitBreakerDataSource().getConnection() : dataSource.getConnection();
    }
    
    @Override
    public Connection getConnection(final String username, final String password) {
        return getConnection();
    }
    
    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }
    
    @Override
    public void close() {
        dataSource.close();
        orchestrationFacade.close();
    }
}
