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

import com.alibaba.kryo.serializer.object.small.class1.User;
import com.alibaba.kryo.serializer.object.small.class2.Hometown;
import com.alibaba.kryo.serializer.object.small.class2.People;
import com.alibaba.kryo.serializer.object.small.class3.Network2G;
import com.alibaba.kryo.serializer.object.small.class3.Phone;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static com.alibaba.kryo.serializer.TestSessionKryo.bytesToHex;
import static org.apache.commons.lang3.reflect.FieldUtils.writeDeclaredField;
import static org.junit.Assert.*;

public class TestCompatible {

    private SessionKryoFactory sessionKryoFactory;
    private SessionKryo writeKryo;
    private SessionKryo readKryo;
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
        sessionKryoFactory = SessionKryoService.newCachedSessionKryoFactory(new CacheSessionKryoPolicy<String>() {
            @Override
            public SessionKryo get(String locateKey) {
                if ("writeKryo".equals(locateKey)) {
                    return writeKryo;
                } else if ("readKryo".equals(locateKey)) {
                    return readKryo;
                } else {
                    return null;
                }
            }

            @Override
            public void put(String locateKey, SessionKryo sessionKryo) {
                if ("writeKryo".equals(locateKey)) {
                    writeKryo = sessionKryo;
                } else if ("readKryo".equals(locateKey)) {
                    readKryo = sessionKryo;
                }
            }
        }, sessionDataLocator, CreateOption.of());
        writeKryo = sessionKryoFactory.get("writeKryo");
        readKryo = sessionKryoFactory.get("readKryo");
    }


    @Test
    public void testCompatibleReadWriteSideDiff() throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        //MemoryClassLoader displayClassLoader= new MemoryClassLoader(getDisplayClass(),MemoryClassLoader.class.getClassLoader());
        MemoryClassLoader writeClassLoader = new MemoryClassLoader(getVersion1Class(), MemoryClassLoader.class.getClassLoader());
        Class writeClass = writeClassLoader.findClass("Computer");
        Object writeObject = writeClass.newInstance();

        setValue1(writeObject);
        writeDeclaredField(writeObject, "timeUnitCommon", TimeUnit.DAYS);
        writeDeclaredField(writeObject, "timeUnitWriteSide", TimeUnit.MINUTES);
        byte[] outputBuffer1 = writeKryo.writeClassAndObject(dummySessionKey, writeObject);

        byte[] outputBuffer2 = writeKryo.writeClassAndObject(dummySessionKey, writeObject);

        MemoryClassLoader readClassLoader = new MemoryClassLoader(getVersion2Class(), MemoryClassLoader.class.getClassLoader());
        Class readClass = readClassLoader.findClass("Computer");
        assertTrue(readClass != writeClass);
        readKryo.setClassLoader(readClassLoader);

        Object readObject1 = readKryo.readClassAndObject(dummySessionKey, outputBuffer1);
        testEqualIgnoreUnknownFields(writeObject, readObject1);

        Object readObject2 = readKryo.readClassAndObject(dummySessionKey, outputBuffer2);
        testEqualIgnoreUnknownFields(writeObject, readObject2);
    }

    @Test
    public void testCompatibleReadWriteSideDiffWithArray() throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        class Foo {
            Object[] objects;
        }
        //MemoryClassLoader displayClassLoader= new MemoryClassLoader(getDisplayClass(),MemoryClassLoader.class.getClassLoader());
        MemoryClassLoader writeClassLoader = new MemoryClassLoader(getVersion1Class(), MemoryClassLoader.class.getClassLoader());
        Class writeClass = writeClassLoader.findClass("Computer");
        Foo foo = new Foo();
        foo.objects = new Object[2];
        Object writeObject1 = writeClass.newInstance();
        Object writeObject2 = writeClass.newInstance();

        setValue1(writeObject1);
        setValue1(writeObject2);
        foo.objects[0] = writeObject1;
        foo.objects[1] = writeObject2;
        byte[] outputBuffer1 = writeKryo.writeClassAndObject(dummySessionKey, foo);
        bytesToHex(outputBuffer1);

        byte[] outputBuffer2 = writeKryo.writeClassAndObject(dummySessionKey, foo);

        MemoryClassLoader readClassLoader = new MemoryClassLoader(getVersion2Class(), MemoryClassLoader.class.getClassLoader());
        Class readClass = readClassLoader.findClass("Computer");
        assertTrue(readClass != writeClass);
        readKryo.setClassLoader(readClassLoader);

        Object readObject1 = readKryo.readClassAndObject(dummySessionKey, outputBuffer1);
        testEqualIgnoreUnknownFields(foo, readObject1);

        Object readObject2 = readKryo.readClassAndObject(dummySessionKey, outputBuffer2);
        testEqualIgnoreUnknownFields(foo, readObject2);
    }


    @Test
    public void testCompatibleReadWriteSideDiffWithFinalArray() throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        MemoryClassLoader writeClassLoader = new MemoryClassLoader(getWriteSideClass(), MemoryClassLoader.class.getClassLoader());
        Class writeComputerClass = writeClassLoader.findClass("Computer");
        Class writeCommonObjectClass = writeClassLoader.findClass("Computer$CommonObject");
        Object writeObject1 = writeComputerClass.newInstance();
        Object writeObject2 = writeComputerClass.newInstance();

        setValue1(writeObject1);
        setValue3(writeObject2);

        Object arrayObject = Array.newInstance(writeComputerClass, 2);
        Array.set(arrayObject, 0, writeObject1);
        Array.set(arrayObject, 1, writeObject2);

        Object commonObject = writeCommonObjectClass.newInstance();
        writeDeclaredField(commonObject, "computers", arrayObject);

        byte[] outputBuffer1 = writeKryo.writeClassAndObject(dummySessionKey, commonObject);
        bytesToHex(outputBuffer1);

        byte[] outputBuffer2 = writeKryo.writeClassAndObject(dummySessionKey, commonObject);

        MemoryClassLoader readClassLoader = new MemoryClassLoader(getReadSideClass(), MemoryClassLoader.class.getClassLoader());
        Class readClass = readClassLoader.findClass("Computer");
        assertTrue(readClass != writeComputerClass);
        readKryo.setClassLoader(readClassLoader);

        Object readObject1 = readKryo.readClassAndObject(dummySessionKey, outputBuffer1);
        testEqualIgnoreUnknownFields(commonObject, readObject1);

        Object readObject2 = readKryo.readClassAndObject(dummySessionKey, outputBuffer2);
        testEqualIgnoreUnknownFields(commonObject, readObject2);
    }

    @Test
    public void testCompatibleReadWriteSideDiffOnlyDefaultType() throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
        //MemoryClassLoader displayClassLoader= new MemoryClassLoader(getDisplayClass(),MemoryClassLoader.class.getClassLoader());
        MemoryClassLoader writeClassLoader = new MemoryClassLoader(compile("MyObject.java", WRITE_MYOBJECT_CLASS), MemoryClassLoader.class.getClassLoader());
        Class writeClass = writeClassLoader.findClass("MyObject");
        Object writeObject = writeClass.newInstance();

        setValueForDefaultType(writeObject);
        byte[] outputBuffer1 = writeKryo.writeClassAndObject(dummySessionKey, writeObject);
        byte[] outputBuffer2 = writeKryo.writeClassAndObject(dummySessionKey, writeObject);

        MemoryClassLoader readClassLoader = new MemoryClassLoader(compile("MyObject.java", READ_MYOBJECT_CLASS), MemoryClassLoader.class.getClassLoader());
        Class readClass = readClassLoader.findClass("MyObject");
        assertTrue(readClass != writeClass);
        readKryo.setClassLoader(readClassLoader);

        bytesToHex(outputBuffer1);
        Object readObject1 = readKryo.readClassAndObject(dummySessionKey, outputBuffer1);
        testEqualIgnoreUnknownFields(writeObject, readObject1);

        Object readObject2 = readKryo.readClassAndObject(dummySessionKey, outputBuffer2);
        testEqualIgnoreUnknownFields(writeObject, readObject2);
    }

    @Test
    public void testObjectWithGenericType() throws IOException, IllegalAccessException {
        class Wrapper {
            public Wrapper(Object value) {
                this.value = value;
            }

            Object value;
        }
        runTestForSerialEqualByReflection(writeKryo, new Wrapper(1000));
        runTestForSerialEqualByReflection(writeKryo, new Wrapper("gangnga"));
    }

    @Test
    public void testObjectWithSerializableField() throws IOException, IllegalAccessException {
        class Wrapper {
            public Wrapper(Serializable value, String name) {
                this.value = value;
                this.name = name;
            }

            Serializable value;
            String name;
        }

        runTestForSerialEqualByReflection(writeKryo, new Wrapper(789, "abc"));
        runTestForSerialEqualByReflection(writeKryo, new Wrapper(1234.12, "kkkk"));
    }


    @Test
    public void testFieldIsObjectType() throws IllegalAccessException {
        class A {
            Object o;
        }
        A aWithString = new A();
        aWithString.o = "This is a test!";

        A aWithUser = new A();
        User user = new User();
        user.setId(1321310310331L);
        user.setAddress("Hangzhou");
        aWithUser.o = user;

        A aWithPeople = new A();
        Hometown hometown = new Hometown<String, String>();
        hometown.setProvince("Where");
        hometown.setAddress("It's a secret");
        hometown.setT1("A very long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long string!");
        hometown.setAge(100);
        hometown.setSales(123131);
        hometown.setT2("bar!");
        People people = new People("John");
        people.setHometown1(hometown);
        people.setHometown2(hometown);
        aWithPeople.o = people;

        A aWithPhone = new A();
        Phone phone1 = new Phone();
        phone1.brand = "Nokia";
        phone1.color = "sliver";
        phone1.weight = 100;
        Network2G network2G = new Network2G();
        network2G.mhz = 1800;
        phone1.network = network2G;
        aWithPhone.o = phone1;

        runTestForSerialEqualByReflection(writeKryo, readKryo, aWithString, (ao, bo) -> {
            String a = (String) (((A) ao).o);
            String b = (String) (((A) bo).o);
            assertEquals(a, b);
        });
        runTestForSerialEqualByReflection(writeKryo, readKryo, aWithUser, (ao, bo) -> {
            User a = (User) (((A) ao).o);
            User b = (User) (((A) bo).o);
            assertEquals(a, b);
        });
        runTestForSerialEqualByReflection(writeKryo, readKryo, aWithPeople, (ao, bo) -> {
            People a = (People) (((A) ao).o);
            People b = (People) (((A) bo).o);
            assertEquals(a, b);
        });
        runTestForSerialEqualByReflection(writeKryo, readKryo, aWithPhone, (ao, bo) -> {
            Phone a = (Phone) (((A) ao).o);
            Phone b = (Phone) (((A) bo).o);
            assertEquals(a, b);
        });
    }


    @Test
    public void testCompatibleDiffLayout() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        MemoryClassLoader classLoader1 = new MemoryClassLoader(getVersion1Class(), MemoryClassLoader.class.getClassLoader());
        Class writeClass1 = classLoader1.findClass("Computer");
        Object writeObject1 = writeClass1.newInstance();
        setValue1(writeObject1);

        writeKryo.setClassLoader(classLoader1);
        runTestForSerialEqualByReflection(writeKryo, writeObject1);

        MemoryClassLoader classLoader2 = new MemoryClassLoader(getVersion2Class(), MemoryClassLoader.class.getClassLoader());
        writeKryo.setClassLoader(classLoader2);
        Class writeClass2 = classLoader2.findClass("Computer");
        Object writeObject2 = writeClass2.newInstance();
        setValue2(writeObject2);

        writeKryo.setClassLoader(classLoader2);
        runTestForSerialEqualByReflection(writeKryo, writeObject2);
    }

    @Test
    public void testSameFieldWithParentClass() throws IllegalAccessException {
        class Base {
            private String value;
        }
        class Derived1 extends Base {
            private String value;
        }
        class Derived2 extends Derived1 {
            private String value;
        }
        Derived2 derived2 = new Derived2();
        ((Base) derived2).value = "Base";
        ((Derived1) derived2).value = "Derived1";
        derived2.value = "Derived2";
        runTestForSerialEqualByReflection(writeKryo, readKryo, derived2, (ao, bo) -> {
            Derived2 boDerived2 = (Derived2) bo;
            assertEquals(boDerived2.value, "Derived2");
            assertEquals(((Derived1) boDerived2).value, "Derived1");
            assertEquals(((Base) derived2).value, "Base");
        });
    }

    private void runTestForSerialEqualByReflection(SessionKryo kryo, Object dataDO) throws IllegalAccessException {
        byte[] b1 = kryo.writeClassAndObject(dummySessionKey, dataDO);
        byte[] b2 = kryo.writeClassAndObject(dummySessionKey, dataDO);
        byte[] b3 = kryo.writeClassAndObject(dummySessionKey, dataDO);

        Object object = kryo.readClassAndObject(dummySessionKey, b1);
        Object object2 = kryo.readClassAndObject(dummySessionKey, b2);
        Object object3 = kryo.readClassAndObject(dummySessionKey, b3);

        testEqualIgnoreUnknownFields(dataDO, object);
        testEqualIgnoreUnknownFields(dataDO, object2);
        testEqualIgnoreUnknownFields(dataDO, object3);
    }


    private Map<String, byte[]> getVersion1Class() throws IOException {
        return compile("Computer.java", Version1_COMPUTER_CLASS);
    }

    private Map<String, byte[]> getWriteSideClass() throws IOException {
        Map<String, byte[]> classToBytes = compile("Computer.java", WRITE_FINAL_COMPUTER_CLASS);
        return classToBytes;
    }

    private Map<String, byte[]> getReadSideClass() throws IOException {
        Map<String, byte[]> classToByte = compile("Computer.java", READ_FINAL_COMPUTER_CLASS);
        return classToByte;
    }

    private Map<String, byte[]> getVersion2Class() throws IOException {
        return compile("Computer.java", Version2_COMPUTER_CLASS);
    }

    private Map<String, byte[]> compile(String fileName, String source) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager stdManager = compiler.getStandardFileManager(null, null, null);
        try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)) {
            JavaFileObject javaFileObject = manager.makeStringSource(fileName, source);
            JavaCompiler.CompilationTask task = compiler.getTask(null, manager, null, null, null, Arrays.asList(javaFileObject));
            Boolean result = task.call();
            if (result == null || !result.booleanValue()) {
                throw new RuntimeException("Compilation failed.");
            }
            return manager.getClassBytes();
        }

    }

    private void runTestForSerialEqualByReflection(SessionKryo kryoWrite, SessionKryo kryoRead, Object dataDO, BiConsumer equal) throws IllegalAccessException {
        byte[] b1 = kryoWrite.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(b1);

        byte[] b2 = kryoWrite.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(b2);

        byte[] b3 = kryoWrite.writeClassAndObject(dummySessionKey, dataDO);
        bytesToHex(b3);

        Object object = kryoRead.readClassAndObject(dummySessionKey, b1);
        Object object2 = kryoRead.readClassAndObject(dummySessionKey, b2);
        Object object3 = kryoRead.readClassAndObject(dummySessionKey, b3);

        equal.accept(dataDO, object);
        equal.accept(dataDO, object2);
        equal.accept(dataDO, object3);
    }

    private void setValue1(Object o) throws IllegalAccessException {
        writeDeclaredField(o, "modelName", "MacPro");
        writeDeclaredField(o, "memorySizeInMb", new Integer(4096));
        writeDeclaredField(o, "displayModelName", "HP");
        writeDeclaredField(o, "displaySize", new Integer(27));
        writeDeclaredField(o, "keyboardModel", "IBMMBI");
        writeDeclaredField(o, "multiCoreCpu", true);
        writeDeclaredField(o, "amdOrIntel", Character.valueOf('A'));
        writeDeclaredField(o, "diskSize", 256 * 1024 * 1024 * 1024L);
    }

    private void setValue3(Object o) throws IllegalAccessException {
        writeDeclaredField(o, "modelName", "Huawei");
        writeDeclaredField(o, "memorySizeInMb", new Integer(1024));
        writeDeclaredField(o, "displayModelName", "XX");
        writeDeclaredField(o, "displaySize", new Integer(21));
        writeDeclaredField(o, "keyboardModel", "罗技");
        writeDeclaredField(o, "multiCoreCpu", false);
        writeDeclaredField(o, "amdOrIntel", Character.valueOf('B'));
        writeDeclaredField(o, "diskSize", 1024 * 1024 * 1024L);
    }

    private void setValue2(Object o) throws IllegalAccessException {
        writeDeclaredField(o, "memorySizeInMb", new Integer(8192));
        writeDeclaredField(o, "displayModelName", "BenQ");
        writeDeclaredField(o, "keyboardModel", "双飞燕");
        writeDeclaredField(o, "multiCoreCpu", true);
        writeDeclaredField(o, "amdOrIntel", Character.valueOf('A'));
        writeDeclaredField(o, "diskSize", 256 * 1024 * 1024 * 1024L);
        writeDeclaredField(o, "bluetooth", true);
        writeDeclaredField(o, "ssdSize", 64 * 1024 * 1024 * 1024);
    }


    private void setValueForDefaultType(Object o) throws IllegalAccessException, MalformedURLException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(1618997829360L);
        writeDeclaredField(o, "z1", true, true);//boolean
        writeDeclaredField(o, "z2", Boolean.TRUE, true);//Boolean
        writeDeclaredField(o, "b1", (byte) 245, true);//byte
        writeDeclaredField(o, "b2", Byte.valueOf((byte) 120), true);//Byte
        writeDeclaredField(o, "c1", 'x', true);//char
        writeDeclaredField(o, "c2", Character.valueOf('~'), true);//Character
        writeDeclaredField(o, "s1", (short) 8812, true);//short
        writeDeclaredField(o, "s2", Short.valueOf((short) 123), true);//Short
        writeDeclaredField(o, "i1", 123, true);//int
        writeDeclaredField(o, "i2", Integer.valueOf(12312), true);//Integer
        writeDeclaredField(o, "l1", 13210313103311L, true);//long
        writeDeclaredField(o, "l2", Long.valueOf(1231313131313808L), true);//Long
        writeDeclaredField(o, "f1", 0.12345678f, true);//float
        writeDeclaredField(o, "f2", Float.valueOf(1231.1313f), true);//Float
        writeDeclaredField(o, "d1", 13131.99912, true);//double
        writeDeclaredField(o, "d2", Double.valueOf(131313.8899), true);//Double
        writeDeclaredField(o, "string1", "String1", true);//String
        writeDeclaredField(o, "bigInteger1", new BigInteger("1321310381038103810"), true);//BigInteger
        writeDeclaredField(o, "bigDecimal1", new BigDecimal("1310230424204"), true);//BigDecimal
        writeDeclaredField(o, "date1", new Date(1618997829360L), true);//Date
        writeDeclaredField(o, "timestamp1", new Timestamp(1618997829360L), true);//Timestamp
        writeDeclaredField(o, "javaSqlDate1", new java.sql.Date(1618997829360L), true);//java.sql.Date
        writeDeclaredField(o, "time1", new Time(1618997829360L), true);//Time
        writeDeclaredField(o, "currency1", Currency.getInstance(Locale.getDefault()), true);//Currency
        writeDeclaredField(o, "stringBuffer1", new StringBuffer("stringBuffer1"), true);//StringBuffer
        writeDeclaredField(o, "stringBuilder1", new StringBuilder("stringBuilder1"), true);//StringBuilder
        writeDeclaredField(o, "timezone1", TimeZone.getDefault(), true);//TimeZone
        writeDeclaredField(o, "calendar1", calendar, true);//Calendar
        writeDeclaredField(o, "locale1", Locale.getDefault(), true);//Locale
        writeDeclaredField(o, "charset1", Charset.forName("UTF-8"), true);//Charset
        writeDeclaredField(o, "url1", new URL("http://www.xyz.com"), true);//URL
        writeDeclaredField(o, "common1", (byte) 0x99, true);//byte
        writeDeclaredField(o, "common2", 1231381203812038L, true);//Long
        writeDeclaredField(o, "common3", "common3stringcontent", true);//String
    }


    private static final String WRITE_FINAL_COMPUTER_CLASS = "public final class Computer {"
            + "public String modelName;"
            + "public int memorySizeInMb;"
            + "public int[] fieldOnlyInReadSize = new int[1024];"
            + "public String displayModelName;"
            + "public int displaySize;"
            + "public String keyboardModel;"
            + "public byte[] fieldOnlyInReadSize2 = new byte[200];"
            + "public boolean multiCoreCpu;"
            + "public byte[] fieldOnlyInReadSize3 = new byte[252];"
            + "public char amdOrIntel;"
            + "public byte[] fieldOnlyInReadSize4 = new byte[253];"
            + "public byte[] fieldOnlyInReadSize5 = new byte[254];"
            + "public long diskSize;"
            + "public Display bDisplay = new Display();"
            + " private static class Display{ public byte[] buffer = new byte[1024];public int displaySize = 21;public boolean equals(Object o) { return ((Display)o).displaySize == this.displaySize;}}"
            + "public static class CommonObject { public Computer[] computers;}"
            + "}";
    private static final String READ_FINAL_COMPUTER_CLASS = "public final class Computer {"
            + "public int memorySizeInMb;"
            + "public String displayModelName;"
            + "public String keyboardModel;"
            + "public boolean multiCoreCpu;"
            + "public int[] fieldOnlyInWriteSize = new int[10];"
            + "public char amdOrIntel;"
            + "public long diskSize;"
            + "public boolean bluetooth;"
            + "public int ssdSize;"
            + "public static class CommonObject { public Computer[] computers;}"
            + "}";


    private static final String Version1_COMPUTER_CLASS = "import java.util.concurrent.TimeUnit;public class Computer {"
            + "public String modelName;"
            + "public int memorySizeInMb;"
            + "public int[] fieldOnlyInReadSize = new int[1024*1024];"
            + "public String displayModelName;"
            + " public TimeUnit timeUnitCommon;"
            + "public int displaySize;"
            + "public String keyboardModel;"
            + "public byte[] fieldOnlyInReadSize2 = new byte[200];"
            + "public boolean multiCoreCpu;"
            + "public TimeUnit timeUnitWriteSide;"
            + "public byte[] fieldOnlyInReadSize3 = new byte[252];"
            + "public char amdOrIntel;"
            + "public byte[] fieldOnlyInReadSize4 = new byte[253];"
            + "public byte[] fieldOnlyInReadSize5 = new byte[254];"
            + "public long diskSize;"
            + "public Display bDisplay = new Display();"
            + " private static class Display{ public byte[] buffer = new byte[1024];public int displaySize = 21;public boolean equals(Object o) { return ((Display)o).displaySize == this.displaySize;}}"
            + "}";
    private static final String Version2_COMPUTER_CLASS = "import java.util.concurrent.TimeUnit;public class Computer {"
            + "public int memorySizeInMb;"
            + "public String displayModelName;"
            + "public String keyboardModel;"
            + "public boolean multiCoreCpu;"
            + "public int[] fieldOnlyInWriteSize = new int[1024*1024];"
            + "public char amdOrIntel;"
            + "public long diskSize;"
            + "public TimeUnit timeUnitCommon;"
            + "public boolean bluetooth;"
            + "public int ssdSize;"
            + " public TimeUnit timeUnitReadSide;"
            + "}";

    private static final String WRITE_MYOBJECT_CLASS =
            "import java.util.*;"
                    + "import java.math.BigDecimal;"
                    + "import java.math.BigInteger;"
                    + "import java.net.URL;"
                    + "import java.nio.charset.Charset;"
                    + "import java.sql.Time;"
                    + "import java.sql.Timestamp;"
                    + "public class MyObject{"
                    + "boolean z1;"
                    + "Boolean z2;"
                    + "byte b1;"
                    + "Byte b2;"
                    + "char c1;"
                    + "Character c2;"
                    + "short s1;"
                    + "Short s2;"
                    + "int i1;"
                    + "Integer i2;"
                    + "long l1;"
                    + "Long l2;"
                    + "float f1;"
                    + "Float f2;"
                    + "double d1;"
                    + "Double d2;"
                    + "String string1;"
                    + "BigInteger bigInteger1;"
                    + "BigDecimal bigDecimal1;"
                    + "Date date1;"
                    + "Timestamp timestamp1;"
                    + "java.sql.Date javaSqlDate1;"
                    + "Time time1;"
                    + "Currency currency1;"
                    + "StringBuffer stringBuffer1;"
                    + "StringBuilder stringBuilder1;"
                    + "TimeZone timezone1;"
                    + "Calendar calendar1;"
                    + "Locale locale1;"
                    + "Charset charset1;"
                    + "URL url1;"
                    + "byte common1;"
                    + "Long common2;"
                    + "String common3;"
                    + "}";

    private static final String READ_MYOBJECT_CLASS = "import java.util.*;"
            + "import java.math.BigDecimal;"
            + "import java.math.BigInteger;"
            + "import java.net.URL;"
            + "import java.nio.charset.Charset;"
            + "import java.sql.Time;"
            + "import java.sql.Timestamp;"
            + "public class MyObject{"
            + "byte common1;"
            + "Long common2;"
            + "String common3;"
            + "}";


    private void testEqualIgnoreUnknownFields(Object base, Object curr) throws IllegalAccessException {
        Field[] baseFields = FieldUtils.getAllFields(base.getClass());
        Field[] currFields = FieldUtils.getAllFields(curr.getClass());
        for (Field currField : currFields) {
            currField.setAccessible(true);
            if (currField.isSynthetic() || Modifier.isStatic(currField.getModifiers())) {
                continue;
            }
            for (Field baseField : baseFields) {
                if (baseField.getName().equals(currField.getName())) {
                    baseField.setAccessible(true);
                    Object currValue = currField.get(curr);
                    Object baseValue = baseField.get(base);
                    if (currValue == null && baseValue == null) {
                        break;
                    } else {
                        //boolean result = currValue.getClass().equals(baseValue.getClass());
                        //assertTrue(result);
                        if (currValue != null) {
                            if (currValue.getClass() == int[].class) {
                                assertArrayEquals((int[]) baseValue, (int[]) currValue);
                            } else if (currValue.getClass() == byte[].class) {
                                assertArrayEquals((byte[]) baseValue, (byte[]) currValue);
                            } else if (Object[].class.isAssignableFrom(currValue.getClass())) {
                                Object[] a = (Object[]) currValue;
                                Object[] b = (Object[]) baseValue;
                                assertEquals(a.length, b.length);
                                for (int i = 0; i < a.length; i++) {
                                    testEqualIgnoreUnknownFields(a[i], b[i]);
                                }
                            } else {
                                boolean result = currValue.equals(baseValue);
                                assertTrue(result);
                            }
                        } else if (baseValue != null) {
                            assertTrue(baseValue.equals(currValue));
                        }
                        break;
                    }
                }
            }
        }
    }
}
