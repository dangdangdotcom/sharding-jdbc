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

package org.apache.shardingsphere.agent.metrics.api.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Reflective utility.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectiveUtil {
    
    /**
     * Set value to static field.
     *
     * @param target target
     * @param fieldName field name
     * @param value value
     */
    @SneakyThrows(ReflectiveOperationException.class)
    public static void setStaticField(final Class<?> target, final String fieldName, final Object value) {
        Field[] fields = target.getDeclaredFields();
        for (Field each : fields) {
            if (Modifier.isStatic(each.getModifiers()) && each.getName().equalsIgnoreCase(fieldName)) {
                each.setAccessible(true);
                each.set(null, value);
            }
        }
    }

    /**
     * Get field value object.
     *
     * @param object object
     * @param fieldName field name
     * @return object
     */
    public static Object getFieldValue(final Object object, final String fieldName) {
        return getFieldValue(object, getField(object.getClass(), fieldName));
    }
    
    private static Field getField(final Class<?> clazz, final String fieldName) {
        Field[] fields = clazz.getDeclaredFields();
        if (fields.length != 0) {
            for (Field each : fields) {
                if (fieldName.equals(each.getName())) {
                    return each;
                }
            }
        }
        return null;
    }
    
    private static Object getFieldValue(final Object object, final Field field) {
        if (null == object || null == field) {
            return null;
        }
        field.setAccessible(true);
        Object result = null;
        try {
            result = field.get(object);
        } catch (IllegalAccessException ignored) {
        }
        return result;
    }
}
