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

import com.alibaba.kryo.serializer.object.large.LargeDO;
import com.alibaba.kryo.serializer.object.large.Conq4;
import com.alibaba.kryo.serializer.object.misc.FinalFoo;
import com.alibaba.kryo.serializer.object.misc.Foo2;
import com.alibaba.kryo.serializer.object.misc.FooArray;
import com.alibaba.kryo.serializer.object.small.class1.User;
import com.alibaba.kryo.serializer.object.small.class2.Height;
import com.alibaba.kryo.serializer.object.small.class2.Hometown;
import com.alibaba.kryo.serializer.object.small.class2.People;
import com.alibaba.kryo.serializer.object.small.class3.Network2G;
import com.alibaba.kryo.serializer.object.small.class3.Network3G;
import com.alibaba.kryo.serializer.object.small.class3.Phone;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

public class TestSessionKryo {

    private static final boolean printByteToHex = false;

    private SessionKryoFactory sessionKryoFactory;
    private SessionKryo kryo;
    private String dummySessionKey = "dummySessionKey";
    private ReadWriteSessionDataLocator sessionDataLocator = new ReadWriteSessionDataLocator() {
        private ReadWriteSessionData readWriteSessionData;

        @Override
        public ReadWriteSessionData find(Object locateKey) {
            return readWriteSessionData;
        }

        @Override
        public void newCreated(Object locateKey, ReadWriteSessionData newReadWriteSessionData) {
            readWriteSessionData = newReadWriteSessionData;
        }
    };

    @Before
    public void setup() {
        sessionKryoFactory = SessionKryoService.newSingletonSessionKryoFactory(sessionDataLocator, CreateOption.of());
        kryo = sessionKryoFactory.get();
    }

    @Test
    public void testWriteObject() throws IOException {
        Hometown hometown = new Hometown<String, String>();
        hometown.setProvince("Where??");
        hometown.setAddress("Where?Where?Where?Where?Where?Where?Where?Where?Where?Where?");
        hometown.setT1("foo");
        hometown.setAge(100);
        hometown.setSales(123131);
        hometown.setT2("bar");
        People people = new People("noname1");
        people.setHometown1(hometown);
        people.setHometown2(hometown);
        people.setHeight(Height.LOW);
        runTestForSerialWriteObject(kryo, people, People.class);
    }

    @Test
    public void testWriteClassAndObject() throws IOException {
        Hometown hometown = new Hometown<String, String>();
        hometown.setProvince("Where??");
        hometown.setAddress("Where??");
        hometown.setT1("foo");
        hometown.setT2("bar");
        People people = new People("noname2");
        people.setHometown1(hometown);
        people.setHometown2(hometown);
        people.setHeight(Height.MIDDLE);
        runTestForSerialClassAndObject(kryo, people);
    }

    @Test
    public void testWriteClassAndObject2() throws IOException {
        Hometown hometown = new Hometown<String, String>();
        hometown.setProvince("I don't know");
        hometown.setAddress("Anywhere");
        hometown.setT1("foo2");
        hometown.setT2("bar2");
        runTestForSerialClassAndObject(kryo, hometown);
    }

    @Test
    public void testWriteClassAndObjectLargeDO() throws IOException {
        Conq4 dataDO = LargeDO.create();
        runTestForSerialClassAndObject(kryo, dataDO);
    }

    @Test
    public void testSameObjectDifferentValue() throws IOException {
        Hometown hometown1 = new Hometown<String, String>();
        hometown1.setProvince("I don't know");
        hometown1.setAddress("Anywhere");
        hometown1.setT1("foo");
        hometown1.setT2("bar!");
        People people1 = new People("Tom");
        people1.setHometown1(hometown1);
        people1.setHometown2(hometown1);

        runTestForSerialClassAndObject(kryo, people1);

        Hometown hometown2 = new Hometown<String, String>();
        hometown2.setAddress("I don't know");
        hometown2.setT1("foo bar");
        People people2 = new People("Jerry");
        people2.setHometown1(hometown1);
        people2.setHometown2(hometown2);
        runTestForSerialClassAndObject(kryo, people2);
    }


    @Test
    public void testContainDiffSubType() throws IOException {
        Phone phone1 = new Phone();
        phone1.brand = "Nokia";
        phone1.color = "sliver";
        phone1.weight = 100;
        Network2G network2G = new Network2G();
        network2G.mhz = 1800;
        phone1.network = network2G;

        runTestForSerialClassAndObject(kryo, phone1);

        Phone phone2 = new Phone();
        phone2.brand = "Huawei";
        phone2.color = "black";
        phone2.weight = 100;
        Network3G network3G = new Network3G();
        network3G.foo = "fooooooo";
        network3G.bar = 99111;
        phone2.network = network3G;
        runTestForSerialClassAndObject(kryo, phone2);
    }

    @Test
    public void testWithDefaultInitializer() throws IOException {
        Foo2 foo2 = new Foo2();
        foo2.setName("ang");
        foo2.setMap(null);
        runTestForSerialClassAndObject(kryo, foo2);
    }


    @Test
    public void testObjectWithObjectArray() throws IllegalAccessException {
        class WrapperInner1 {
        }
        class WrapperInner2 {
            WrapperInner1[] wrapperInner1Array;
        }
        class WrapperOuter {
            WrapperInner2 wrapperInner2;
        }
        WrapperOuter wrapperOuter = new WrapperOuter();
        wrapperOuter.wrapperInner2 = new WrapperInner2();
        wrapperOuter.wrapperInner2.wrapperInner1Array = new WrapperInner1[3];
        wrapperOuter.wrapperInner2.wrapperInner1Array[0] = new WrapperInner1();
        wrapperOuter.wrapperInner2.wrapperInner1Array[1] = new WrapperInner1();
        wrapperOuter.wrapperInner2.wrapperInner1Array[2] = new WrapperInner1();
        runTestForSerialEqualByReflection(kryo, wrapperOuter, (a, b) -> {
            WrapperOuter aw = (WrapperOuter) a;
            WrapperOuter bw = (WrapperOuter) b;
            assertTrue(aw.wrapperInner2 != null);
            assertTrue(bw.wrapperInner2 != null);
            assertTrue(aw.wrapperInner2.wrapperInner1Array.length == bw.wrapperInner2.wrapperInner1Array.length);
        });
    }

    @Test
    public void testWithRuntimeException() throws IllegalAccessException {
        RuntimeException runtimeException = new RuntimeException();
        runtimeException.setStackTrace(Thread.currentThread().getStackTrace());
        runTestForSerialEqualByReflection(kryo, runtimeException, (a, b) -> {
            RuntimeException ra = (RuntimeException) a;
            RuntimeException rb = (RuntimeException) b;
            assertEquals(ra.getMessage(), rb.getMessage());
            assertArrayEquals(ra.getStackTrace(), rb.getStackTrace());
        });
    }

    @Test
    public void testObjectArray() throws IllegalAccessException {
        class A {
            String value;
        }
        class B {
            A[] array;
            String c;
        }

        B b = new B();
        b.array = new A[1];
        b.array[0] = new A();
        b.array[0].value = "xxx";
        b.c = "c";
        runTestForSerialEqualByReflection(kryo, b, (ao, bo) -> {
            assertTrue(ao.getClass() == bo.getClass());
            B b1 = (B) ao;
            B b2 = (B) bo;
            assertTrue(b1.c.equals(b2.c));
            assertTrue(b1.array.length == b2.array.length);
            for (int i = 0; i < b1.array.length; i++) {
                assertTrue(b1.array[i].value.equals(b2.array[i].value));
            }
        });
    }

    @Test
    public void testObjectWithSet() throws IllegalAccessException {
        class A {
            Set<Long> numbers;
        }

        A a = new A();
        a.numbers = new HashSet<>();
        a.numbers.add(100L);
        a.numbers.add(200L);
        runTestForSerialEqualByReflection(kryo, a, (ao, bo) -> {
            assert (ao.getClass().equals(bo.getClass()));
        });
    }

    @Test
    public void testFinalFoo() throws IllegalAccessException {
        class A {
            FinalFoo[] finalFoos;
        }

        A a = new A();
        FinalFoo[] finalFoos = new FinalFoo[3];
        finalFoos[0] = new FinalFoo(1000000000L, "a", 1313);
        finalFoos[1] = new FinalFoo(2000000000L, "adfafc", 78);
        finalFoos[2] = new FinalFoo(1123103103L, "13210310", 999);
        a.finalFoos = finalFoos;
        runTestForSerialEqualByReflection(kryo, a, (ao, bo) -> {
            A a1 = (A) ao;
            A a2 = (A) bo;
            assertArrayEquals(a1.finalFoos, a2.finalFoos);
        });
    }


    @Test
    public void testGenericArray() throws IllegalAccessException {
        FooArray fooArray = new FooArray();
        Object[] objects = new Object[2];
        objects[0] = new User();
        Phone phone1 = new Phone();
        phone1.brand = "Nokia";
        phone1.color = "sliver";
        phone1.weight = 100;
        Network2G network2G = new Network2G();
        network2G.mhz = 1800;
        phone1.network = network2G;
        objects[1] = phone1;
        fooArray.setObjects(objects);

        Serializable[] serializables = new Serializable[2];
        serializables[0] = new Integer(10);
        serializables[1] = new Long(999);
        fooArray.setSerializableArray(serializables);
        runTestForSerialEqualByReflection(kryo, fooArray, (a, b) -> {
            assertEquals(a, b);
        });

    }

    @Test
    public void testGenericCollectionMixWithDiffTypes() throws IllegalAccessException {
        class A {
            Set<Long> set;
        }

        A a = new A();
        Set set = new HashSet();
        a.set = set;
        set.add(new Long(100));
        set.add(new Integer(100));
        set.add(Byte.valueOf((byte) 100));
        runTestForSerialEqualByReflection(kryo, a, (ao, bo) -> {
            Set bset = ((A) bo).set;
            assertTrue(bset.contains(new Long(100)));
            assertTrue(bset.contains(new Integer(100)));
            assertTrue(bset.contains(Byte.valueOf((byte) 100)));
        });
    }


    private void runTestForSerialClassAndObject(SessionKryo kryo, Object dataDO) throws IOException {
        byte[] outputBuffer1 = kryo.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(outputBuffer1);

        byte[] outputBuffer2 = kryo.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(outputBuffer2);

        byte[] outputBuffer3 = kryo.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(outputBuffer3);

        Object object = kryo.readClassAndObject(dummySessionKey, outputBuffer1);
        Object object2 = kryo.readClassAndObject(dummySessionKey, outputBuffer2);
        Object object3 = kryo.readClassAndObject(dummySessionKey, outputBuffer3);

        assertTrue(dataDO.equals(object));
        assertTrue(dataDO.equals(object2));
        assertTrue(dataDO.equals(object3));
    }


    private void runTestForSerialWriteObject(SessionKryo kryo, Object dataDO, Class type) throws IOException {
        byte[] outputBuffer1 = kryo.writeObject(dummySessionKey, dataDO);
        bytesToHex(outputBuffer1);

        byte[] outputBuffer2 = kryo.writeObject(dummySessionKey, dataDO);
        bytesToHex(outputBuffer2);

        byte[] outputBuffer3 = kryo.writeObject(dummySessionKey, dataDO);
        bytesToHex(outputBuffer3);

        Object object = kryo.readObject(dummySessionKey, outputBuffer1, type);
        Object object2 = kryo.readObject(dummySessionKey, outputBuffer2, type);
        Object object3 = kryo.readObject(dummySessionKey, outputBuffer3, type);

        assertTrue(dataDO.equals(object));
        assertTrue(dataDO.equals(object2));
        assertTrue(dataDO.equals(object3));

    }

    private void runTestForSerialEqualByReflection(SessionKryo kryo, Object dataDO, BiConsumer equal) throws IllegalAccessException {
        byte[] b1 = kryo.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(b1);
        byte[] b2 = kryo.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(b2);
        byte[] b3 = kryo.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(b3);

        Object object = kryo.readClassAndObject(dummySessionKey, b1);
        Object object2 = kryo.readClassAndObject(dummySessionKey, b2);
        Object object3 = kryo.readClassAndObject(dummySessionKey, b3);

        equal.accept(dataDO, object);
        equal.accept(dataDO, object2);
        equal.accept(dataDO, object3);
    }


    private boolean useReflect(Class<?> aClass) {
        if (aClass.isPrimitive()) {
            return false;
        }
        if (aClass == String.class) {
            return false;
        }

        return true;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static void bytesToHex(byte[] bytes) {
        bytesToHex(bytes, bytes.length);
    }

    public static void bytesToHex(byte[] bytes, int length) {
        if (printByteToHex) {
            System.out.println("\n=====================\n");
            for (int j = 0; j < length; j++) {
                int v = bytes[j] & 0xFF;
                System.out.print(HEX_ARRAY[v >>> 4]);
                System.out.print(HEX_ARRAY[v & 0x0F]);
                System.out.print(' ');
                if ((j + 1) % 16 == 0) {
                    System.out.println();
                }
            }
        }
    }
}
