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

package org.apache.shardingsphere.test.integration.junit.container;

import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.test.integration.junit.annotation.XmlResource;
import org.apache.shardingsphere.test.integration.junit.processor.Processor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public abstract class ShardingContainer extends GenericContainer {
    
    private static final Pattern REGEX = Pattern.compile("\\{([\\w._-]*)\\}");
    
    @Getter
    @Setter
    private String dockerName;
    
    public ShardingContainer(final String dockerImageName) {
        super(DockerImageName.parse(dockerImageName));
    }
    
    @Override
    public void start() {
        annotationProcess(this.getClass());
        configure();
        startDependencies();
        super.start();
        execute();
    }
    
    private void startDependencies() {
        final List<ShardingContainer> dependencies = Lists.newArrayList(getDependencies());
        dependencies.stream()
                .filter(c -> !c.isCreated())
                .forEach(GenericContainer::start);
        dependencies.stream()
                .filter(c -> {
                    try {
                        return !c.isHealthy();
                        // CHECKSTYLE:OFF
                    } catch (Exception e) {
                        // CHECKSTYLE:ON
                        log.warn("Failed to check container {} healthy.", c.getDockerName(), e);
                        return false;
                    }
                })
                .forEach(c -> {
                    DockerHealthcheckWaitStrategy waitStrategy = new DockerHealthcheckWaitStrategy();
                    log.info("Waiting for container {} healthy.", c.getDockerImageName());
                    waitStrategy.withStartupTimeout(Duration.of(90, ChronoUnit.SECONDS));
                    waitStrategy.waitUntilReady(c);
                    log.info("Container {} is startup.", c.getDockerImageName());
                });
    }
    
    private void annotationProcess(Class<?> klass) {
        ArrayList<Field> fields = Lists.newArrayList(klass.getDeclaredFields());
        fields.stream()
                .filter(it -> it.isAnnotationPresent(XmlResource.class))
                .forEach(this::resourceInject);
        Class<?> parent = klass.getSuperclass();
        if (Objects.nonNull(parent)) {
            annotationProcess(parent);
        }
    }
    
    private void resourceInject(Field field) {
        XmlResource annotation = field.getAnnotation(XmlResource.class);
        String file = parse(annotation.file());
        Class<? extends Processor> processor = annotation.processor();
        try {
            String base = getClass().getResource("/").getFile();
            Object result = processor.newInstance().process(Files.newInputStream(new File(base, file).toPath(), StandardOpenOption.READ));
            field.setAccessible(true);
            field.set(this, result);
        } catch (InstantiationException | IllegalAccessException | IOException e) {
            e.printStackTrace(); // fixme
        }
    }
    
    private static String parse(final String pattern) {
        String result = pattern;
        Matcher matcher = REGEX.matcher(pattern);
        while (matcher.find()) {
            result = result.replace(matcher.group(), System.getProperty(matcher.group(1), "null"));
        }
        return result;
    }
    
    protected void configure() {
    
    }
    
    protected void execute() {
    
    }
}