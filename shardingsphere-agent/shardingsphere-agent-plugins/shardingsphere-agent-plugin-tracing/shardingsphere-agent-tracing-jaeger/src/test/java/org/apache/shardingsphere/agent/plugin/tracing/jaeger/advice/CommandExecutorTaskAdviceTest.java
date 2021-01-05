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

package org.apache.shardingsphere.agent.plugin.tracing.jaeger.advice;

import io.netty.channel.ChannelHandlerContext;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import lombok.SneakyThrows;
import org.apache.shardingsphere.agent.api.result.MethodInvocationResult;
import org.apache.shardingsphere.db.protocol.payload.PacketPayload;
import org.apache.shardingsphere.proxy.backend.communication.jdbc.connection.BackendConnection;
import org.apache.shardingsphere.proxy.frontend.command.CommandExecutorTask;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.internal.util.reflection.FieldReader;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class CommandExecutorTaskAdviceTest {
    
    private static final CommandExecutorTaskAdvice ADVICE = new CommandExecutorTaskAdvice();
    
    private static MockTracer tracer;
    
    private static Method executeCommandMethod;
    
    @BeforeClass
    @SneakyThrows
    public static void setup() {
        if (!GlobalTracer.isRegistered()) {
            GlobalTracer.register(new MockTracer());
        }
        FieldReader fieldReader = new FieldReader(GlobalTracer.get(), GlobalTracer.class.getDeclaredField("tracer"));
        tracer = (MockTracer) fieldReader.read();
        executeCommandMethod = CommandExecutorTask.class.getDeclaredMethod("executeCommand", ChannelHandlerContext.class, PacketPayload.class, BackendConnection.class);
    }
    
    @Before
    public void reset() {
        tracer.reset();
    }
    
    @Test
    public void testMethod() {
        final MockTargetObject targetObject = new MockTargetObject();
        ADVICE.beforeMethod(targetObject, executeCommandMethod, new Object[]{}, new MethodInvocationResult());
        ADVICE.afterMethod(targetObject, executeCommandMethod, new Object[]{}, new MethodInvocationResult());
        List<MockSpan> spans = tracer.finishedSpans();
        Assert.assertEquals(1, spans.size());
        Assert.assertEquals(0, spans.get(0).logEntries().size());
        Assert.assertEquals("/ShardingSphere/rootInvoke/", spans.get(0).operationName());
    }
    
    @Test
    public void testExceptionHandle() {
        MockTargetObject targetObject = new MockTargetObject();
        ADVICE.beforeMethod(targetObject, executeCommandMethod, new Object[]{}, new MethodInvocationResult());
        ADVICE.onThrowing(targetObject, executeCommandMethod, new Object[]{}, new IOException());
        ADVICE.afterMethod(targetObject, executeCommandMethod, new Object[]{}, new MethodInvocationResult());
        List<MockSpan> spans = tracer.finishedSpans();
        Assert.assertEquals(1, spans.size());
        MockSpan span = spans.get(0);
        Assert.assertTrue((boolean) span.tags().get("error"));
        List<MockSpan.LogEntry> entries = span.logEntries();
        Assert.assertEquals(1, entries.size());
        Map<String, ?> fields = entries.get(0).fields();
        Assert.assertEquals("error", fields.get("event"));
        Assert.assertEquals(null, fields.get("message"));
        Assert.assertEquals("java.io.IOException", fields.get("error.kind"));
        Assert.assertEquals("/ShardingSphere/rootInvoke/", span.operationName());
    }
    
}
