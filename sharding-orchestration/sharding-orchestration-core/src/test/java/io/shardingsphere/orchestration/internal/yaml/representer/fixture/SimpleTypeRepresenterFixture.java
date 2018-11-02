/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.orchestration.internal.yaml.representer.fixture;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collection;

@Getter
@Setter
public final class SimpleTypeRepresenterFixture {
    
    private boolean booleanValue;
    
    private Boolean booleanObjectValue = Boolean.TRUE;
    
    private int intValue;
    
    private Integer integerObjectValue = 10;
    
    private long longValue;
    
    private Long longObjectValue = 10L;
    
    private String string = "value";
    
    private Collection<String> collection = Arrays.asList("value1", "value2");
    
    private String skippedProperty = "skipped";
}
