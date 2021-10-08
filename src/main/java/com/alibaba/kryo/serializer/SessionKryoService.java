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

public class SessionKryoService {

    public static SessionKryoFactory newSingletonSessionKryoFactory(ReadWriteSessionDataLocator readWriteSessionDataLocator, CreateOption createOption) {
        return new SessionKryoFactory() {
            private SessionKryo sessionKryo = createSessionKryo(readWriteSessionDataLocator, createOption);

            @Override
            public <T> SessionKryo get(T locateKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SessionKryo get() {
                return sessionKryo;
            }
        };
    }

    public static SessionKryoFactory newThreadLocalSessionKryoFactory(ReadWriteSessionDataLocator readWriteSessionDataLocator, CreateOption createOption) {
        return new SessionKryoFactory() {
            @Override
            public <T> SessionKryo get(T locateKey) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SessionKryo get() {
                SessionKryo kryo = KryoThreadLocal.get();
                if (kryo == null) {
                    kryo = createSessionKryo(readWriteSessionDataLocator, createOption);
                    KryoThreadLocal.set(kryo);
                }
                return kryo;
            }
        };
    }

    public static SessionKryoFactory newCachedSessionKryoFactory(CacheSessionKryoPolicy cachePolicy, ReadWriteSessionDataLocator readWriteSessionDataLocator, CreateOption createOption) {
        return new SessionKryoFactory() {
            @Override
            public <T> SessionKryo get(T locateKey) {
                SessionKryo kryo = cachePolicy.get(locateKey);
                if (kryo == null) {
                    kryo = createSessionKryo(readWriteSessionDataLocator, createOption);
                    cachePolicy.put(locateKey, kryo);
                }
                return kryo;
            }

            @Override
            public SessionKryo get() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static SessionKryo createSessionKryo(ReadWriteSessionDataLocator readWriteSessionDataLocator, CreateOption createOption) {
        SessionKryo sessionKryo = new SessionKryo(readWriteSessionDataLocator, createOption);
        return sessionKryo;
    }


    private static class KryoThreadLocal {
        private static final ThreadLocal<SessionKryo> context = new ThreadLocal<>();

        public static void set(SessionKryo sessionalKryo) {
            context.set(sessionalKryo);
        }

        public static SessionKryo get() {
            return context.get();
        }
    }
}
