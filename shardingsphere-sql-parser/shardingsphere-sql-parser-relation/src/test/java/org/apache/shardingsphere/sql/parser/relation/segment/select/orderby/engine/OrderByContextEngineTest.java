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

package org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.engine;

import org.apache.shardingsphere.sql.parser.relation.segment.select.groupby.GroupByContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.OrderByContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.orderby.OrderByItem;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.OrderBySegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.ColumnOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class OrderByContextEngineTest {
    
    @Test
    public void assertCreateOrderByWithoutOrderBy() {
        SelectStatement selectStatement = mock(SelectStatement.class);
        GroupByContext groupByContext = mock(GroupByContext.class);
        OrderByItem orderByItem1 = mock(OrderByItem.class);
        OrderByItem orderByItem2 = mock(OrderByItem.class);
        Collection<OrderByItem> orderByItems = Arrays.asList(orderByItem1, orderByItem2);
        when(groupByContext.getItems()).thenReturn(orderByItems);
        when(selectStatement.getOrderBy()).thenReturn(Optional.empty());
        OrderByContext actualOrderByContext = new OrderByContextEngine().createOrderBy(selectStatement, groupByContext);
        assertThat(actualOrderByContext.getItems(), is(orderByItems));
        assertTrue(actualOrderByContext.isGenerated());
    }
    
    @Test
    public void assertCreateOrderByWithOrderBy() {
        SelectStatement selectStatement = mock(SelectStatement.class);
        ColumnOrderByItemSegment columnOrderByItemSegment = mock(ColumnOrderByItemSegment.class);
        IndexOrderByItemSegment indexOrderByItemSegment1 = mock(IndexOrderByItemSegment.class);
        when(indexOrderByItemSegment1.getColumnIndex()).thenReturn(2);
        IndexOrderByItemSegment indexOrderByItemSegment2 = mock(IndexOrderByItemSegment.class);
        when(indexOrderByItemSegment2.getColumnIndex()).thenReturn(3);
        OrderBySegment orderBySegment = mock(OrderBySegment.class);
        when(orderBySegment.getOrderByItems()).thenReturn(Arrays.asList(columnOrderByItemSegment, indexOrderByItemSegment1, indexOrderByItemSegment2));
        when(selectStatement.getOrderBy()).thenReturn(Optional.of(orderBySegment));
        OrderByContext actualOrderByContext = new OrderByContextEngine().createOrderBy(selectStatement, mock(GroupByContext.class));
        OrderByItem orderByItem1 = new OrderByItem(indexOrderByItemSegment1);
        orderByItem1.setIndex(2);
        OrderByItem orderByItem2 = new OrderByItem(indexOrderByItemSegment2);
        orderByItem2.setIndex(3);
        assertThat(actualOrderByContext.getItems(), is(Arrays.asList(new OrderByItem(columnOrderByItemSegment), orderByItem1, orderByItem2)));
        assertFalse(actualOrderByContext.isGenerated());
        List<OrderByItem> results = new ArrayList<>(actualOrderByContext.getItems());
        assertThat(results.get(0).getIndex(), is(0));
        assertThat(results.get(1).getIndex(), is(2));
        assertThat(results.get(2).getIndex(), is(3));
    }

    @Test
    public void assertCreateOrderInDistinctByWithoutOrderBy() {
        SelectStatement selectStatement = mock(SelectStatement.class);
        GroupByContext groupByContext = mock(GroupByContext.class);
        ProjectionsSegment projections = mock(ProjectionsSegment.class);
        when(selectStatement.getProjections()).thenReturn(projections);
        when(projections.isDistinctRow()).thenReturn(true);
        when(groupByContext.getItems()).thenReturn(Collections.emptyList());
        when(selectStatement.getOrderBy()).thenReturn(Optional.empty());
        OrderByContext actualOrderByContext = new OrderByContextEngine().createOrderBy(selectStatement, groupByContext);
        assertThat(actualOrderByContext.getItems(), is(Collections.emptyList()));
        assertTrue(actualOrderByContext.isGenerated());
    }
}
