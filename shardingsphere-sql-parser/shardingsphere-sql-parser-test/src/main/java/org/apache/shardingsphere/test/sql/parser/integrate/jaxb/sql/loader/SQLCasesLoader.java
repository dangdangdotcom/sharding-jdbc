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

package org.apache.shardingsphere.test.sql.parser.integrate.jaxb.sql.loader;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import lombok.SneakyThrows;
import org.apache.shardingsphere.test.sql.parser.integrate.jaxb.sql.SQLCase;
import org.apache.shardingsphere.test.sql.parser.integrate.jaxb.sql.SQLCaseType;
import org.apache.shardingsphere.test.sql.parser.integrate.jaxb.sql.SQLCases;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * SQL test cases loader.
 */
public final class SQLCasesLoader {
    
    private final Map<String, SQLCase> sqlCases;
    
    public SQLCasesLoader(final String rootDirection) {
        sqlCases = loadSQLCases(rootDirection);
    }
    
    @SneakyThrows({JAXBException.class, IOException.class})
    private Map<String, SQLCase> loadSQLCases(final String path) {
        File file = new File(SQLCasesLoader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        return file.isFile() ? loadSQLCasesFromJar(path, file) : loadSQLCasesFromTargetDirectory(path);
    }
    
    private Map<String, SQLCase> loadSQLCasesFromJar(final String path, final File file) throws IOException, JAXBException {
        Map<String, SQLCase> result = new TreeMap<>();
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path + "/") && name.endsWith(".xml")) {
                    fillSQLMap(result, SQLCasesLoader.class.getClassLoader().getResourceAsStream(name));
                }
            }
        }
        return result;
    }
    
    @SneakyThrows(URISyntaxException.class)
    private Map<String, SQLCase> loadSQLCasesFromTargetDirectory(final String path) {
        Map<String, SQLCase> result = new TreeMap<>();
        URL url = SQLCasesLoader.class.getClassLoader().getResource(path);
        if (null == url) {
            return result;
        }
        File filePath = new File(url.toURI().getPath());
        if (!filePath.exists()) {
            return result;
        }
        File[] files = filePath.listFiles();
        if (null == files) {
            return result;
        }
        for (File each : files) {
            loadSQLCasesFromDirectory(result, each);
        }
        return result;
    }
    
    @SneakyThrows({JAXBException.class, IOException.class})
    private void loadSQLCasesFromDirectory(final Map<String, SQLCase> sqlStatementMap, final File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (null == files) {
                return;
            }
            for (File each : files) {
                loadSQLCasesFromDirectory(sqlStatementMap, each);
            }
        } else {
            fillSQLMap(sqlStatementMap, new FileInputStream(file));
        }
    }
    
    private void fillSQLMap(final Map<String, SQLCase> sqlCaseMap, final InputStream inputStream) throws JAXBException {
        SQLCases sqlCases = (SQLCases) JAXBContext.newInstance(SQLCases.class).createUnmarshaller().unmarshal(inputStream);
        for (SQLCase each : sqlCases.getSqlCases()) {
            if (null == each.getDatabaseTypes()) {
                each.setDatabaseTypes(sqlCases.getDatabaseTypes());
            }
            Preconditions.checkState(!sqlCaseMap.containsKey(each.getId()), "Find duplicated SQL Case ID: %s", each.getId());
            sqlCaseMap.put(each.getId(), each);
        }
    }
    
    /**
     * Get SQL.
     *
     * @param sqlCaseId SQL case ID
     * @param sqlCaseType SQL case type
     * @param parameters SQL parameters
     * @return SQL
     */
    public String getSQL(final String sqlCaseId, final SQLCaseType sqlCaseType, final List<?> parameters) {
        switch (sqlCaseType) {
            case Literal:
                return getLiteralSQL(getSQLFromMap(sqlCaseId, sqlCases), parameters);
            case Placeholder:
                return getPlaceholderSQL(getSQLFromMap(sqlCaseId, sqlCases));
            default:
                throw new UnsupportedOperationException(sqlCaseType.name());
        }
    }
    
    private String getSQLFromMap(final String id, final Map<String, SQLCase> sqlCaseMap) {
        Preconditions.checkState(sqlCaseMap.containsKey(id), "Can't find SQL of id: " + id);
        SQLCase statement = sqlCaseMap.get(id);
        return statement.getValue();
    }
    
    private String getPlaceholderSQL(final String sql) {
        return sql.replace("%%", "%").replace("'%'", "'%%'");
    }
    
    private String getLiteralSQL(final String sql, final List<?> parameters) {
        if (null == parameters || parameters.isEmpty()) {
            return sql;
        }
        return String.format(sql.replace("%", "$").replace("?", "%s"), parameters.toArray()).replace("$", "%")
                .replace("%%", "%").replace("'%'", "'%%'");
    }
    
    /**
     * Get test parameters for junit parameterized test cases.
     *
     * @param databaseTypes database types
     * @return test parameters for junit parameterized test cases
     */
    public Collection<Object[]> getSQLTestParameters(final Collection<String> databaseTypes) {
        Collection<Object[]> result = new LinkedList<>();
        for (SQLCase each : sqlCases.values()) {
            result.addAll(getTestParameters(databaseTypes, each));
        }
        return result;
    }
    
    private Collection<Object[]> getTestParameters(final Collection<String> databaseTypes, final SQLCase sqlCase) {
        Collection<Object[]> result = new LinkedList<>();
        for (SQLCaseType each : SQLCaseType.values()) {
            result.addAll(getTestParameters(databaseTypes, sqlCase, each));
        }
        return result;
    }
    
    private static Collection<Object[]> getTestParameters(final Collection<String> databaseTypes, final SQLCase sqlCase, final SQLCaseType sqlCaseType) {
        Collection<Object[]> result = new LinkedList<>();
        for (String each : getDatabaseTypes(sqlCase.getDatabaseTypes())) {
            if (databaseTypes.contains(each)) {
                Object[] parameters = new Object[3];
                parameters[0] = sqlCase.getId();
                parameters[1] = each;
                parameters[2] = sqlCaseType;
                result.add(parameters);
            }
        }
        return result;
    }
    
    private static Collection<String> getDatabaseTypes(final String databaseTypes) {
        return Strings.isNullOrEmpty(databaseTypes) ? getALlDatabaseTypes() : Splitter.on(',').trimResults().splitToList(databaseTypes);
    }
    
    private static Collection<String> getALlDatabaseTypes() {
        return Arrays.asList("H2", "MySQL", "PostgreSQL", "Oracle", "SQLServer", "SQL92");
    }
    
    /**
     * Get all SQL case IDs.
     * 
     * @return all SQL case IDs
     */
    public Collection<String> getAllSQLCaseIDs() {
        return sqlCases.keySet();
    }
}
