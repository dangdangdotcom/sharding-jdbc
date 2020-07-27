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

package org.apache.shardingsphere.orchestration.repository.apollo;

import com.ctrip.framework.apollo.mockserver.EmbeddedApollo;
import com.google.common.util.concurrent.SettableFuture;
import lombok.SneakyThrows;
import org.apache.shardingsphere.orchestration.repository.api.ConfigurationRepository;
import org.apache.shardingsphere.orchestration.repository.apollo.wrapper.ApolloConfigWrapper;
import org.apache.shardingsphere.orchestration.repository.apollo.wrapper.ApolloOpenApiWrapper;
import org.apache.shardingsphere.orchestration.repository.api.listener.DataChangedEvent;
import org.apache.shardingsphere.orchestration.repository.api.util.ConfigKeyUtils;
import org.apache.shardingsphere.orchestration.repository.api.config.OrchestrationCenterConfiguration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.internal.util.reflection.FieldSetter;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public final class ApolloRepositoryTest {
    
    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
    
    @ClassRule
    public static EmbeddedApollo embeddedApollo = new EmbeddedApollo();
    
    private static final ConfigurationRepository REPOSITORY = new ApolloRepository();
    
    private static final ApolloOpenApiWrapper OPEN_API_WRAPPER = mock(ApolloOpenApiWrapper.class);
    
    private static final String PORTAL_URL = "http://127.0.0.1";
    
    private static final String TOKEN = "testToken";
    
    @SneakyThrows(ReflectiveOperationException.class)
    @BeforeClass
    public static void init() {
        
        Properties props = new Properties();
        props.setProperty(ApolloPropertyKey.PORTAL_URL.getKey(), PORTAL_URL);
        props.setProperty(ApolloPropertyKey.TOKEN.getKey(), TOKEN);
        OrchestrationCenterConfiguration config = new OrchestrationCenterConfiguration("apollo", "http://config-service-url", props);
        REPOSITORY.setProps(props);
        REPOSITORY.init("orchestration", config);
        ApolloConfigWrapper configWrapper = new ApolloConfigWrapper("orchestration", config, new ApolloProperties(props));
        FieldSetter.setField(REPOSITORY, ApolloRepository.class.getDeclaredField("configWrapper"), configWrapper);
        FieldSetter.setField(REPOSITORY, ApolloRepository.class.getDeclaredField("openApiWrapper"), OPEN_API_WRAPPER);
    }
    
    @Test
    public void assertGet() {
        assertThat(REPOSITORY.get("/test/children/0"), is("value0"));
    }
    
    @Test
    public void assertGetByOpenApi() {
        assertNull(REPOSITORY.get("/test/children/6"));
    }
    
    @Test
    public void assertWatch() throws InterruptedException, ExecutionException, TimeoutException {
        assertWatchUpdateChangedType("/test/children/1", "newValue");
    }
    
    @Test
    public void assertGetWithNonExistentKey() {
        assertNull(REPOSITORY.get("/test/nonExistentKey"));
    }
    
    @Test
    public void assertWatchUpdateChangedTypeWithExistedKey() throws InterruptedException, ExecutionException, TimeoutException {
        assertWatchUpdateChangedType("/test/children/4", "newValue4");
        assertThat(REPOSITORY.get("/test/children/4"), is("newValue4"));
    }
    
    @Test
    public void assertWatchDeletedChangedTypeWithExistedKey() throws InterruptedException, ExecutionException, TimeoutException {
        assertWatchDeletedChangedType("/test/children/3");
    }
    
    private void assertWatchDeletedChangedType(final String key) throws InterruptedException, ExecutionException, TimeoutException {
        final SettableFuture<DataChangedEvent> future = SettableFuture.create();
        REPOSITORY.watch(key, future::set);
        embeddedApollo.deleteProperty("orchestration", ConfigKeyUtils.pathToKey(key));
        DataChangedEvent changeEvent = future.get(5, TimeUnit.SECONDS);
        assertThat(changeEvent.getKey(), is(key));
        assertNull(changeEvent.getValue());
        assertThat(changeEvent.getChangedType(), is(DataChangedEvent.ChangedType.DELETED));
        assertNull(REPOSITORY.get(key));
    }
    
    @Test
    public void assertWatchUpdateChangedTypeWithNotExistedKey() throws InterruptedException, ExecutionException, TimeoutException {
        assertWatchUpdateChangedType("/test/children/newKey", "newValue");
    }
    
    private void assertWatchUpdateChangedType(final String key, final String newValue) throws InterruptedException, ExecutionException, TimeoutException {
        final SettableFuture<DataChangedEvent> future = SettableFuture.create();
        REPOSITORY.watch(key, future::set);
        embeddedApollo.addOrModifyProperty("orchestration", ConfigKeyUtils.pathToKey(key), newValue);
        DataChangedEvent changeEvent = future.get(5, TimeUnit.SECONDS);
        assertThat(changeEvent.getKey(), is(key));
        assertThat(changeEvent.getValue(), is(newValue));
        assertThat(changeEvent.getChangedType(), is(DataChangedEvent.ChangedType.UPDATED));
    }
    
    @Test
    public void assertDelete() {
        REPOSITORY.delete("/test/children/2");
        verify(OPEN_API_WRAPPER).remove(ConfigKeyUtils.pathToKey("/test/children/2"));
    }
    
    @Test
    public void assertGetChildrenKeys() {
        assertNull(REPOSITORY.getChildrenKeys("/test/children"));
    }
    
    @Test
    public void assertPersist() {
        REPOSITORY.persist("/test/children/6", "value6");
        verify(OPEN_API_WRAPPER).persist(ConfigKeyUtils.pathToKey("/test/children/6"), "value6");
    }
    
    @Test
    public void assertGetType() {
        assertThat(REPOSITORY.getType(), is("apollo"));
    }
}
