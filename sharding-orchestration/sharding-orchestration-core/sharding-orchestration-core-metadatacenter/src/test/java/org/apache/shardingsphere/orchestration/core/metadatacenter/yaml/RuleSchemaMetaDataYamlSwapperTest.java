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

package org.apache.shardingsphere.orchestration.core.metadatacenter.yaml;

import org.apache.shardingsphere.orchestration.core.metadatacenter.MetaDataTest;
import org.apache.shardingsphere.underlying.common.metadata.schema.RuleSchemaMetaData;
import org.apache.shardingsphere.underlying.common.yaml.engine.YamlEngine;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public final class RuleSchemaMetaDataYamlSwapperTest {
    
    @Test
    public void assertSwapToYamlRuleSchemaMetaData() {
        RuleSchemaMetaData ruleSchemaMetaData = new RuleSchemaMetaDataYamlSwapper().swap(YamlEngine.unmarshal(MetaDataTest.META_DATA, YamlRuleSchemaMetaData.class));
        YamlRuleSchemaMetaData yamlRuleSchemaMetaData = new RuleSchemaMetaDataYamlSwapper().swap(ruleSchemaMetaData);
        assertNotNull(yamlRuleSchemaMetaData);
        assertNotNull(yamlRuleSchemaMetaData.getConfiguredSchemaMetaData());
    }
    
    @Test
    public void assertSwapToRuleSchemaMetaData() {
        YamlRuleSchemaMetaData yamlRuleSchemaMetaData = YamlEngine.unmarshal(MetaDataTest.META_DATA, YamlRuleSchemaMetaData.class);
        RuleSchemaMetaData ruleSchemaMetaData = new RuleSchemaMetaDataYamlSwapper().swap(yamlRuleSchemaMetaData);
        assertNotNull(ruleSchemaMetaData);
    }
}
