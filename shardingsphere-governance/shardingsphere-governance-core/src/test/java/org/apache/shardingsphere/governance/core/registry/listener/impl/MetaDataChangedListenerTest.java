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

package org.apache.shardingsphere.governance.core.registry.listener.impl;

import org.apache.shardingsphere.governance.core.registry.listener.event.GovernanceEvent;
import org.apache.shardingsphere.governance.repository.api.listener.DataChangedEvent;
import org.apache.shardingsphere.governance.repository.api.listener.DataChangedEvent.Type;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
public final class MetaDataChangedListenerTest extends GovernanceListenerTest {
    
    private MetaDataChangedListener metaDataChangedListener;
    
    @Before
    public void setUp() {
        metaDataChangedListener = new MetaDataChangedListener(getRegistryCenterRepository());
    }
    
    @Test
    public void assertCreateEventWithInvalidPath() {
        DataChangedEvent dataChangedEvent = new DataChangedEvent("/metadata_invalid/sharding_db", "encrypt_db", Type.UPDATED);
        Optional<GovernanceEvent> actual = metaDataChangedListener.createEvent(dataChangedEvent);
        assertFalse(actual.isPresent());
    }
}
