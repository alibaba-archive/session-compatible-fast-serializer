/*
 * Copyright 1999-2021 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.kryo.serializer.object.misc;

import java.io.Serializable;
import java.util.Arrays;

public class FooArray {
    private Object[] objects;
    private Serializable[] serializableArray;

    public Object[] getObjects() {
        return objects;
    }

    public void setObjects(Object[] objects) {
        this.objects = objects;
    }

    public Serializable[] getSerializableArray() {
        return serializableArray;
    }

    public void setSerializableArray(Serializable[] serializableArray) {
        this.serializableArray = serializableArray;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FooArray fooArray = (FooArray) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(objects, fooArray.objects)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(serializableArray, fooArray.serializableArray);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(objects);
        result = 31 * result + Arrays.hashCode(serializableArray);
        return result;
    }
}
