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

package org.apache.shardingsphere.primaryreplica.yaml.swapper;

import com.google.common.collect.ImmutableMap;
import org.apache.shardingsphere.infra.config.algorithm.ShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.infra.yaml.swapper.YamlRuleConfigurationSwapper;
import org.apache.shardingsphere.primaryreplica.api.config.PrimaryReplicaRuleConfiguration;
import org.apache.shardingsphere.primaryreplica.api.config.rule.PrimaryReplicaDataSourceRuleConfiguration;
import org.apache.shardingsphere.primaryreplica.constant.PrimaryReplicaOrder;
import org.apache.shardingsphere.primaryreplica.yaml.config.YamlPrimaryReplicaRuleConfiguration;
import org.apache.shardingsphere.primaryreplica.yaml.config.rule.YamlPrimaryReplicaDataSourceRuleConfiguration;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class PrimaryReplicaRuleConfigurationYamlSwapperTest {
    
    private final Collection<YamlRuleConfigurationSwapper> collection = ShardingSphereServiceLoader.newServiceInstances(YamlRuleConfigurationSwapper.class);
    
    static {
        ShardingSphereServiceLoader.register(YamlRuleConfigurationSwapper.class);
    }
    
    @Test
    public void assertSwapToYamlWithLoadBalanceAlgorithm() {
        PrimaryReplicaDataSourceRuleConfiguration dataSourceConfiguration = new PrimaryReplicaDataSourceRuleConfiguration("ds", "primary", Collections.singletonList("replica"), "roundRobin");
        YamlPrimaryReplicaRuleConfiguration actual = getPrimaryReplicaRuleConfigurationYamlSwapper().swapToYamlConfiguration(new PrimaryReplicaRuleConfiguration(
                Collections.singleton(dataSourceConfiguration), ImmutableMap.of("roundRobin", new ShardingSphereAlgorithmConfiguration("ROUND_ROBIN", new Properties()))));
        assertThat(actual.getDataSources().get("ds").getName(), is("ds"));
        assertThat(actual.getDataSources().get("ds").getPrimaryDataSourceName(), is("primary"));
        assertThat(actual.getDataSources().get("ds").getReplicaDataSourceNames(), is(Collections.singletonList("replica")));
        assertThat(actual.getDataSources().get("ds").getLoadBalancerName(), is("roundRobin"));
    }
    
    @Test
    public void assertSwapToYamlWithoutLoadBalanceAlgorithm() {
        PrimaryReplicaDataSourceRuleConfiguration dataSourceConfiguration = new PrimaryReplicaDataSourceRuleConfiguration("ds", "primary", Collections.singletonList("replica"), null);
        YamlPrimaryReplicaRuleConfiguration actual = getPrimaryReplicaRuleConfigurationYamlSwapper().swapToYamlConfiguration(
                new PrimaryReplicaRuleConfiguration(Collections.singleton(dataSourceConfiguration), Collections.emptyMap()));
        assertThat(actual.getDataSources().get("ds").getName(), is("ds"));
        assertThat(actual.getDataSources().get("ds").getPrimaryDataSourceName(), is("primary"));
        assertThat(actual.getDataSources().get("ds").getReplicaDataSourceNames(), is(Collections.singletonList("replica")));
        assertNull(actual.getDataSources().get("ds").getLoadBalancerName());
    }
    
    @Test
    public void assertSwapToObjectWithLoadBalanceAlgorithmType() {
        YamlPrimaryReplicaRuleConfiguration yamlConfiguration = createYamlPrimaryReplicaRuleConfiguration();
        yamlConfiguration.getDataSources().get("primary_replica_ds").setLoadBalancerName("RANDOM");
        PrimaryReplicaRuleConfiguration actual = getPrimaryReplicaRuleConfigurationYamlSwapper().swapToObject(yamlConfiguration);
        assertPrimaryReplicaRuleConfiguration(actual);
        assertThat(actual.getDataSources().iterator().next().getLoadBalancerName(), is("RANDOM"));
    }
    
    @Test
    public void assertSwapToObjectWithoutLoadBalanceAlgorithm() {
        YamlPrimaryReplicaRuleConfiguration yamlConfiguration = createYamlPrimaryReplicaRuleConfiguration();
        PrimaryReplicaRuleConfiguration actual = getPrimaryReplicaRuleConfigurationYamlSwapper().swapToObject(yamlConfiguration);
        assertPrimaryReplicaRuleConfiguration(actual);
        assertNull(actual.getDataSources().iterator().next().getLoadBalancerName());
    }
    
    private YamlPrimaryReplicaRuleConfiguration createYamlPrimaryReplicaRuleConfiguration() {
        YamlPrimaryReplicaRuleConfiguration result = new YamlPrimaryReplicaRuleConfiguration();
        result.getDataSources().put("primary_replica_ds", new YamlPrimaryReplicaDataSourceRuleConfiguration());
        result.getDataSources().get("primary_replica_ds").setName("primary_replica_ds");
        result.getDataSources().get("primary_replica_ds").setPrimaryDataSourceName("primary_ds");
        result.getDataSources().get("primary_replica_ds").setReplicaDataSourceNames(Arrays.asList("replica_ds_0", "replica_ds_1"));
        return result;
    }
    
    private void assertPrimaryReplicaRuleConfiguration(final PrimaryReplicaRuleConfiguration actual) {
        PrimaryReplicaDataSourceRuleConfiguration group = actual.getDataSources().iterator().next();
        assertThat(group.getName(), is("primary_replica_ds"));
        assertThat(group.getPrimaryDataSourceName(), is("primary_ds"));
        assertThat(group.getReplicaDataSourceNames(), is(Arrays.asList("replica_ds_0", "replica_ds_1")));
    }
    
    @Test
    public void assertGetTypeClass() {
        PrimaryReplicaRuleConfigurationYamlSwapper primaryReplicaRuleConfigurationYamlSwapper = getPrimaryReplicaRuleConfigurationYamlSwapper();
        Class<PrimaryReplicaRuleConfiguration> actual = primaryReplicaRuleConfigurationYamlSwapper.getTypeClass();
        assertTrue(actual.isAssignableFrom(PrimaryReplicaRuleConfiguration.class));
    }
    
    @Test
    public void assertGetOrder() {
        PrimaryReplicaRuleConfigurationYamlSwapper primaryReplicaRuleConfigurationYamlSwapper = getPrimaryReplicaRuleConfigurationYamlSwapper();
        int actual = primaryReplicaRuleConfigurationYamlSwapper.getOrder();
        assertThat(actual, is(PrimaryReplicaOrder.ORDER));
    }
    
    private PrimaryReplicaRuleConfigurationYamlSwapper getPrimaryReplicaRuleConfigurationYamlSwapper() {
        Optional<PrimaryReplicaRuleConfigurationYamlSwapper> optional = collection.stream()
                .filter(swapper -> swapper instanceof PrimaryReplicaRuleConfigurationYamlSwapper)
                .map(swapper -> (PrimaryReplicaRuleConfigurationYamlSwapper) swapper)
                .findFirst();
        assertTrue(optional.isPresent());
        return optional.get();
    }
}
