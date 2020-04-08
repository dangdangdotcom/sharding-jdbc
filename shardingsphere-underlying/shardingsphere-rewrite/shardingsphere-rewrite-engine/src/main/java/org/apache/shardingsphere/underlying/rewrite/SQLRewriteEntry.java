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

package org.apache.shardingsphere.underlying.rewrite;

import org.apache.shardingsphere.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.spi.order.OrderedSPIRegistry;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.underlying.common.config.properties.ConfigurationProperties;
import org.apache.shardingsphere.underlying.common.rule.BaseRule;
import org.apache.shardingsphere.underlying.rewrite.context.SQLRewriteContext;
import org.apache.shardingsphere.underlying.rewrite.context.SQLRewriteContextDecorator;
import org.apache.shardingsphere.underlying.rewrite.engine.SQLRewriteEngine;
import org.apache.shardingsphere.underlying.rewrite.engine.SQLRewriteResult;
import org.apache.shardingsphere.underlying.rewrite.engine.SQLRouteRewriteEngine;
import org.apache.shardingsphere.underlying.route.context.RouteContext;
import org.apache.shardingsphere.underlying.route.context.RouteUnit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL rewrite entry.
 */
public final class SQLRewriteEntry {
    
    private final SchemaMetaData schemaMetaData;
    
    private final ConfigurationProperties properties;
    
    private final Map<BaseRule, SQLRewriteContextDecorator> decorators;
    
    static {
        ShardingSphereServiceLoader.register(SQLRewriteContextDecorator.class);
    }
    
    public SQLRewriteEntry(final SchemaMetaData schemaMetaData, final ConfigurationProperties properties, final Collection<BaseRule> rules) {
        this.schemaMetaData = schemaMetaData;
        this.properties = properties;
        decorators = new LinkedHashMap<>();
        OrderedSPIRegistry.getRegisteredServices(rules, SQLRewriteContextDecorator.class).forEach(decorators::put);
    }
    
    /**
     * Rewrite.
     * 
     * @param sql SQL
     * @param parameters SQL parameters
     * @param routeContext route context
     * @return route unit and SQL rewrite result map
     */
    public Map<RouteUnit, SQLRewriteResult> rewrite(final String sql, final List<Object> parameters, final RouteContext routeContext) {
        SQLRewriteContext sqlRewriteContext = createSQLRewriteContext(sql, parameters, routeContext.getSqlStatementContext(), routeContext);
        return routeContext.getRouteResult().getRouteUnits().isEmpty() ? rewrite(sqlRewriteContext) : rewrite(routeContext, sqlRewriteContext);
    }
    
    private Map<RouteUnit, SQLRewriteResult> rewrite(final SQLRewriteContext sqlRewriteContext) {
        return Collections.singletonMap(null, new SQLRewriteEngine().rewrite(sqlRewriteContext));
    }
    
    private Map<RouteUnit, SQLRewriteResult> rewrite(final RouteContext routeContext, final SQLRewriteContext sqlRewriteContext) {
        return new SQLRouteRewriteEngine().rewrite(sqlRewriteContext, routeContext.getRouteResult());
    }
    
    private SQLRewriteContext createSQLRewriteContext(final String sql, final List<Object> parameters, final SQLStatementContext sqlStatementContext, final RouteContext routeContext) {
        SQLRewriteContext result = new SQLRewriteContext(schemaMetaData, sqlStatementContext, sql, parameters);
        decorate(decorators, result, routeContext);
        result.generateSQLTokens();
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private void decorate(final Map<BaseRule, SQLRewriteContextDecorator> decorators, final SQLRewriteContext sqlRewriteContext, final RouteContext routeContext) {
        decorators.forEach((key, value) -> value.decorate(key, properties, sqlRewriteContext, routeContext));
    }
}
