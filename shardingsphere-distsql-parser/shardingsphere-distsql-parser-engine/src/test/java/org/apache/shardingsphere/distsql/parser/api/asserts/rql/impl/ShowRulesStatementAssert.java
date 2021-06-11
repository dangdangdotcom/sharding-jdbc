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

package org.apache.shardingsphere.distsql.parser.api.asserts.rql.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.distsql.parser.api.asserts.rql.impl.rule.*;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.SQLCaseAssertContext;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.statement.SQLParserTestCase;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.statement.rql.*;
import org.apache.shardingsphere.distsql.parser.statement.rql.show.*;

/**
 * Show rule statement assert.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ShowRulesStatementAssert {

    /**
     * Assert show rule statement is correct with expected parser result.
     *
     * @param assertContext assert context
     * @param actual        actual show rule statement
     * @param expected      expected show rule statement test case
     */
    public static void assertIs(SQLCaseAssertContext assertContext, ShowRulesStatement actual, SQLParserTestCase expected) {
        if (actual instanceof ShowDatabaseDiscoveryRulesStatement) {
            ShowDatabaseDiscoveryRulesStatementAssert.assertIs(assertContext, (ShowDatabaseDiscoveryRulesStatement) actual, (ShowDataBaseDiscoveryRulesStatementTestCase) expected);
        } else if (actual instanceof ShowEncryptRulesStatement) {
            ShowEncryptRulesStatementAssert.assertIs(assertContext, (ShowEncryptRulesStatement) actual, (ShowEncryptRulesStatementTestCase) expected);
        } else if (actual instanceof ShowReadwriteSplittingRulesStatement) {
            ShowReadwriteSplittingRulesStatementAssert.assertIs(assertContext, (ShowReadwriteSplittingRulesStatement) actual, (ShowReadWriteSplittingRulesStatementTestCase) expected);
        } else if (actual instanceof ShowShardingBindingTableRulesStatement) {
            ShowShardingBindingTableRulesStatementAssert.assertIs(assertContext, (ShowShardingBindingTableRulesStatement) actual, (ShowShardingBindingTableRulesStatementTestCase) expected);
        } else if (actual instanceof ShowShardingBroadcastTableRulesStatement) {
            ShowShardingBroadcastTableRulesStatementAssert.assertIs(assertContext, (ShowShardingBroadcastTableRulesStatement) actual, (ShowShardingBroadcastTableRulesStatementTestCase) expected);
        } else if (actual instanceof ShowShardingTableRulesStatement) {
            ShowShardingTableRulesStatementAssert.assertIs(assertContext, (ShowShardingTableRulesStatement) actual, (ShowShardingTableRulesStatementTestCase) expected);
        }
    }
}
