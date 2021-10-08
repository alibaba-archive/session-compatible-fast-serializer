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

public class HelloSessionKryo {
    public static void main(String[] args) {
        SessionKryo sessionKryo = SessionKryoService.newSingletonSessionKryoFactory(new ReadWriteSessionDataLocator() {
            private ReadWriteSessionData readWriteSessionData;

            public ReadWriteSessionData find(Object locateKey) {
                return readWriteSessionData;
            }

            public void newCreated(Object locateKey, ReadWriteSessionData newReadWriteSessionData) {
                readWriteSessionData = newReadWriteSessionData;
            }
        }, CreateOption.of()).get();

        FooData fooData = new FooData();
        fooData.a = "this is a test";
        fooData.b = 199;
        fooData.c = 121313L;

        byte[] b = sessionKryo.writeClassAndObject(null, fooData);
        System.out.println("Write Foo: " + fooData);
        FooData fooDataRead = (FooData) sessionKryo.readClassAndObject(null, b);
        System.out.println("Read Foo:" + fooDataRead);
    }

    private static class FooData {
        String a;
        int b;
        long c;

        public String toString() {
            return "FooData{" + "a='" + a + '\'' + ", b=" + b + ", c=" + c + '}';
        }
    }
}
