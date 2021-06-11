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

package org.apache.shardingsphere.distsql.parser.api.asserts.rdl.impl;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.shardingsphere.distsql.parser.api.asserts.rdl.impl.drop.*;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.SQLCaseAssertContext;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.statement.SQLParserTestCase;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.statement.rdl.drop.*;
import org.apache.shardingsphere.distsql.parser.statement.rdl.drop.DropRDLStatement;
import org.apache.shardingsphere.distsql.parser.statement.rdl.drop.impl.*;

/**
 * Drop RDL statement assert.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DropRDLStatementAssert {

    /**
     * Assert drop RDL statement is correct with expected parser result.
     *
     * @param assertContext assert context
     * @param actual        actual drop RDL statement
     * @param expected      expected drop RDL statement test case
     */
    public static void assertIs(SQLCaseAssertContext assertContext, DropRDLStatement actual, SQLParserTestCase expected) {
        if (actual instanceof DropDatabaseDiscoveryRuleStatement) {
            DropDatabaseDiscoveryRuleStatementAssert.assertIs(assertContext, (DropDatabaseDiscoveryRuleStatement) actual, (DropDataBaseDiscoveryRuleStatementTestCase) expected);
        } else if (actual instanceof DropEncryptRuleStatement) {
            DropEncryptRuleStatementAssert.assertIs(assertContext, (DropEncryptRuleStatement) actual, (DropEncryptRuleStatementTestCase) expected);
        } else if (actual instanceof DropReadwriteSplittingRuleStatement) {
            DropReadwriteSplittingRuleStatementAssert.assertIs(assertContext, (DropReadwriteSplittingRuleStatement) actual, (DropReadWriteSplittingRuleStatementTestCase) expected);
        }else if (actual instanceof DropResourceStatement) {
            DropResourceStatementAssert.assertIs(assertContext, (DropResourceStatement) actual, (DropResourceStatementTestCase) expected);
        } else if (actual instanceof DropShardingBindingTableRulesStatement) {
            DropShardingBindingTableRulesStatementAssert.assertIs(assertContext, (DropShardingBindingTableRulesStatement) actual, (DropShardingBindingTableRulesStatementTestCase) expected);
        } else if (actual instanceof DropShardingBroadcastTableRulesStatement) {
            DropShardingBroadcastTableRulesStatementAssert.assertIs(assertContext, (DropShardingBroadcastTableRulesStatement) actual, (DropShardingBroadcastTableRulesStatementTestCase) expected);
        } else if (actual instanceof DropShardingTableRuleStatement) {
            DropShardingTableRuleStatementAssert.assertIs(assertContext, (DropShardingTableRuleStatement) actual, (DropShardingTableRuleStatementTestCase) expected);
        }
    }
}
