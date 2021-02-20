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

package org.apache.shardingsphere.test.integration.junit.compose;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.test.integration.junit.TestingEnvProps;
import org.apache.shardingsphere.test.integration.junit.annotation.Conditional;
import org.apache.shardingsphere.test.integration.junit.annotation.ContainerInitializer;
import org.apache.shardingsphere.test.integration.junit.annotation.OnContainer;
import org.apache.shardingsphere.test.integration.junit.condition.Condition;
import org.apache.shardingsphere.test.integration.junit.container.MySQLContainer;
import org.apache.shardingsphere.test.integration.junit.container.ShardingContainer;
import org.apache.shardingsphere.test.integration.junit.container.StorageContainer;
import org.apache.shardingsphere.test.integration.junit.logging.ContainerLogs;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startable;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class ContainerCompose implements Closeable {
    
    private final Network network = Network.newNetwork();
    
    private final String clusterName;
    
    private final TestClass testClass;
    
    private ImmutableList<ShardingContainer> containers;
    
    /**
     * Startup all containers.
     */
    @SneakyThrows
    public void startup() {
        createContainers();
        createInitializerAndExecute();
        containers.stream().filter(c -> !c.isCreated()).forEach(GenericContainer::start);
    }
    
    private void createContainers() {
        final Class<?> klass = testClass.getJavaClass();
        final List<ShardingContainer> containerList = testClass.getAnnotatedFields(OnContainer.class).stream()
                .map(field -> {
                    final OnContainer metadata = field.getAnnotation(OnContainer.class);
                    try {
                        final ShardingContainer container = createContainer(field, metadata);
                        if (Objects.isNull(container)) {
                            log.warn("container {} is not activated.", metadata.name());
                            return null;
                        }
                        log.info("container {} is activated.", metadata.name());
                        
                        container.setDockerName(metadata.name());
                        String hostName = metadata.hostName();
                        if (Strings.isNullOrEmpty(hostName)) {
                            hostName = metadata.name();
                        }
                        container.withNetworkAliases(hostName);
                        container.setNetwork(network);
                        container.withLogConsumer(ContainerLogs.newConsumer(clusterName + "_" + metadata.name()));
                        
                        field.getField().setAccessible(true);
                        field.getField().set(klass, container);
                        
                        return container;
                        // CHECKSTYLE:OFF
                    } catch (Exception e) {
                        // CHECKSTYLE:ON
                        log.error("Failed to instantiate container {}.", metadata.name(), e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        containers = new ImmutableList.Builder<ShardingContainer>().addAll(containerList).build();
    }
    
    @SneakyThrows
    private ShardingContainer createContainer(final FrameworkField field, final OnContainer metadata) {
        final boolean matches = Arrays.stream(field.getAnnotations())
                .map(this::matches)
                .reduce((a, b) -> a && b)
                .orElse(true);
        if (matches) {
            switch (metadata.type()) {
                case PROXY:
                    return (ShardingContainer) field.getType().newInstance();
                case STORAGE:
                    return createStorageContainer();
                case COORDINATOR:
                    throw new NotSupportedException(); // FIXME
                default:
                    return null;
            }
        }
        return null;
    }
    
    @SneakyThrows
    private boolean matches(final Annotation annotation) {
        if (annotation.annotationType().isAnnotationPresent(Conditional.class)) {
            Conditional conditional = annotation.annotationType().getAnnotation(Conditional.class);
            try {
                Condition condition = conditional.value().newInstance();
                return condition.matches(annotation);
                // CHECKSTYLE:OFF
            } catch (Exception e) {
                // CHECKSTYLE:ON
                throw new Exception("Failed to instantiate conditional " + conditional.value() + ".");
            }
        }
        return true;
    }
    
    private StorageContainer createStorageContainer() {
        switch (TestingEnvProps.getStorageType()) {
            case MySQL:
                return new MySQLContainer();
            case H2:
                throw new RuntimeException("Not yet support storage type " + TestingEnvProps.getStorageType());
            default:
                throw new RuntimeException("Unknown storage type " + TestingEnvProps.getStorageType());
        }
    }
    
    @SneakyThrows
    private void createInitializerAndExecute() {
        List<FrameworkMethod> methods = testClass.getAnnotatedMethods(ContainerInitializer.class).stream()
                .filter(e -> Arrays.stream(e.getAnnotations()).map(this::matches).reduce((a, b) -> a && b).orElse(true))
                .collect(Collectors.toList());
        if (methods.size() > 1) {
            throw new RuntimeException("Only support to have one or zero initializer.");
        }
        if (!methods.isEmpty()) {
            FrameworkMethod method = methods.get(0);
            if (method.isStatic()) {
                method.invokeExplosively(testClass.getJavaClass());
            } else {
                throw new Exception("Method " + method.getName() + " is not a static method.");
            }
        }
    }
    
    /**
     * Wait until all containers ready.
     */
    public void waitUntilReady() {
        containers.stream()
                .filter(c -> {
                    try {
                        return !c.isHealthy();
                        // CHECKSTYLE:OFF
                    } catch (Exception e) {
                        // CHECKSTYLE:ON
                        return false;
                    }
                })
                .forEach(c -> {
                    while (!(c.isRunning() && c.isHealthy())) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(200L);
                        } catch (InterruptedException ignore) {
                        
                        }
                    }
                });
        log.info("Any container is startup.");
    }
    
    @Override
    public void close() {
        containers.forEach(Startable::close);
    }
    
}