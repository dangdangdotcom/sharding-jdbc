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

package org.apache.shardingsphere.distsql.parser.api.asserts.rdl.impl.drop;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.SQLCaseAssertContext;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.segment.impl.ExpectedDataSourceName;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.statement.rdl.drop.DropResourceStatementTestCase;
import org.apache.shardingsphere.distsql.parser.statement.rdl.drop.impl.DropResourceStatement;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Drop resource statement assert.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DropResourceStatementAssert {

    /**
     * Assert drop resource statement is correct with expected parser result.
     *
     * @param assertContext assert context
     * @param actual        actual drop resource statement
     * @param expected      expected drop resource statement test case
     */
    public static void assertIs(SQLCaseAssertContext assertContext, DropResourceStatement actual, DropResourceStatementTestCase expected) {
        if (null != expected.getDataSource()) {
            List<String> expectResources = expected.getDataSource().stream().map(ExpectedDataSourceName::getName).collect(Collectors.toList());
            assertThat(assertContext.getText(String.format("%s assert error", actual.getClass().getSimpleName())), actual.getResourceNames().containsAll(expectResources), is(true));
        } else {
            assertNull(assertContext.getText("Actual resource should not exit."), actual);
        }
    }
}
