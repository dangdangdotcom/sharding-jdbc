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

package org.apache.shardingsphere.sql.parser.core.parser;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.apache.shardingsphere.core.spi.NewInstanceServiceLoader;
import org.apache.shardingsphere.spi.database.BranchDatabaseType;
import org.apache.shardingsphere.spi.database.DatabaseType;
import org.apache.shardingsphere.sql.parser.api.SQLParser;
import org.apache.shardingsphere.sql.parser.spi.SQLParserEntry;

import java.util.Collection;
import java.util.HashSet;

/**
 * SQL parser factory.
 * 
 * @author duhongjun
 * @author zhangliang
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SQLParserFactory {
    
    private static final Collection<String> DATABASE_TYPE_NAMES = new HashSet<>();
    
    static {
        NewInstanceServiceLoader.register(SQLParserEntry.class);
        for (SQLParserEntry each : NewInstanceServiceLoader.newServiceInstances(SQLParserEntry.class)) {
            DATABASE_TYPE_NAMES.add(each.getDatabaseTypeName());
        }
    }
    
    /**
     * Get add on name of database types.
     * 
     * @return add on name of database types
     */
    public static Collection<String> getAddOnDatabaseTypeNames() {
        return DATABASE_TYPE_NAMES;
    }
    
    /** 
     * New instance of SQL parser.
     * 
     * @param databaseType database type
     * @param sql SQL
     * @return SQL parser
     */
    public static SQLParser newInstance(final DatabaseType databaseType, final String sql) {
        for (SQLParserEntry each : NewInstanceServiceLoader.newServiceInstances(SQLParserEntry.class)) {
            if (each.getDatabaseTypeName().equals(getDatabaseTypeName(databaseType))) {
                return createSQLParser(sql, each);
            }
        }
        throw new UnsupportedOperationException(String.format("Cannot support database type '%s'", databaseType));
    }
    
    private static String getDatabaseTypeName(final DatabaseType databaseType) {
        if (databaseType instanceof BranchDatabaseType) {
            return ((BranchDatabaseType) databaseType).getTrunkDatabaseType().getName();
        }
        return databaseType.getName();
    }
    
    @SneakyThrows
    private static SQLParser createSQLParser(final String sql, final SQLParserEntry parserEntry) {
        Lexer lexer = parserEntry.getLexerClass().getConstructor(CharStream.class).newInstance(CharStreams.fromString(sql));
        return parserEntry.getParserClass().getConstructor(TokenStream.class).newInstance(new CommonTokenStream(lexer));
    }
}
