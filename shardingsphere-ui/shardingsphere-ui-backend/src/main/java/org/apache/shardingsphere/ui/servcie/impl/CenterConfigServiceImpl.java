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

package org.apache.shardingsphere.ui.servcie.impl;

import org.apache.shardingsphere.ui.common.domain.CenterConfig;
import org.apache.shardingsphere.ui.common.domain.CenterConfigs;
import org.apache.shardingsphere.ui.common.exception.ShardingSphereUIException;
import org.apache.shardingsphere.ui.repository.CenterConfigsRepository;
import org.apache.shardingsphere.ui.servcie.CenterConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of Center config service.
 */
@Service
public final class CenterConfigServiceImpl implements CenterConfigService {
    
    @Autowired
    private CenterConfigsRepository centerConfigsRepository;
    
    @Override
    public CenterConfig load(final String name, final String orchestrationType) {
        return find(name, orchestrationType, loadAll());
    }
    
    @Override
    public Optional<CenterConfig> loadActivated(String orchestrationType) {
        return Optional.ofNullable(findActivatedCenterConfiguration(loadAll(orchestrationType)));
    }
    
    @Override
    public void add(final CenterConfig config) {
        CenterConfigs configs = loadAll();
        CenterConfig existedConfig = find(config.getName(), config.getOrchestrationType(), configs);
        if (null != existedConfig) {
            throw new ShardingSphereUIException(ShardingSphereUIException.SERVER_ERROR, "Center already existed!");
        }
        configs.getCenterConfigs().add(config);
        centerConfigsRepository.save(configs);
    }
    
    @Override
    public void delete(final String name, final String orchestrationType) {
        CenterConfigs configs = loadAll();
        CenterConfig toBeRemovedConfig = find(name, orchestrationType, configs);
        if (null != toBeRemovedConfig) {
            configs.getCenterConfigs().remove(toBeRemovedConfig);
            centerConfigsRepository.save(configs);
        }
    }
    
    @Override
    public void setActivated(final String name, final String orchestrationType) {
        CenterConfigs configs = loadAll();
        CenterConfig config = find(name, orchestrationType, configs);
        if (null == config) {
            throw new ShardingSphereUIException(ShardingSphereUIException.SERVER_ERROR, "Center not existed!");
        }
        CenterConfig activatedConfig = findActivatedCenterConfiguration(configs);
        if (!config.equals(activatedConfig)) {
            if (null != activatedConfig) {
                activatedConfig.setActivated(false);
            }
            config.setActivated(true);
            centerConfigsRepository.save(configs);
        }
    }
    
    @Override
    public CenterConfigs loadAll() {
        return centerConfigsRepository.load();
    }
    
    @Override
    public CenterConfigs loadAll(String orchestrationType) {
        CenterConfigs result = new CenterConfigs();
        List<CenterConfig> centerConfigs = new ArrayList<>();
        centerConfigsRepository.load().getCenterConfigs().stream()
                .filter(each->orchestrationType.equals(each.getOrchestrationType()))
                .forEach(each->centerConfigs.add(each));
        result.setCenterConfigs(centerConfigs);
        return result;
    }
    
    private CenterConfig findActivatedCenterConfiguration(final CenterConfigs centerConfigs) {
        return centerConfigs.getCenterConfigs().stream()
                .filter(each->each.isActivated())
                .findAny()
                .orElse(null);
    }
    
    private CenterConfig find(final String name, final String orchestrationType, final CenterConfigs configs) {
        return configs.getCenterConfigs().stream().filter(each->name.equals(each.getName()) && orchestrationType.equals(each.getOrchestrationType()))
                .findAny()
                .orElse(null);
    }
}
