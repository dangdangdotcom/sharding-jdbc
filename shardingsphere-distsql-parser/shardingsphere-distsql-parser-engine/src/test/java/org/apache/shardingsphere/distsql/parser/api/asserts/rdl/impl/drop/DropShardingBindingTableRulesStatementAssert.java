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
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.statement.rdl.drop.DropShardingBindingTableRulesStatementTestCase;
import org.apache.shardingsphere.distsql.parser.statement.rdl.drop.impl.DropShardingBindingTableRulesStatement;

/**
 * Drop sharding binding table rule statement assert.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DropShardingBindingTableRulesStatementAssert {


    /**
     * Assert drop sharding binding table rule statement is correct with expected parser result.
     *
     * @param assertContext assert context
     * @param actual        actual drop sharding binding table rule statement
     * @param expected      expected drop sharding binding table rule statement test case
     */
    public static void assertIs(SQLCaseAssertContext assertContext, DropShardingBindingTableRulesStatement actual, DropShardingBindingTableRulesStatementTestCase expected) {
    }
}
