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

package org.apache.shardingsphere.sql.parser.integrate.asserts.segment.table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.sql.parser.integrate.asserts.SQLCaseAssertMessage;
import org.apache.shardingsphere.sql.parser.integrate.asserts.segment.SQLSegmentAssert;
import org.apache.shardingsphere.sql.parser.integrate.asserts.segment.owner.OwnerAssert;
import org.apache.shardingsphere.sql.parser.integrate.jaxb.impl.table.ExpectedTable;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.TableSegment;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Table assert.
 *
 * @author zhangliang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TableAssert {
    
    /**
     * Assert actual table segment is correct with expected table.
     * 
     * @param assertMessage assert message
     * @param actual actual tables
     * @param expected expected tables
     */
    public static void assertIs(final SQLCaseAssertMessage assertMessage, final Collection<TableSegment> actual, final List<ExpectedTable> expected) {
        assertThat(assertMessage.getText("Tables size assertion error: "), actual.size(), is(expected.size()));
        int count = 0;
        for (TableSegment each : actual) {
            assertTable(assertMessage, each, expected.get(count));
            count++;
        }
    }
    
    private static void assertTable(final SQLCaseAssertMessage assertMessage, final TableSegment actual, final ExpectedTable expected) {
        assertThat(assertMessage.getText("Table name assertion error: "), actual.getTableName(), is(expected.getName()));
        assertThat(assertMessage.getText("Table alias assertion error: "), actual.getAlias().orNull(), is(expected.getAlias()));
        if (null != expected.getOwner()) {
            assertTrue(assertMessage.getText("Actual owner should exist."), actual.getOwner().isPresent());
            OwnerAssert.assertSchema(assertMessage, actual.getOwner().get(), expected.getOwner());
        } else {
            assertFalse(assertMessage.getText("Actual owner should not exist."), actual.getOwner().isPresent());
        }
        assertThat(assertMessage.getText("Table start delimiter assertion error: "), actual.getTableQuoteCharacter().getStartDelimiter(), is(expected.getStartDelimiter()));
        assertThat(assertMessage.getText("Table end delimiter assertion error: "), actual.getTableQuoteCharacter().getEndDelimiter(), is(expected.getEndDelimiter()));
        SQLSegmentAssert.assertIs(assertMessage, actual, expected);
    }
}
