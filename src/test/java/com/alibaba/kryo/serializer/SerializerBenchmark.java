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
import com.alibaba.kryo.serializer.object.medium.MediumObject1;
import com.alibaba.kryo.serializer.object.small.class1.User;
import com.alibaba.kryo.serializer.object.small.class2.People;
import com.alibaba.kryo.serializer.object.small.class3.Phone;
import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.SerializerFactory;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class SerializerBenchmark {

    public static class Kryo_Upstream_Compatible {
        public Kryo init(Object dataDO) {
            Kryo writeKryo = new Kryo();
            writeKryo.setDefaultSerializer(new SerializerFactory.CompatibleFieldSerializerFactory());
            writeKryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            writeKryo.setRegistrationRequired(false);
            writeKryo.setReferences(true);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Output output = new Output(os);
            writeKryo.writeClassAndObject(output, dataDO);
            output.close();

            Input input = new Input(os.toByteArray());
            writeKryo.readClassAndObject(input);

            return writeKryo;
        }

        public void run(Kryo kryo, Object dataDO) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Output output = new Output(os);
            kryo.writeClassAndObject(output, dataDO);
            output.close();
            Input input = new Input(os.toByteArray());
            kryo.readClassAndObject(input);
            input.close();
        }
    }

    public static class Kryo_Upstream_Compatible_Diff_Layout {
        public Kryo[] init(Object dataDO, MemoryClassLoader writeClassLoader, MemoryClassLoader readClassLoader) {
            Kryo writeKryo = createKryo();
            writeKryo.setClassLoader(writeClassLoader);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Output output = new Output(os);
            writeKryo.writeClassAndObject(output, dataDO);
            output.close();

            Kryo readKryo = createKryo();
            readKryo.setClassLoader(readClassLoader);
            Input input = new Input(os.toByteArray());
            readKryo.readClassAndObject(input);

            return new Kryo[]{writeKryo, readKryo};
        }

        Kryo createKryo() {
            Kryo writeKryo = new Kryo();
            writeKryo.setDefaultSerializer(new SerializerFactory.CompatibleFieldSerializerFactory());
            writeKryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            writeKryo.setRegistrationRequired(false);
            writeKryo.setReferences(true);
            return writeKryo;
        }

        public void run(Kryo[] writeReadKryo, Object dataDO) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Output output = new Output(os);
            writeReadKryo[0].writeClassAndObject(output, dataDO);
            output.close();
            Input input = new Input(os.toByteArray());
            writeReadKryo[1].readClassAndObject(input);
            input.close();
        }
    }

    public static class SCFS_Diff_Layout {
        private static final String DUMMY_SESSION_KEY = "dummySessionKey";
        private SessionKryo writeKryo;
        private SessionKryo readKryo;

        public SessionKryo[] init(Object dataDO, MemoryClassLoader writeClassLoader, MemoryClassLoader readClassLoader) {
            ReadWriteSessionDataLocator readWriteSessionDataLocator = new ReadWriteSessionDataLocator() {
                private ReadWriteSessionData sessionData;

                @Override
                public ReadWriteSessionData find(Object locateKey) {
                    return sessionData;
                }

                @Override
                public void newCreated(Object locateKey, ReadWriteSessionData newReadWriteSessionData) {
                    this.sessionData = newReadWriteSessionData;
                }
            };
            SessionKryoFactory sessionKryoFactory = SessionKryoService.newCachedSessionKryoFactory(new CacheSessionKryoPolicy<String>() {
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
            }, readWriteSessionDataLocator, CreateOption.of());
            writeKryo = sessionKryoFactory.get("writeKryo");
            writeKryo.setClassLoader(writeClassLoader);
            readKryo = sessionKryoFactory.get("readKryo");
            readKryo.setClassLoader(readClassLoader);
            return new SessionKryo[]{writeKryo, readKryo};
        }

        public void run(SessionKryo[] writeReadKryo, Object dataDO) {
            byte[] b = writeReadKryo[0].writeClassAndObject(DUMMY_SESSION_KEY, dataDO);
            writeReadKryo[1].readClassAndObject(DUMMY_SESSION_KEY, b);
        }
    }

    public static class SCFS_Same_Layout {
        private static final String DUMMY_SESSION_KEY = "dummySessionKey";

        public SessionKryo init(Object dataDO) {
            ReadWriteSessionDataLocator readWriteSessionDataLocator = new ReadWriteSessionDataLocator() {
                private ReadWriteSessionData sessionData;

                @Override
                public ReadWriteSessionData find(Object locateKey) {
                    return sessionData;
                }

                @Override
                public void newCreated(Object locateKey, ReadWriteSessionData newReadWriteSessionData) {
                    this.sessionData = newReadWriteSessionData;
                }
            };
            SessionKryoFactory sessionKryoFactory = SessionKryoService.newThreadLocalSessionKryoFactory(readWriteSessionDataLocator, CreateOption.of());
            byte[] b = sessionKryoFactory.get().writeClassAndObject(DUMMY_SESSION_KEY, dataDO);
            sessionKryoFactory.get().readClassAndObject(DUMMY_SESSION_KEY, b);
            return sessionKryoFactory.get();
        }

        public void run(SessionKryo kryo, Object dataDO) {
            byte[] b = kryo.writeClassAndObject(DUMMY_SESSION_KEY, dataDO);
            kryo.readClassAndObject(DUMMY_SESSION_KEY, b);
        }
    }

    public static class Kryo_Upstream_NotCompatible {
        public Kryo init(Object dataDO) {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setReferences(true);
            kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Output output = new Output(os);
            kryo.writeClassAndObject(output, dataDO);
            output.close();

            Input input = new Input(os.toByteArray());
            kryo.readClassAndObject(input);
            return kryo;
        }

        public void run(Kryo kryo, Object dataDO) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Output output = new Output(os);
            kryo.writeClassAndObject(output, dataDO);
            output.close();

            Input input = new Input(os.toByteArray());
            kryo.readClassAndObject(input);
            input.close();
        }
    }


    @State(Scope.Thread)
    public static class Kryo_Upstream_Compatible_Large1 extends Kryo_Upstream_Compatible {

        private Kryo writeKryo;
        private Conq4 dataDO;

        @Setup(Level.Trial)
        public void init() {
            dataDO = LargeDO.create();
            writeKryo = super.init(dataDO);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, dataDO);
        }
    }

    @State(Scope.Thread)
    public static class SCFS_Large1 extends SCFS_Same_Layout {

        private SessionKryo writeKryo;
        private Conq4 dataDO;

        @Setup(Level.Trial)
        public void init() {
            dataDO = LargeDO.create();
            writeKryo = super.init(dataDO);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, dataDO);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_NotCompatible_Large1 extends Kryo_Upstream_NotCompatible {

        private Kryo writeKryo;
        private Conq4 dataDO;

        @Setup(Level.Trial)
        public void init() {
            dataDO = LargeDO.create();
            writeKryo = super.init(dataDO);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, dataDO);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_Compatible_Small1 extends Kryo_Upstream_Compatible {

        private Kryo writeKryo;
        private User user;

        @Setup(Level.Trial)
        public void init() {
            user = new User();
            writeKryo = super.init(user);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, user);
        }
    }

    @State(Scope.Thread)
    public static class SCFS_Small1 extends SCFS_Same_Layout {

        private SessionKryo writeKryo;
        private User user;

        @Setup(Level.Trial)
        public void init() {
            user = new User();
            writeKryo = super.init(user);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, user);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_NotCompatible_Small1 extends Kryo_Upstream_NotCompatible {

        private Kryo writeKryo;
        private User user;

        @Setup(Level.Trial)
        public void init() {
            user = new User();
            writeKryo = super.init(user);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, user);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_Compatible_Small2 extends Kryo_Upstream_Compatible {

        private Kryo writeKryo;
        private People dataDO;

        @Setup(Level.Trial)
        public void init() {
            dataDO = People.create();
            writeKryo = super.init(dataDO);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, dataDO);
        }
    }

    @State(Scope.Thread)
    public static class SCFS_Small2 extends SCFS_Same_Layout {

        private SessionKryo writeKryo;
        private People dataDO;

        @Setup(Level.Trial)
        public void init() {
            dataDO = People.create();
            writeKryo = super.init(dataDO);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, dataDO);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_NotCompatible_Small2 extends Kryo_Upstream_NotCompatible {

        private Kryo writeKryo;
        private People dataDO;

        @Setup(Level.Trial)
        public void init() {
            dataDO = People.create();
            writeKryo = super.init(dataDO);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, dataDO);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_Compatible_Small3 extends Kryo_Upstream_Compatible {

        private Kryo writeKryo;
        private Phone phone;

        @Setup(Level.Trial)
        public void init() {
            phone = Phone.create();
            writeKryo = super.init(phone);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, phone);
        }
    }

    @State(Scope.Thread)
    public static class SCFS_Small3 extends SCFS_Same_Layout {

        private SessionKryo writeKryo;
        private Phone phone;

        @Setup(Level.Trial)
        public void init() {
            phone = Phone.create();
            writeKryo = super.init(phone);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, phone);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_NotCompatible_Small3 extends Kryo_Upstream_NotCompatible {

        private Kryo writeKryo;
        private Phone phone;

        @Setup(Level.Trial)
        public void init() {
            phone = Phone.create();
            writeKryo = super.init(phone);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, phone);
        }
    }


    @State(Scope.Thread)
    public static class Kryo_Upstream_Compatible_Medium1 extends Kryo_Upstream_Compatible {

        private Kryo writeKryo;
        private MediumObject1 mediumObject1;

        @Setup(Level.Trial)
        public void init() {
            mediumObject1 = MediumObject1.create();
            writeKryo = super.init(mediumObject1);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, mediumObject1);
        }
    }

    @State(Scope.Thread)
    public static class SCFS_Medium1 extends SCFS_Same_Layout {

        private SessionKryo writeKryo;
        private MediumObject1 mediumObject1;

        @Setup(Level.Trial)
        public void init() {
            mediumObject1 = MediumObject1.create();
            writeKryo = super.init(mediumObject1);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, mediumObject1);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_NotCompatible_Medium1 extends Kryo_Upstream_NotCompatible {

        private Kryo writeKryo;
        private MediumObject1 mediumObject1;

        @Setup(Level.Trial)
        public void init() {
            mediumObject1 = MediumObject1.create();
            writeKryo = super.init(mediumObject1);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            super.run(writeKryo, mediumObject1);
        }
    }

    @State(Scope.Thread)
    public static class Kryo_Upstream_Compatible_DiffLayout1 extends Kryo_Upstream_Compatible_Diff_Layout {

        private Kryo[] writeReadKryo;
        private Object toWriteObject;
        private MemoryClassLoader writeClassLoader;
        private MemoryClassLoader readClassLoader;

        @Setup(Level.Trial)
        public void init() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
            writeClassLoader = new MemoryClassLoader(getVersion1Class(), MemoryClassLoader.class.getClassLoader());
            toWriteObject = writeClassLoader.findClass("Computer").newInstance();
            readClassLoader = new MemoryClassLoader(getVersion2Class(), MemoryClassLoader.class.getClassLoader());
            writeReadKryo = super.init(toWriteObject, writeClassLoader, readClassLoader);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            writeReadKryo[0].setClassLoader(writeClassLoader);
            writeReadKryo[1].setClassLoader(readClassLoader);
            super.run(writeReadKryo, toWriteObject);
        }
    }

    @State(Scope.Thread)
    public static class SCFS_DiffLayout1 extends SCFS_Diff_Layout {

        private SessionKryo[] writeReadKryo;
        private Object toWriteObject;
        private MemoryClassLoader writeClassLoader;
        private MemoryClassLoader readClassLoader;

        @Setup(Level.Trial)
        public void init() throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
            writeClassLoader = new MemoryClassLoader(getVersion1Class(), MemoryClassLoader.class.getClassLoader());
            toWriteObject = writeClassLoader.findClass("Computer").newInstance();
            readClassLoader = new MemoryClassLoader(getVersion2Class(), MemoryClassLoader.class.getClassLoader());
            writeReadKryo = super.init(toWriteObject, writeClassLoader, readClassLoader);
        }

        @Benchmark
        @BenchmarkMode({Mode.Throughput})
        @OutputTimeUnit(TimeUnit.SECONDS)
        public void run() throws Exception {
            writeReadKryo[0].setClassLoader(writeClassLoader);
            writeReadKryo[1].setClassLoader(readClassLoader);
            super.run(writeReadKryo, toWriteObject);
        }
    }

    private static Map<String, byte[]> getVersion1Class() throws IOException {
        return compile("Computer.java", Version1_COMPUTER_CLASS);
    }

    private static Map<String, byte[]> getVersion2Class() throws IOException {
        return compile("Computer.java", Version2_COMPUTER_CLASS);
    }

    private static Map<String, byte[]> compile(String fileName, String source) throws IOException {
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

    private static final String Version1_COMPUTER_CLASS = "import java.util.concurrent.TimeUnit;public class Computer {"
            + "public String modelName;"
            + "public int memorySizeInMb;"
            + "public int[] fieldOnlyInReadSize = new int[10];"
            + "public String displayModelName;"
            + " public TimeUnit timeUnitCommon;"
            + "public int displaySize;"
            + "public String keyboardModel;"
            + "public byte[] fieldOnlyInReadSize2 = new byte[20];"
            + "public boolean multiCoreCpu;"
            + "public TimeUnit timeUnitWriteSide;"
            + "public byte[] fieldOnlyInReadSize3 = new byte[22];"
            + "public char amdOrIntel;"
            + "public byte[] fieldOnlyInReadSize4 = new byte[23];"
            + "public byte[] fieldOnlyInReadSize5 = new byte[4];"
            + "public long diskSize;"
            + "}";
    private static final String Version2_COMPUTER_CLASS = "import java.util.concurrent.TimeUnit;public class Computer {"
            + "public int memorySizeInMb;"
            + "public String displayModelName;"
            + "public String keyboardModel;"
            + "public boolean multiCoreCpu;"
            + "public int[] fieldOnlyInWriteSize = new int[24];"
            + "public char amdOrIntel;"
            + "public long diskSize;"
            + "public TimeUnit timeUnitCommon;"
            + "public boolean bluetooth;"
            + "public int ssdSize;"
            + " public TimeUnit timeUnitReadSide;"
            + "}";


    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(Kryo_Upstream_Compatible_Large1.class.getSimpleName())
                .include(SCFS_Large1.class.getSimpleName())
                .include(Kryo_Upstream_NotCompatible_Large1.class.getSimpleName())
                .include(Kryo_Upstream_Compatible_Small1.class.getSimpleName())
                .include(SCFS_Small1.class.getSimpleName())
                .include(Kryo_Upstream_NotCompatible_Small1.class.getSimpleName())
                .include(Kryo_Upstream_Compatible_Small2.class.getSimpleName())
                .include(SCFS_Small2.class.getSimpleName())
                .include(Kryo_Upstream_NotCompatible_Small2.class.getSimpleName())
                .include(Kryo_Upstream_Compatible_Small3.class.getSimpleName())
                .include(SCFS_Small3.class.getSimpleName())
                .include(Kryo_Upstream_NotCompatible_Small3.class.getSimpleName())
                .include(Kryo_Upstream_Compatible_Medium1.class.getSimpleName())
                .include(SCFS_Medium1.class.getSimpleName())
                .include(Kryo_Upstream_NotCompatible_Medium1.class.getSimpleName())
                .include(Kryo_Upstream_Compatible_DiffLayout1.class.getSimpleName())
                .include(SCFS_DiffLayout1.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .forks(3)
                .build();

        new Runner(opt).run();
    }
}
