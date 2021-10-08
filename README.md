# 1. Introduction
[kryo](https://github.com/EsotericSoftware/kryo) is a fast and efficient object graph serialization framework for Java. 
You can use the Kryo library to serialize objects quickly, but it does require the object layout to be the same 
on both serialization and deserialization sides. Generally, this prerequisite is unrealistic in real application scenarios. 
For example, the applications in the distribution system rely on an object model for communication across the nodes. 
Along with the system's evolution, the developers have to add new fields to the object model to meet new business requirements.
In this typical case, it is challenging for the developers to simultaneously upgrade all the applications to use the same object 
layout for fast performance, which usually requires tremendous work.   

If using kryo's CompatibleFieldSerializer will incur performance degradation.This project, session-compatible-fast-serializer(SCFS), 
try to keep a balance between compatibility and performance.

We perform JMH test with the following conditions:
- OS: Linux 64 bit
- JDK: Dragonwell 8 - 64bits
- Hardware: Aliyun ECS with 4vCPU+8GB

The following test results are divide into groups according to the complexity of the test object. 
```
Benchmark                                                      Mode  Cnt        Score                Error  Units
SerializerBenchmark.Kryo_Upstream_Compatible_DiffLayout1.run  thrpt   15   370660.262 ±  2967.346           ops/s
SerializerBenchmark.SCFS_DiffLayout1.run                      thrpt   15  1237834.811 ± 14960.798           ops/s

SerializerBenchmark.Kryo_Upstream_Compatible_Small1.run       thrpt   15   371108.770 ±  1509.108           ops/s
SerializerBenchmark.Kryo_Upstream_NotCompatible_Small1.run    thrpt   15   556920.706 ±  7405.588           ops/s
SerializerBenchmark.SCFS_Small1.run                           thrpt   15   904877.294 ±  8731.215           ops/s

SerializerBenchmark.Kryo_Upstream_Compatible_Small2.run       thrpt   15   284163.019 ±  2834.764           ops/s
SerializerBenchmark.Kryo_Upstream_NotCompatible_Small2.run    thrpt   15   392914.210 ±  3365.496           ops/s
SerializerBenchmark.SCFS_Small2.run                           thrpt   15   571458.927 ±  7719.183           ops/s

SerializerBenchmark.Kryo_Upstream_Compatible_Small3.run       thrpt   15   568131.324 ±  3588.822           ops/s
SerializerBenchmark.Kryo_Upstream_NotCompatible_Small3.run    thrpt   15   817075.307 ±  5256.913           ops/s
SerializerBenchmark.SCFS_Small3.run                           thrpt   15  1623851.291 ± 14505.581           ops/s

SerializerBenchmark.Kryo_Upstream_Compatible_Medium1.run      thrpt   15    67248.623 ±  1557.259           ops/s
SerializerBenchmark.Kryo_Upstream_NotCompatible_Medium1.run   thrpt   15   141754.221 ±   765.331           ops/s
SerializerBenchmark.SCFS_Medium1.run                          thrpt   15   176203.113 ±  1621.590           ops/s

SerializerBenchmark.Kryo_Upstream_Compatible_Large1.run       thrpt   15    18341.364 ±    97.690           ops/s
SerializerBenchmark.Kryo_Upstream_NotCompatible_Large1.run    thrpt   15    22221.829 ±   545.190           ops/s
SerializerBenchmark.SCFS_Large1.run                           thrpt   15    24012.446 ±   280.637           ops/s
```
We also test SCFS with the following JDKs:
- Dragonwell 8 - 64bits
- Dragonwell 11 - 64bits
- AdoptOpenJDK 8 - 64bits
- AdoptOpenJDK 11 - 64bits
- OpenJ9 JDK8 - 64bits
- OpenJ9 JDK11 - 64bits 

# 2. Quickstart
## 2.1 Add Dependency
**Note:** SCFS requires JDK 1.8 and kryo 5 or later.
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>session-compatible-fast-serializer</artifactId>
    <version>1.0.0</version>
</dependency>
```
If not, you can download JAR in [Maven Center Repository](https://mvnrepository.com/artifact/com.alibaba/session-compatible-fast-serializer).
## 2.2 Hello SessionKryo
```java
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
```

## 2.3 Integration Guide
What's the difference between SCFS and kryo? SCFS is a stateful serializer, it defines a session between writer and reader,
and the session must be unique between writer and reader. For example, a socket connection can regard as a session identifier.
So using SCFS requires doing some mapping between some unique identifiers in your application to SCFS's session.
### 2.3.1 Implement ReadWriteSessionDataLocator 
The performance improvement comes from the abstraction of the Session. The metadata of the object can transmit only once in the compatible mode in one session.
During each serialization, SCFS need to get the current session by a unique identifier. In a distributed system, a session can bind to a socket connection. 

Pay attention to the following points when implementing ReadWriteSessionDataLocator:
1. Session is not thread-safe, make sure ReadWriteSessionData cannot be used by multiple threads simultaneously.
2. Cached ReadWriteSessionData should consider when to destroy, such as ReadWriteSessionData associated with the socket, it should be cleared when the socket closed.
An example of implementation is as follows(SocketLifecycle is a sample,not a real interface):
```java
public class SocketReadWriteSessionDataLocator implements ReadWriteSessionDataLocator,SocketLifecycle {
    private Map<String,ReadWriteSessionData> sessionDataMap = new ConcurrentHashMap<>();

    @Override
    public ReadWriteSessionData find(String locateKey) {
        return sessionDataMap.get(locateKey);
    }

    @Override
    public void newCreated(String locateKey, ReadWriteSessionData newReadWriteSessionData) {
        sessionDataMap.put(locateKey,newReadWriteSessionData);
    }

    @Override
    public void socketClose(String channelId){
        sessionDataMap.remove(channelId);
    }
}
```

### 2.3.2 Instance SessionKryoFactory
SessionKryo is a memory consumed object in runtime, and it's not thread-safe. So SessionKryo should be cached.
SessionKryoFactory is that factory for reusing SessionKryo instances. SessionKryoFactory should be initialized only once.
#### 2.3.2.1 Singleton SessionKryo
Applicable to only one thread uses the Kryo.
```java
SessionKryoFactory sessionKryoFactory = SessionKryoService.newSingletonSessionKryoFactory(sessionDataLocator, CreateOption.of());
```
#### 2.3.2.2 ThreadLocal SessionKryo
In a distributed framework, there is usually a special IO thread pool to process serialization, so in this scenario, 
binding a SessionKryo to each IO thread can reduce the number of SessionKryo instances.
```java
 SessionKryoFactory sessionKryoFactory = SessionKryoService.newThreadLocalSessionKryoFactory(readWriteSessionDataLocator, CreateOption.of());
```
#### 2.3.2.3 Custom SessionKryoFactory
If the above two situations do not meet requirements, you can customize them. For example, 
```java
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
}, sessionDataLocator, CreateOption.of());
```
## 2.3.4 Use SessionKryo's API to serialize and deserialize 
```
SessionKryo sessionKryo = sessionKryoFactory.get(); 
byte[] b = kryo.writeClassAndObject(sessionKey, objToSerialize);
Object object = kryo.readClassAndObject(sessionKey, b);
```
# 3. How it works
## 3.1 Compatible
Kryo uses chunked encoding to support compatibility. Chucked encoding incurs some extra memory copy which leads to performance degradation.
However, in some cases, the serialization process writes the result to a byte array instead of java.io.OutputStream. In the same way, 
deserialization reads from the byte array instead of java.io.InputStream.
Because byte array can be randomly read and write, we don't using chunked encoding but also support serialization compatibility.
### 3.1.1  Type encoding for known types
The key to compatibility is how to skip an unrecognized field. If each field encode with a length, the overhead will be large, but  
some known types, such as int, long, String, etc., even if it didn't know the length, it also knows how to decode.
Therefore, SCFS encode type information into the data for the first time. The deserializer caches the type information of the object for later use.
### 3.1.2 Encode variable-length fields
```
|length block|larger field pointer|field 1| field 2| ... |larger field num|larger field 1 size|larger field 2 size| ...
```
* length block: fixed-length byte array. If the length of a field is less than or equal to 255, marked the length here directly. Otherwise, the length will be written in the larger field.
* larger field pointer: if the length of a field exceeds 255 characters, point to the beginning of larger field num.
* field 1: the data area of a specific field
* larger field number: the number of larger fields.
* larger field 1 size: the size of a larger field.
The main purpose is to reduce the number of additional bytes :
1. For known types, no need to encode field length. 
2. Otherwise, If the field length ≤ 255, need one byte.
3. For length> 255 need a variable-length block. Considering such fields is relatively small, so the overhead is relatively small.

### 3.1.3 Quickly skip unrecognized fields based on encoding information.
By the type and field length information in the byte stream, the deserialization side knows how to skip an unrecognized field.
## 3.2 Performance
### 3.2.1 Object metadata sent only once
The metadata of an object is sent only once in a session. The metadata of an object includes the name and type information of the fields.
### 3.2.2 Traverse object by memory offsets in the JVM to make the Cache more friendly
Kryo traverses fields by field name, but the JVM internal does not layout the fields by the name.
If an object traverse according to the offset of the field in JVM internal, this will give the chance to hit L1 cache.
### 3.2.3 Remember object output size after serialization
During the test, we found there is a call hotspot on System.arrayCopy function.
For the large object, the output buffer needs to expand many times to fulfil the final serialize requirements.
If the output buffer initial size same as the previous serialization, it can reduce some unnecessary calls to System.arrayCopy.
### 3.2.4 Cache layout compare the result for deserialization 
If the object layout is the same, cache the result avoid comparing each time.
If the object layout is not the same, cache the mapping between the fields avoid comparing each time.
### 3.2.5 Divide cached data into the global area, thread area, and session area
Put the major memory consumed part into the global area, the other area reference to the global area. In this way, when the number of sessions reaches tens of thousands, 
the memory overhead is relatively small. 
