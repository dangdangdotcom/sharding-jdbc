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

package org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.segment.impl.rdl;

import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.segment.AbstractExpectedIdentifierSQLSegment;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.segment.impl.ExceptedLoadBalancer;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.segment.impl.ExpectedProperties;
import org.apache.shardingsphere.distsql.parser.api.sql.jaxb.cases.domain.segment.impl.ExpectedDataSourceName;

import javax.xml.bind.annotation.XmlElement;
import java.util.Collection;

@Getter
@Setter
public final class ExceptedReadwriteSplittingRule extends AbstractExpectedIdentifierSQLSegment {
    
    @XmlElement
    private String name;

    @XmlElement(name = "auto-aware-resource")
    private ExpectedDataSourceName autoAwareResource;

    @XmlElement(name = "write-data-resource")
    private ExpectedDataSourceName writeDataSource;

    @XmlElement(name = "read-data-source")
    private Collection<ExpectedDataSourceName> readDataSources;

    @XmlElement(name = "load-balancer")
    private ExceptedLoadBalancer loadBalancer;

    @XmlElement
    private ExpectedProperties props;
}
