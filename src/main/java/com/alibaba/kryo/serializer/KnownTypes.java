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
package com.alibaba.kryo.serializer;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer;
import com.esotericsoftware.kryo.kryo5.util.IdentityObjectIntMap;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

class KnownTypes {
    final static IdentityObjectIntMap<Class> class2Code = new IdentityObjectIntMap<>();

    final static Class[] KNOW_CLASSES = new Class[]{
            boolean.class, Boolean.class, byte.class, Byte.class, char.class,
            Character.class, short.class, Short.class, int.class, Integer.class,
            long.class, Long.class, float.class, Float.class, double.class, Double.class,
            String.class, BigInteger.class, BigDecimal.class, Date.class, Timestamp.class,
            java.sql.Date.class, Time.class, Currency.class, StringBuffer.class,
            StringBuilder.class, TimeZone.class, Calendar.class, Locale.class, Charset.class, URL.class
    };

    public final static int NOT_KNOWN = 0;

    /**
     * index 0 : not used.
     */
    final FieldSerializer.CachedField code2Serializer[] = new FieldSerializer.CachedField[KNOW_CLASSES.length + 1];

    static {
        //code muse start not start with 0,0 is a special code indicate not a known type.
        int code = 1;
        for (Class clazz : KNOW_CLASSES) {
            class2Code.put(clazz, code++);
        }
    }

    public KnownTypes(Kryo kryo) {
        FieldSerializer<YummyObject> serializer = new FieldSerializer(kryo, YummyObject.class);
        FieldSerializer.CachedField[] fields = serializer.getFields();
        for (FieldSerializer.CachedField field : fields) {
            code2Serializer[class2Code.get(field.getField().getType(), -1)] = field;
        }
    }
}
