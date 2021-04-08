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

package org.apache.shardingsphere.authority.loader.storage.impl.dialect;

import org.apache.shardingsphere.authority.loader.storage.impl.StoragePrivilegeLoader;
import org.apache.shardingsphere.authority.model.PrivilegeType;
import org.apache.shardingsphere.authority.model.ShardingSpherePrivileges;
import org.apache.shardingsphere.authority.model.database.SchemaPrivileges;
import org.apache.shardingsphere.infra.metadata.user.ShardingSphereUser;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.infra.spi.typed.TypedSPIRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class SQLServerPrivilegeLoaderTest {

    @BeforeClass
    public static void setUp() {
        ShardingSphereServiceLoader.register(StoragePrivilegeLoader.class);
    }

    @Test
    public void assertLoad() throws SQLException {
        Collection<ShardingSphereUser> users = createUsers();
        DataSource dataSource = mockDataSource(users);
        assertPrivileges(TypedSPIRegistry.getRegisteredService(StoragePrivilegeLoader.class, "SQLServer", new Properties()).load(users, dataSource));
    }

    private void assertPrivileges(final Map<ShardingSphereUser, ShardingSpherePrivileges> actual) {
        assertThat(actual.size(), is(1));
        ShardingSphereUser user = new ShardingSphereUser("dbo", "", "");
        assertThat(actual.get(user).getDatabasePrivileges().getGlobalPrivileges().size(), is(0));
        assertThat(actual.get(user).getDatabasePrivileges().getSpecificPrivileges().size(), is(1));
        Collection<PrivilegeType> expectedSpecificPrivilege = new CopyOnWriteArraySet(Arrays.asList(PrivilegeType.INSERT, PrivilegeType.SELECT, PrivilegeType.UPDATE,
                PrivilegeType.DELETE));
        SchemaPrivileges schemaPrivileges = actual.get(user).getDatabasePrivileges().getSpecificPrivileges().get("db0");
        assertThat(schemaPrivileges.getSpecificPrivileges().get("t_order").hasPrivileges(expectedSpecificPrivilege), is(true));
    }

    private Collection<ShardingSphereUser> createUsers() {
        LinkedList<ShardingSphereUser> result = new LinkedList<>();
        result.add(new ShardingSphereUser("dbo", "", ""));
        return result;
    }

    private DataSource mockDataSource(final Collection<ShardingSphereUser> users) throws SQLException {
        ResultSet tablePrivilegeResultSet = mockTablePrivilegeResultSet();
        DataSource result = mock(DataSource.class, RETURNS_DEEP_STUBS);
        String tablePrivilegeSql = "SELECT GRANTOR, GRANTEE, TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, PRIVILEGE_TYPE, IS_GRANTABLE from INFORMATION_SCHEMA.TABLE_PRIVILEGES WHERE GRANTEE IN (%s)";
        String userList = users.stream().map(item -> String.format("'%s'", item.getGrantee().getUsername())).collect(Collectors.joining(", "));
        when(result.getConnection().createStatement().executeQuery(String.format(tablePrivilegeSql, userList))).thenReturn(tablePrivilegeResultSet);
        return result;
    }

    private ResultSet mockTablePrivilegeResultSet() throws SQLException {
        ResultSet result = mock(ResultSet.class, RETURNS_DEEP_STUBS);
        when(result.next()).thenReturn(true, true, true, true, true, true, true, false);
        when(result.getString("TABLE_CATALOG")).thenReturn("db0");
        when(result.getString("TABLE_NAME")).thenReturn("t_order");
        when(result.getString("PRIVILEGE_TYPE")).thenReturn("INSERT", "SELECT", "UPDATE", "DELETE", "REFERENCES");
        when(result.getString("IS_GRANTABLE")).thenReturn("YES", "YES", "YES", "YES", "YES", "YES", "YES");
        when(result.getString("GRANTEE")).thenReturn("dbo");
        return result;
    }
}
