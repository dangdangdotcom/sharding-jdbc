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

package org.apache.shardingsphere.core.rewrite.parameterized.jaxb.entity;

import lombok.Getter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Encrypt rewrite assertion entity for JAXB.
 *
 * @author zhangliang
 */
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public final class EncryptRewriteAssertionEntity {
    
    @XmlAttribute(required = true)
    private String id;
    
    @XmlAttribute(required = true)
    private String sql;
    
    @XmlAttribute
    private String parameters;
    
    @XmlAttribute(name = "rewrited-sql", required = true)
    private String rewritedSQL;
    
    @XmlAttribute(name = "rewrited-parameters")
    private String rewritedParameters;
    
    @XmlAttribute(name = "db-type")
    private String databaseType;
}
