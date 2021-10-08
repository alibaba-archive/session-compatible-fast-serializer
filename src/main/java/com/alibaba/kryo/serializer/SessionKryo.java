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

import com.alibaba.kryo.serializer.annotation.SingleThreadLevelShare;
import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.SerializerFactory;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.util.IdentityObjectIntMap;
import com.esotericsoftware.kryo.kryo5.util.MapReferenceResolver;

import java.util.PriorityQueue;

@SingleThreadLevelShare
public class SessionKryo {
    private final ReadWriteSessionDataLocator readWriteSessionDataLocator;
    private final Kryo kryo;
    private final CreateOption createOption;
    KnownTypes knownTypes;
    ReadWriteSessionData sessionData;
    final YummyObject yummyObject = new YummyObject();
    private final CachedClassResolver cachedClassResolver;
    private final IdentityObjectIntMap<Class> classToSize = new IdentityObjectIntMap<>();
    private static final int DEFAULT_OUTPUT_SIZE = 4096;

    public SessionKryo(ReadWriteSessionDataLocator readWriteSessionDataLocator, CreateOption createOption) {
        this.createOption = createOption;
        cachedClassResolver = new CachedClassResolver(this);
        this.readWriteSessionDataLocator = readWriteSessionDataLocator;
        kryo = new Kryo(cachedClassResolver, new MapReferenceResolver());
        //handle missing no-arg constructor issue
        kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        //for compatible: must turn off optimized generics
        //for example: Set<Long> set; but put Long and integer to set
        //hessian2 can serialize/deserialize,but kryo will not if not turn off this flag
        kryo.setOptimizedGenerics(false);
        kryo.setDefaultSerializer(new OffsetSkipableCachedCompatibleFieldSerializerFactory(createOption.supportSameFieldNameWithSuper));
        kryo.addDefaultSerializer(PriorityQueue.class, new CanEmptyPriorityQueueSerializer());
        this.knownTypes = new KnownTypes(kryo);
    }

    public <T> byte[] writeClassAndObject(T sessionLocateKey, Object object) {
        try {
            sessionData = findSessionData(sessionLocateKey);
            Output output = createOutput(object);
            kryo.writeClassAndObject(output, object);
            if (object != null) {
                recordClassSize(object.getClass(), (int) output.total());
            }
            return output.toBytes();
        } finally {
            this.sessionData.resetWriteData();
            this.sessionData = null;
        }
    }

    public <T> byte[] writeObject(T sessionLocateKey, Object object) {
        try {
            sessionData = findSessionData(sessionLocateKey);
            Output output = createOutput(object);
            kryo.writeObject(output, object);
            if (object != null) {
                recordClassSize(object.getClass(), (int) output.total());
            }
            return output.toBytes();
        } finally {
            sessionData.resetWriteData();
            sessionData = null;
        }
    }

    public <T> Object readClassAndObject(T sessionLocateKey, byte[] buffer) {
        try {
            sessionData = findSessionData(sessionLocateKey);
            Input input = new Input(buffer);
            return kryo.readClassAndObject(input);
        } finally {
            sessionData = null;
        }
    }

    public <T> T readObject(T sessionLocateKey, byte[] buffer, Class<T> type) {
        try {
            sessionData = findSessionData(sessionLocateKey);
            Input input = new Input(buffer);
            return kryo.readObject(input, type);
        } finally {
            sessionData = null;
        }
    }

    private <T> ReadWriteSessionData findSessionData(T sessionLocateKey) {
        ReadWriteSessionData sessionData = readWriteSessionDataLocator.find(sessionLocateKey);
        if (sessionData == null) {
            sessionData = new ReadWriteSessionData();
            readWriteSessionDataLocator.newCreated(sessionLocateKey, sessionData);
        }
        return sessionData;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.kryo.setClassLoader(classLoader);
    }

    static class OffsetSkipableCachedCompatibleFieldSerializerFactory extends SerializerFactory.BaseSerializerFactory<FastCachedCompatibleFieldSerializer> {
        private final FastCachedCompatibleFieldSerializer.CachedCompatibleFieldSerializerConfig config;

        public OffsetSkipableCachedCompatibleFieldSerializerFactory(boolean supportSameFieldNameWithSuper) {
            this.config = new FastCachedCompatibleFieldSerializer.CachedCompatibleFieldSerializerConfig();
            if (supportSameFieldNameWithSuper) {
                this.config.setExtendedFieldNames(true);
            }
        }

        public FastCachedCompatibleFieldSerializer.CachedCompatibleFieldSerializerConfig getConfig() {
            return config;
        }

        public FastCachedCompatibleFieldSerializer newSerializer(Kryo kryo, Class type) {
            return new FastCachedCompatibleFieldSerializer(kryo, type, config.clone());
        }
    }

    private Output createOutput(Object o) {
        if (o != null) {
            return new Output(getClassOutputSize(o.getClass()), createOption.maxOutputBufferSize);
        } else {
            return new Output(8, createOption.maxOutputBufferSize);
        }
    }

    private void recordClassSize(Class type, int size) {
        if (size > 0) {
            classToSize.put(type, size);
        }
    }

    private int getClassOutputSize(Class type) {
        return classToSize.get(type, DEFAULT_OUTPUT_SIZE);
    }

    /**
     * java.util.PriorityQueue cannot accept a size < 1,so it throw exception when read a empty PriorityQueue.
     * Handle this is simple,always make sure not pass a size <1 to constructor.
     */
    public static class CanEmptyPriorityQueueSerializer extends DefaultSerializers.PriorityQueueSerializer {
        @Override
        protected PriorityQueue create(Kryo kryo, Input input, Class<? extends PriorityQueue> type, int size) {
            return super.create(kryo, input, type, size < 1 ? 1 : size);
        }
    }
}
