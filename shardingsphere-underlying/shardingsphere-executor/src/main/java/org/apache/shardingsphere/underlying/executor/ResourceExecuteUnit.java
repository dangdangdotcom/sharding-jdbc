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

package org.apache.shardingsphere.underlying.executor;

import org.apache.shardingsphere.underlying.executor.constant.ConnectionMode;
import org.apache.shardingsphere.underlying.executor.context.ExecutionUnit;

/**
 * Resource execute unit.
 * 
 * @param <T> type of resource
 */
public interface ResourceExecuteUnit<T> {
    
    /**
     * Get execution unit.
     * 
     * @return execution unit
     */
    ExecutionUnit getExecutionUnit();
    
    /**
     * Get resource.
     * 
     * @return resource
     */
    T getResource();
    
    /**
     * Get connection mode.
     * 
     * @return connection mode
     */
    ConnectionMode getConnectionMode();
}
