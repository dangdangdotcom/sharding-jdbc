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

package org.apache.shardingsphere.sql.parser.integrate.jaxb.impl.projection;

import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.sql.parser.integrate.jaxb.generic.AbstractExpectedSegment;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@Setter
public final class ExpectedProjections extends AbstractExpectedSegment {
    
    @Getter
    @XmlAttribute(name = "distinct-row")
    private boolean distinctRow;
    
    @XmlElement(name = "shorthand-projection")
    private Collection<ExpectedShorthandProjection> shorthandProjections = new LinkedList<>();
    
    @XmlElement(name = "column-projection")
    private Collection<ExpectedColumnProjection> columnProjections = new LinkedList<>();
    
    @XmlElement(name = "aggregation-projection")
    private Collection<ExpectedAggregationProjection> aggregationProjections = new LinkedList<>();
    
    @XmlElement(name = "aggregation-distinct-projection")
    private Collection<ExpectedAggregationDistinctProjection> aggregationDistinctProjections = new LinkedList<>();
    
    @XmlElement(name = "expression-projection")
    private Collection<ExpectedExpressionProjection> expressionProjections = new LinkedList<>();
    
    @XmlElement(name = "top-projection")
    private Collection<ExpectedTopProjection> topProjections = new LinkedList<>();
    
    /**
     * Get size.
     * 
     * @return size
     */
    public int getSize() {
        return shorthandProjections.size() + columnProjections.size() + aggregationProjections.size() + aggregationDistinctProjections.size() + expressionProjections.size() + topProjections.size();
    }
    
    /**
     * Get expected projections.
     *
     * @return expected projections
     */
    public List<ExpectedProjection> getExpectedProjections() {
        List<ExpectedProjection> result = new LinkedList<>();
        result.addAll(shorthandProjections);
        result.addAll(columnProjections);
        result.addAll(aggregationProjections);
        result.addAll(aggregationDistinctProjections);
        result.addAll(expressionProjections);
        result.addAll(topProjections);
        Collections.sort(result, new Comparator<ExpectedProjection>() {
            
            @Override
            public int compare(final ExpectedProjection o1, final ExpectedProjection o2) {
                return o1.getStartIndex() - o2.getStartIndex();
            }
        });
        return result;
    }
}
