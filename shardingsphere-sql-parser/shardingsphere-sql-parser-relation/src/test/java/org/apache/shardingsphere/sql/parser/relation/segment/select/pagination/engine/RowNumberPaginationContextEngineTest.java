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

package org.apache.shardingsphere.sql.parser.relation.segment.select.pagination.engine;

import org.apache.shardingsphere.sql.parser.relation.segment.select.pagination.PaginationContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.Projection;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.ProjectionsContext;
import org.apache.shardingsphere.sql.parser.relation.segment.select.projection.impl.ColumnProjection;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.column.ColumnSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.LiteralExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.simple.ParameterMarkerExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.pagination.PaginationValueSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.pagination.rownum.NumberLiteralRowNumberValueSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.pagination.rownum.ParameterMarkerRowNumberValueSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.AndPredicate;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.PredicateSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateCompareRightValue;
import org.apache.shardingsphere.sql.parser.sql.value.identifier.IdentifierValue;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class RowNumberPaginationContextEngineTest {
    
    private static final String ROW_NUMBER_COLUMN_NAME = "rownum";
    
    private static final String ROW_NUMBER_COLUMN_ALIAS = "predicateRowNumberAlias";
    
    @Test
    public void assertCreatePaginationContextWhenRowNumberAliasNotPresent() {
        ProjectionsContext projectionsContext = new ProjectionsContext(0, 0, false, Collections.emptyList());
        PaginationContext paginationContext = new RowNumberPaginationContextEngine().createPaginationContext(null, projectionsContext, Collections.emptyList());
        assertFalse(paginationContext.getOffsetSegment().isPresent());
        assertFalse(paginationContext.getRowCountSegment().isPresent());
    }
    
    @Test
    public void assertCreatePaginationContextWhenRowNumberAliasIsPresentAndRowNumberPredicatesIsEmpty() {
        Projection projectionWithRowNumberAlias = new ColumnProjection(null, ROW_NUMBER_COLUMN_NAME, ROW_NUMBER_COLUMN_ALIAS);
        ProjectionsContext projectionsContext = new ProjectionsContext(0, 0, false, Collections.singleton(projectionWithRowNumberAlias));
        PaginationContext paginationContext = new RowNumberPaginationContextEngine().createPaginationContext(Collections.emptyList(), projectionsContext, Collections.emptyList());
        assertFalse(paginationContext.getOffsetSegment().isPresent());
        assertFalse(paginationContext.getRowCountSegment().isPresent());
    }
    
    @Test
    public void assertCreatePaginationContextWhenRowNumberAliasIsPresentAndRowNumberPredicatesNotEmptyWithLessThanOperator() {
        assertCreatePaginationContextWhenRowNumberAliasPresentAndRowNumberPredicatedNotEmptyWithGivenOperator("<");
    }
    
    @Test
    public void assertCreatePaginationContextWhenRowNumberAliasIsPresentAndRowNumberPredicatesNotEmptyWithLessThanEqualOperator() {
        assertCreatePaginationContextWhenRowNumberAliasPresentAndRowNumberPredicatedNotEmptyWithGivenOperator("<=");
    }
    
    @Test
    public void assertCreatePaginationContextWhenOffsetSegmentInstanceOfNumberLiteralRowNumberValueSegmentWithGreatThanOperator() {
        assertCreatePaginationContextWhenOffsetSegmentInstanceOfNumberLiteralRowNumberValueSegmentWithGivenOperator(">");
    }
    
    @Test
    public void assertCreatePaginationContextWhenOffsetSegmentInstanceOfNumberLiteralRowNumberValueSegmentWithGreatThanEqualOperator() {
        assertCreatePaginationContextWhenOffsetSegmentInstanceOfNumberLiteralRowNumberValueSegmentWithGivenOperator(">=");
    }
    
    @Test
    public void assertCreatePaginationContextWhenRowNumberAliasIsPresentAndRowNumberPredicatesNotEmpty() {
        Projection projectionWithRowNumberAlias = new ColumnProjection(null, ROW_NUMBER_COLUMN_NAME, ROW_NUMBER_COLUMN_ALIAS);
        ProjectionsContext projectionsContext = new ProjectionsContext(0, 0, false, Collections.singleton(projectionWithRowNumberAlias));
        AndPredicate andPredicate = new AndPredicate();
        PredicateSegment predicateSegment = new PredicateSegment(0, 0, new ColumnSegment(0, 10, new IdentifierValue(ROW_NUMBER_COLUMN_NAME)), null);
        andPredicate.getPredicates().addAll(Collections.singleton(predicateSegment));
        PaginationContext paginationContext = new RowNumberPaginationContextEngine().createPaginationContext(Collections.singleton(andPredicate), projectionsContext, Collections.emptyList());
        assertFalse(paginationContext.getOffsetSegment().isPresent());
        assertFalse(paginationContext.getRowCountSegment().isPresent());
    }
    
    @Test
    public void assertCreatePaginationContextWhenParameterMarkerRowNumberValueSegment() {
        PredicateCompareRightValue predicateCompareRightValue = new PredicateCompareRightValue(">", new ParameterMarkerExpressionSegment(0, 10, 0));
        AndPredicate andPredicate = new AndPredicate();
        andPredicate.getPredicates().add(new PredicateSegment(0, 10, new ColumnSegment(0, 10, new IdentifierValue(ROW_NUMBER_COLUMN_NAME)), predicateCompareRightValue));
        Projection projectionWithRowNumberAlias = new ColumnProjection(null, ROW_NUMBER_COLUMN_NAME, ROW_NUMBER_COLUMN_ALIAS);
        ProjectionsContext projectionsContext = new ProjectionsContext(0, 0, false, Collections.singleton(projectionWithRowNumberAlias));
        
        PaginationContext paginationContext = new RowNumberPaginationContextEngine()
                .createPaginationContext(Collections.singleton(andPredicate), projectionsContext, Collections.singletonList(1));
        Optional<PaginationValueSegment> offSetSegmentPaginationValue = paginationContext.getOffsetSegment();
        assertTrue(offSetSegmentPaginationValue.isPresent());
        assertThat(offSetSegmentPaginationValue.get(), instanceOf(ParameterMarkerRowNumberValueSegment.class));
        assertFalse(paginationContext.getRowCountSegment().isPresent());
    }
    
    private void assertCreatePaginationContextWhenRowNumberAliasPresentAndRowNumberPredicatedNotEmptyWithGivenOperator(final String operator) {
        PredicateCompareRightValue predicateCompareRightValue = new PredicateCompareRightValue(operator, new LiteralExpressionSegment(0, 10, 100));
        AndPredicate andPredicate = new AndPredicate();
        andPredicate.getPredicates().add(new PredicateSegment(0, 10, new ColumnSegment(0, 10, new IdentifierValue(ROW_NUMBER_COLUMN_NAME)), predicateCompareRightValue));
        Projection projectionWithRowNumberAlias = new ColumnProjection(null, ROW_NUMBER_COLUMN_NAME, ROW_NUMBER_COLUMN_ALIAS);
        ProjectionsContext projectionsContext = new ProjectionsContext(0, 0, false, Collections.singleton(projectionWithRowNumberAlias));
        
        PaginationContext paginationContext = new RowNumberPaginationContextEngine().createPaginationContext(Collections.singleton(andPredicate), projectionsContext, Collections.emptyList());
        assertFalse(paginationContext.getOffsetSegment().isPresent());
        Optional<PaginationValueSegment> paginationValueSegmentOptional = paginationContext.getRowCountSegment();
        assertTrue(paginationValueSegmentOptional.isPresent());
        PaginationValueSegment paginationValueSegment = paginationValueSegmentOptional.get();
        assertTrue(paginationValueSegment instanceof NumberLiteralRowNumberValueSegment);
        NumberLiteralRowNumberValueSegment numberLiteralRowNumberValueSegment = (NumberLiteralRowNumberValueSegment) paginationValueSegment;
        assertThat(numberLiteralRowNumberValueSegment.getStartIndex(), is(0));
        assertThat(numberLiteralRowNumberValueSegment.getStopIndex(), is(10));
        assertThat(numberLiteralRowNumberValueSegment.getValue(), is(100L));
    }
    
    private void assertCreatePaginationContextWhenOffsetSegmentInstanceOfNumberLiteralRowNumberValueSegmentWithGivenOperator(final String operator) {
        PredicateCompareRightValue predicateCompareRightValue = new PredicateCompareRightValue(operator, new LiteralExpressionSegment(0, 10, 100));
        AndPredicate andPredicate = new AndPredicate();
        andPredicate.getPredicates().add(new PredicateSegment(0, 10, new ColumnSegment(0, 10, new IdentifierValue(ROW_NUMBER_COLUMN_NAME)), predicateCompareRightValue));
        Projection projectionWithRowNumberAlias = new ColumnProjection(null, ROW_NUMBER_COLUMN_NAME, ROW_NUMBER_COLUMN_ALIAS);
        ProjectionsContext projectionsContext = new ProjectionsContext(0, 0, false, Collections.singleton(projectionWithRowNumberAlias));
        
        PaginationContext rowNumberPaginationContextEngine = new RowNumberPaginationContextEngine()
                .createPaginationContext(Collections.singleton(andPredicate), projectionsContext, Collections.emptyList());
        Optional<PaginationValueSegment> paginationValueSegment = rowNumberPaginationContextEngine.getOffsetSegment();
        assertTrue(paginationValueSegment.isPresent());
        PaginationValueSegment actualPaginationValueSegment = paginationValueSegment.get();
        assertThat(actualPaginationValueSegment, instanceOf(NumberLiteralRowNumberValueSegment.class));
        assertThat(actualPaginationValueSegment.getStartIndex(), is(0));
        assertThat(actualPaginationValueSegment.getStopIndex(), is(10));
        assertThat(((NumberLiteralRowNumberValueSegment) actualPaginationValueSegment).getValue(), is(100L));
        Optional<PaginationValueSegment> rowCountSegment = rowNumberPaginationContextEngine.getRowCountSegment();
        assertFalse(rowCountSegment.isPresent());
    }
}
