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

package org.apache.shardingsphere.sharding.rewrite.token.generator.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.apache.shardingsphere.sharding.rewrite.token.pojo.impl.TableToken;
import org.apache.shardingsphere.sql.parser.relation.segment.table.Table;
import org.apache.shardingsphere.sql.parser.relation.segment.table.TablesContext;
import org.apache.shardingsphere.sql.parser.relation.statement.SQLStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ColumnProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ShorthandProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.ColumnOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.OrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.PredicateSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.OwnerAvailable;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.OwnerSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.generic.TableSegmentsAvailable;
import org.apache.shardingsphere.sql.parser.sql.statement.generic.WhereSegmentAvailable;
import org.apache.shardingsphere.underlying.rewrite.sql.token.generator.CollectionSQLTokenGenerator;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Table token generator.
 */
public final class TableTokenGenerator implements CollectionSQLTokenGenerator {
    
    @Override
    public boolean isGenerateSQLToken(final SQLStatementContext sqlStatementContext) {
        return true;
    }
    
    @Override
    public Collection<TableToken> generateSQLTokens(final SQLStatementContext sqlStatementContext) {
        if (!(sqlStatementContext.getSqlStatement() instanceof TableSegmentsAvailable)) {
            return Collections.emptyList();
        }
        Collection<TableToken> result = new LinkedList<>();
        for (TableSegment each : ((TableSegmentsAvailable) sqlStatementContext.getSqlStatement()).getAllTables()) {
            result.add(new TableToken(each.getStartIndex(), each.getStopIndex(), each.getIdentifier()));
        }
        if (sqlStatementContext.getSqlStatement() instanceof WhereSegmentAvailable && ((WhereSegmentAvailable) sqlStatementContext.getSqlStatement()).getWhere().isPresent()) {
            result.addAll(generateSQLTokens(sqlStatementContext, ((WhereSegmentAvailable) sqlStatementContext.getSqlStatement()).getWhere().get()));
        }
        if (sqlStatementContext.getSqlStatement() instanceof SelectStatement) {
            result.addAll(generateSQLTokens(sqlStatementContext, ((SelectStatement) sqlStatementContext.getSqlStatement()).getProjections()));
            if (((SelectStatement) sqlStatementContext.getSqlStatement()).getGroupBy().isPresent()) {
                result.addAll(generateSQLTokens(sqlStatementContext, ((SelectStatement) sqlStatementContext.getSqlStatement()).getGroupBy().get().getGroupByItems()));
            }
            if (((SelectStatement) sqlStatementContext.getSqlStatement()).getOrderBy().isPresent()) {
                result.addAll(generateSQLTokens(sqlStatementContext, ((SelectStatement) sqlStatementContext.getSqlStatement()).getOrderBy().get().getOrderByItems()));
            }
        }
        return result;
    }
    
    private Collection<TableToken> generateSQLTokens(final SQLStatementContext sqlStatementContext, final WhereSegment where) {
        Collection<TableToken> result = new LinkedList<>();
        for (AndPredicate each : where.getAndPredicates()) {
            for (PredicateSegment predicate : each.getPredicates()) {
                result.addAll(getTableTokens(sqlStatementContext, predicate));
            }
        }
        return result;
    }
    
    private Collection<TableToken> generateSQLTokens(final SQLStatementContext sqlStatementContext, final ProjectionsSegment projectionsSegment) {
        Collection<TableToken> result = new LinkedList<>();
        for (ProjectionSegment each : projectionsSegment.getProjections()) {
            if (each instanceof ShorthandProjectionSegment) {
                Optional<TableToken> tableToken = generateSQLToken(sqlStatementContext, (ShorthandProjectionSegment) each);
                if (tableToken.isPresent()) {
                    result.add(tableToken.get());
                }
            }
            if (each instanceof ColumnProjectionSegment) {
                Optional<TableToken> tableToken = generateSQLToken(sqlStatementContext, (ColumnProjectionSegment) each);
                if (tableToken.isPresent()) {
                    result.add(tableToken.get());
                }
            }
        }
        return result;
    }
    
    private Collection<TableToken> generateSQLTokens(final SQLStatementContext sqlStatementContext, final Collection<OrderByItemSegment> orderBys) {
        Collection<TableToken> result = new LinkedList<>();
        for (OrderByItemSegment each : orderBys) {
            if (isToGenerateTableToken(sqlStatementContext.getTablesContext(), each)) {
                Preconditions.checkState(((ColumnOrderByItemSegment) each).getColumn().getOwner().isPresent());
                OwnerSegment segment = ((ColumnOrderByItemSegment) each).getColumn().getOwner().get();
                result.add(new TableToken(segment.getStartIndex(), segment.getStopIndex(), segment.getIdentifier()));
            }
        }
        return result;
    }
    
    private Optional<TableToken> generateSQLToken(final SQLStatementContext sqlStatementContext, final OwnerAvailable segment) {
        Optional<OwnerSegment> owner = segment.getOwner();
        return owner.isPresent() && isToGenerateTableToken(sqlStatementContext, owner.get())
                ? Optional.of(new TableToken(owner.get().getStartIndex(), owner.get().getStopIndex(), owner.get().getIdentifier())) : Optional.<TableToken>absent();
    }
    
    private Collection<TableToken> getTableTokens(final SQLStatementContext sqlStatementContext, final PredicateSegment predicate) {
        Collection<TableToken> result = new LinkedList<>();
        if (isToGenerateTableTokenLeftValue(sqlStatementContext.getTablesContext(), predicate)) {
            Preconditions.checkState(predicate.getColumn().getOwner().isPresent());
            OwnerSegment segment = predicate.getColumn().getOwner().get();
            result.add(new TableToken(segment.getStartIndex(), segment.getStopIndex(), segment.getIdentifier()));
        }
        if (isToGenerateTableTokenForRightValue(sqlStatementContext.getTablesContext(), predicate)) {
            Preconditions.checkState(((ColumnSegment) predicate.getRightValue()).getOwner().isPresent());
            OwnerSegment segment = ((ColumnSegment) predicate.getRightValue()).getOwner().get();
            result.add(new TableToken(segment.getStartIndex(), segment.getStopIndex(), segment.getIdentifier()));
        }
        if (isToGenerateTableTokenForProjection(sqlStatementContext.getTablesContext(), predicate)) {
            Preconditions.checkState(((ColumnProjectionSegment) predicate.getRightValue()).getOwner().isPresent());
            OwnerSegment segment = ((ColumnProjectionSegment) predicate.getRightValue()).getOwner().get();
            result.add(new TableToken(segment.getStartIndex(), segment.getStopIndex(), segment.getIdentifier()));
        }
        return result;
    }
    
    private boolean isToGenerateTableTokenLeftValue(final TablesContext tablesContext, final PredicateSegment predicate) {
        return predicate.getColumn().getOwner().isPresent() && isTable(predicate.getColumn().getOwner().get(), tablesContext);
    }
    
    private boolean isToGenerateTableTokenForRightValue(final TablesContext tablesContext, final PredicateSegment predicate) {
        return predicate.getRightValue() instanceof ColumnSegment
                && ((ColumnSegment) predicate.getRightValue()).getOwner().isPresent() && isTable(((ColumnSegment) predicate.getRightValue()).getOwner().get(), tablesContext);
    }
    
    private boolean isToGenerateTableTokenForProjection(final TablesContext tablesContext, final PredicateSegment predicate) {
        return predicate.getRightValue() instanceof ColumnProjectionSegment && ((ColumnProjectionSegment) predicate.getRightValue()).getOwner().isPresent()
                && isTable(((ColumnProjectionSegment) predicate.getRightValue()).getOwner().get(), tablesContext);
    }
    
    private boolean isToGenerateTableToken(final TablesContext tablesContext, final OrderByItemSegment orderByItemSegment) {
        return orderByItemSegment instanceof ColumnOrderByItemSegment && ((ColumnOrderByItemSegment) orderByItemSegment).getColumn().getOwner().isPresent() 
                && isTable(((ColumnOrderByItemSegment) orderByItemSegment).getColumn().getOwner().get(), tablesContext);
    }
    
    private boolean isToGenerateTableToken(final SQLStatementContext sqlStatementContext, final OwnerSegment ownerSegment) {
        Optional<Table> table = sqlStatementContext.getTablesContext().find(ownerSegment.getIdentifier().getValue());
        return table.isPresent() && !table.get().getAlias().isPresent();
    }
    
    private boolean isTable(final OwnerSegment owner, final TablesContext tablesContext) {
        return !tablesContext.findTableFromAlias(owner.getIdentifier().getValue()).isPresent();
    }
}
