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

package org.apache.shardingsphere.sql.parser.binder.statement.impl;

import com.google.common.collect.Sets;
import org.apache.shardingsphere.sql.parser.binder.metadata.schema.SchemaMetaData;
import org.apache.shardingsphere.sql.parser.binder.statement.dml.InsertStatementContext;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.assignment.AssignmentSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.assignment.InsertValuesSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.InsertColumnsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.OnDuplicateKeyColumnsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.subquery.SubquerySegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.SimpleTableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.InsertStatement;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.sql.parser.sql.value.identifier.IdentifierValue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class InsertStatementContextTest {
    
    @Test
    public void assertInsertStatementContextWithColumnNames() {
        InsertStatement insertStatement = new InsertStatement();
        insertStatement.setTable(new SimpleTableSegment(0, 0, new IdentifierValue("tbl")));
        InsertColumnsSegment insertColumnsSegment = new InsertColumnsSegment(0, 0, Arrays.asList(
                new ColumnSegment(0, 0, new IdentifierValue("id")), new ColumnSegment(0, 0, new IdentifierValue("name")), new ColumnSegment(0, 0, new IdentifierValue("status"))));
        insertStatement.setInsertColumns(insertColumnsSegment);
        setUpInsertValues(insertStatement);
        InsertStatementContext actual = new InsertStatementContext(mock(SchemaMetaData.class), Arrays.asList(1, "Tom", 2, "Jerry"), insertStatement);
        assertInsertStatementContext(actual);
    }
    
    @Test
    public void assertInsertStatementContextWithoutColumnNames() {
        SchemaMetaData schemaMetaData = mock(SchemaMetaData.class);
        when(schemaMetaData.getAllColumnNames("tbl")).thenReturn(Arrays.asList("id", "name", "status"));
        InsertStatement insertStatement = new InsertStatement();
        insertStatement.setTable(new SimpleTableSegment(0, 0, new IdentifierValue("tbl")));
        setUpInsertValues(insertStatement);
        InsertStatementContext actual = new InsertStatementContext(schemaMetaData, Arrays.asList(1, "Tom", 2, "Jerry"), insertStatement);
        assertInsertStatementContext(actual);
    }
    
    @Test
    public void assertGetGroupedParametersWithoutOnDuplicateParameter() {
        SchemaMetaData schemaMetaData = mock(SchemaMetaData.class);
        when(schemaMetaData.getAllColumnNames("tbl")).thenReturn(Arrays.asList("id", "name", "status"));
        InsertStatement insertStatement = new InsertStatement();
        insertStatement.setTable(new SimpleTableSegment(0, 0, new IdentifierValue("tbl")));
        setUpInsertValues(insertStatement);
        InsertStatementContext actual = new InsertStatementContext(schemaMetaData, Arrays.asList(1, "Tom", 2, "Jerry"), insertStatement);
        assertThat(actual.getGroupedParameters().size(), is(2));
        assertNull(actual.getOnDuplicateKeyUpdateValueContext());
        assertThat(actual.getOnDuplicateKeyUpdateParameters().size(), is(0));
    }
    
    @Test
    public void assertGetGroupedParametersWithOnDuplicateParameters() {
        SchemaMetaData schemaMetaData = mock(SchemaMetaData.class);
        when(schemaMetaData.getAllColumnNames("tbl")).thenReturn(Arrays.asList("id", "name", "status"));
        InsertStatement insertStatement = new InsertStatement();
        insertStatement.setTable(new SimpleTableSegment(0, 0, new IdentifierValue("tbl")));
        setUpInsertValues(insertStatement);
        setUpOnDuplicateValues(insertStatement);
        InsertStatementContext actual = new InsertStatementContext(schemaMetaData, Arrays.asList(1, "Tom", 2, "Jerry", "onDuplicateKeyUpdateColumnValue"), insertStatement);
        assertThat(actual.getGroupedParameters().size(), is(2));
        assertThat(actual.getOnDuplicateKeyUpdateValueContext().getColumns().size(), is(2));
        assertThat(actual.getOnDuplicateKeyUpdateParameters().size(), is(1));
    }
    
    @Test
    public void assertInsertSelect() {
        SchemaMetaData schemaMetaData = mock(SchemaMetaData.class);
        when(schemaMetaData.getAllColumnNames("tbl")).thenReturn(Arrays.asList("id", "name", "status"));
        InsertStatement insertStatement = new InsertStatement();
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.setProjections(new ProjectionsSegment(0, 0));
        SubquerySegment insertSelect = new SubquerySegment(0, 0, selectStatement);
        insertStatement.setInsertSelect(insertSelect);
        insertStatement.setTable(new SimpleTableSegment(0, 0, new IdentifierValue("tbl")));
        InsertStatementContext actual = new InsertStatementContext(schemaMetaData, Arrays.asList("param"), insertStatement);
        assertThat(actual.getInsertSelectContext().getParametersCount(), is(0));
        assertThat(actual.getGroupedParameters().size(), is(1));
        assertThat(actual.getGroupedParameters().iterator().next(), is(Collections.EMPTY_LIST));
    }
    
    private void setUpInsertValues(final InsertStatement insertStatement) {
        insertStatement.getValues().add(new InsertValuesSegment(0, 0, Arrays.asList(
                new ParameterMarkerExpressionSegment(0, 0, 1), new ParameterMarkerExpressionSegment(0, 0, 2), new LiteralExpressionSegment(0, 0, "init"))));
        insertStatement.getValues().add(new InsertValuesSegment(0, 0, Arrays.asList(
                new ParameterMarkerExpressionSegment(0, 0, 3), new ParameterMarkerExpressionSegment(0, 0, 4), new LiteralExpressionSegment(0, 0, "init"))));
    }
    
    private void setUpOnDuplicateValues(final InsertStatement insertStatement) {
        AssignmentSegment parameterMarkerExpressionAssignment = new AssignmentSegment(0, 0,
                new ColumnSegment(0, 0, new IdentifierValue("on_duplicate_key_update_column_1")),
                new ParameterMarkerExpressionSegment(0, 0, 4)
        );
        AssignmentSegment literalExpressionAssignment = new AssignmentSegment(0, 0,
                new ColumnSegment(0, 0, new IdentifierValue("on_duplicate_key_update_column_2")),
                new LiteralExpressionSegment(0, 0, 5)
        );
        OnDuplicateKeyColumnsSegment onDuplicateKeyColumnsSegment = new OnDuplicateKeyColumnsSegment(0, 0, Arrays.asList(
                parameterMarkerExpressionAssignment, literalExpressionAssignment
        ));
        insertStatement.setOnDuplicateKeyColumns(onDuplicateKeyColumnsSegment);
    }
    
    private void assertInsertStatementContext(final InsertStatementContext actual) {
        assertTrue(actual.toString().startsWith(String.format("%s(super", InsertStatementContext.class.getSimpleName())));
        assertNotNull(actual.getTablesContext());
        assertThat(actual.getTablesContext().getTableNames(), is(Sets.newLinkedHashSet(Arrays.asList("tbl"))));
        assertNotNull(actual.getAllTables());
        assertThat(actual.getAllTables().size(), is(1));
        SimpleTableSegment simpleTableSegment = actual.getAllTables().iterator().next();
        assertThat(simpleTableSegment.getTableName().getStartIndex(), is(0));
        assertThat(simpleTableSegment.getTableName().getStopIndex(), is(0));
        assertThat(simpleTableSegment.getTableName().getIdentifier().getValue(), is("tbl"));
        List<String> columnNames = new ArrayList<>(3);
        actual.getDescendingColumnNames().forEachRemaining(columnNames::add);
        assertThat(columnNames, is(Arrays.asList("status", "name", "id")));
        assertThat(actual.getGeneratedKeyContext(), is(Optional.empty()));
        assertThat(actual.getColumnNames(), is(Arrays.asList("id", "name", "status")));
        assertThat(actual.getInsertValueContexts().size(), is(2));
        assertThat(actual.getInsertValueContexts().get(0).getValue(0), is(1));
        assertThat(actual.getInsertValueContexts().get(0).getValue(1), is("Tom"));
        assertThat(actual.getInsertValueContexts().get(0).getValue(2), is("init"));
        assertThat(actual.getInsertValueContexts().get(1).getValue(0), is(2));
        assertThat(actual.getInsertValueContexts().get(1).getValue(1), is("Jerry"));
        assertThat(actual.getInsertValueContexts().get(1).getValue(2), is("init"));
    }
}
