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
import com.esotericsoftware.kryo.kryo5.KryoException;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer;
import com.esotericsoftware.kryo.kryo5.unsafe.UnsafeUtil;
import com.esotericsoftware.kryo.kryo5.util.ObjectMap;

import java.util.Arrays;
import java.util.Comparator;

import static com.esotericsoftware.kryo.kryo5.minlog.Log.TRACE;
import static com.esotericsoftware.kryo.kryo5.minlog.Log.trace;
import static com.esotericsoftware.kryo.kryo5.util.Util.pos;
import static com.esotericsoftware.kryo.kryo5.util.Util.unsafe;

/**
 * Implement a quick and compatible field serializer.
 * Each chunk must be process in a temporarily buffer,then copy the chunk into output stream.The extra memory copy impact the performance.
 * However, in actual use, we find that serialization writes the result to a byte array instead of directly writing it to java.io.OutputStream. In the same way, deserialization reads from the byte data.
 * Instead of reading directly from java.io.InputStream.
 * Because byte arrays can be randomly read and written, we can avoid using Chunk-based temporary buffers to achieve compatibility.
 *
 * @param <T>
 */
public class FastCachedCompatibleFieldSerializer<T> extends FieldSerializer<T> {
    public static final int SHORTER_FIELD_LEN = 255;
    public static final int LONGER_FIELD = 0x0;

    private static final byte fields_flag = (byte) 0xff;
    private static final byte no_fields_flag = (byte) 0xfe;

    private final FastCachedCompatibleFieldSerializer.CachedCompatibleFieldSerializerConfig config;
    private static final QuickSkipSerializer quickSkipSerializer = new QuickSkipSerializer();
    private SessionKryo sessionKryo;

    public FastCachedCompatibleFieldSerializer(Kryo kryo, Class type) {
        this(kryo, type, new FastCachedCompatibleFieldSerializer.CachedCompatibleFieldSerializerConfig());
        this.sessionKryo = ((CachedClassResolver) kryo.getClassResolver()).sessionKryo;
    }

    /**
     * Kryo traverses fields by field name, but the JVM does not layout the fields by the name.
     * If object traverse according the offset of the field in memory, this will give the more chances to hit L1 cache.
     */
    @Override
    protected void initializeCachedFields() {
        super.initializeCachedFields();
        if (unsafe) {
            try {
                long offset = UnsafeUtil.unsafe.objectFieldOffset(CachedField.class.getDeclaredField("offset"));
                CachedField[] fields = getFields();
                Arrays.sort(fields, new Comparator<CachedField>() {
                    @Override
                    public int compare(CachedField o1, CachedField o2) {
                        long a = UnsafeUtil.unsafe.getLong(o1, offset);
                        long b = UnsafeUtil.unsafe.getLong(o2, offset);
                        return a < b ? -1 : (a > b ? 1 : 0);
                    }
                });
            } catch (NoSuchFieldException e) {
                //silent ignore.
            }
        }
    }

    public FastCachedCompatibleFieldSerializer(Kryo kryo, Class type, FastCachedCompatibleFieldSerializer.CachedCompatibleFieldSerializerConfig config) {
        super(kryo, type, config);
        this.config = config;
        this.sessionKryo = ((CachedClassResolver) kryo.getClassResolver()).sessionKryo;
    }

    /**
     * <ul>
     *     <li>Object metadata sent only once</li>
     *     <li>Type encoding for known types and Encode variable-length fields</li>
     * </ul>
     * @param kryo
     * @param output
     * @param object
     */
    public void write(Kryo kryo, Output output, T object) {
        int pop = pushTypeVariables();
        ReadWriteSessionData sessionData = sessionKryo.sessionData;

        CachedField[] fields = getFields();
        ObjectMap context = kryo.getGraphContext();
        FieldsDefaultType fieldsDefaultType = null;
        if (!context.containsKey(this)) {
            if (TRACE) trace("kryo", "Write fields for class: " + getType().getName());
            context.put(this, null);

            if (!sessionData.writeClassToNameId.containsKey(getType())) {
                // 2 cases will run to here:
                // 1)writeObject which no write classes.
                // 2)ObjectArraySerializer when element is final,no need write class
                int nameId = sessionData.writeNextNameId++;
                sessionData.writeClassToNameId.put(getType(), nameId);
                sessionData.toWrittenFieldClassSet.add(getType());
            }

            if (!sessionData.toWrittenFieldClassSet.contains(getType())) {
                output.writeByte(no_fields_flag);
            } else {
                output.writeByte(fields_flag);
                output.writeVarInt(fields.length, true);
                fieldsDefaultType = ApplicationShareData.writtenField.get(getType());
                if (fieldsDefaultType != null) {
                    for (int i = 0, n = fields.length; i < n; i++) {
                        if (TRACE) trace("kryo", "Write field name: " + fields[i].getName() + pos(output.position()));
                        output.writeString(fields[i].getName());
                    }
                    output.writeBytes(fieldsDefaultType.fieldsCode);
                } else {
                    fieldsDefaultType = new FieldsDefaultType(fields.length);
                    int notDefaultTypeFieldNum = 0;
                    for (int i = 0, n = fields.length; i < n; i++) {
                        if (TRACE) trace("kryo", "Write field name: " + fields[i].getName() + pos(output.position()));
                        output.writeString(fields[i].getName());
                        byte typeCode = (byte) KnownTypes.class2Code.get(fields[i].getField().getType(), KnownTypes.NOT_KNOWN);
                        fieldsDefaultType.fieldsCode[i] = typeCode;
                        if (typeCode == KnownTypes.NOT_KNOWN) {
                            notDefaultTypeFieldNum++;
                        }
                    }

                    if (notDefaultTypeFieldNum > 0) {
                        fieldsDefaultType.containVarLenField = true;
                        //the 2 following field need write to final byte stream
                        //allocate in cache so that avoid allocate array evey time.
                        fieldsDefaultType.varLenField = new byte[notDefaultTypeFieldNum];
                    } else {
                        fieldsDefaultType.containVarLenField = false;
                    }

                    output.writeBytes(fieldsDefaultType.fieldsCode);
                    ApplicationShareData.writtenField.put(getType(), fieldsDefaultType);
                }
            }
        }

        Output fieldOutput = output;
        if (fieldsDefaultType == null) {
            fieldsDefaultType = ApplicationShareData.writtenField.get(getType());
        }

        if (fieldsDefaultType.containVarLenField) {
            int varLenSmallerPos = output.position();
            output.writeBytes(fieldsDefaultType.varLenField);

            //pointer to field length > SMALLER_FIELD_LEN
            int varLenLargerPos = output.position();
            output.writeInt(0);

            int varLenFieldNum = 0;
            int largerFieldNum = 0;
            int[] longerFieldLength = new int[fieldsDefaultType.varLenField.length];
            for (int i = 0, n = fields.length; i < n; i++) {
                int startPos = output.position();
                CachedField cachedField = fields[i];
                if (TRACE) log("Write", cachedField, output.position());
                cachedField.write(fieldOutput, object);
                if (fieldsDefaultType.fieldsCode[i] == KnownTypes.NOT_KNOWN) {
                    int size = output.position() - startPos;
                    if (size <= SHORTER_FIELD_LEN) {
                        output.getBuffer()[varLenSmallerPos + varLenFieldNum] = (byte) size;
                    } else {
                        output.getBuffer()[varLenSmallerPos + varLenFieldNum] = (byte) LONGER_FIELD;
                        longerFieldLength[largerFieldNum++] = size;
                    }
                    varLenFieldNum++;
                }
            }

            if (largerFieldNum > 0) {
                writeInAt(output.getBuffer(), varLenLargerPos, output.position() - varLenLargerPos);
                output.writeInt(largerFieldNum);
                for (int i = 0; i < largerFieldNum; i++) {
                    output.writeInt(longerFieldLength[i]);
                }
            }
        } else {
            for (int i = 0, n = fields.length; i < n; i++) {
                CachedField cachedField = fields[i];
                if (TRACE) log("Write", cachedField, output.position());
                cachedField.write(fieldOutput, object);
            }
        }

        popTypeVariables(pop);
    }

    private void writeInAt(byte[] buffer, int p, int value) {
        buffer[p] = (byte) value;
        buffer[p + 1] = (byte) (value >> 8);
        buffer[p + 2] = (byte) (value >> 16);
        buffer[p + 3] = (byte) (value >> 24);
    }

    private int readIntAt(byte[] buffer, int p) {
        return buffer[p] & 0xFF //
                | (buffer[p + 1] & 0xFF) << 8 //
                | (buffer[p + 2] & 0xFF) << 16 //
                | (buffer[p + 3] & 0xFF) << 24;
    }

    /**
     * Cache layout compare result for deserialization
     * @param kryo
     * @param input
     * @param type
     * @return
     */
    public T read(Kryo kryo, Input input, Class<? extends T> type) {
        int pop = pushTypeVariables();
        ReadWriteSessionData sessionData = sessionKryo.sessionData;

        T object = create(kryo, input, type);
        kryo.reference(object);

        CachedField[] fields = (CachedField[]) kryo.getGraphContext().get(this);
        ReadFieldContext readFieldContext = new ReadFieldContext();
        if (fields == null) {
            fields = readFields(kryo, input, readFieldContext, sessionData);
        } else {
            readFieldContext.readMetaInfo = sessionData.readNameToMeta.get(getType());
        }

        if (readFieldContext.readMetaInfo != null) {
            if (readFieldContext.readMetaInfo.varLenFieldNum > 0) {
                //skip length field.
                byte[] sizeArray = input.readBytes(readFieldContext.readMetaInfo.varLenFieldNum);
                int pointerBasePos = input.position();
                int offsetToVarLen = input.readInt();
                int largerFieldNum = offsetToVarLen == 0 ? 0 : readIntAt(input.getBuffer(), pointerBasePos + offsetToVarLen);

                if (readFieldContext.readMetaInfo.sameLayout) {
                    for (int i = 0, n = fields.length; i < n; i++) {
                        fields[i].read(input, object);
                    }
                } else {
                    int varLenIndex = 0;
                    int largeFieldIndex = 0;
                    byte[] fieldTypeCode = readFieldContext.readMetaInfo.fieldsCode;
                    for (int i = 0, n = fields.length; i < n; i++) {
                        CachedField cachedField = fields[i];
                        if (cachedField != null) {
                            fields[i].read(input, object);
                        } else {
                            if (fieldTypeCode[i] == KnownTypes.NOT_KNOWN) {
                                if (sizeArray[varLenIndex] == LONGER_FIELD) {
                                    quickSkipSerializer.skip(kryo, input, readIntAt(input.getBuffer(), pointerBasePos + offsetToVarLen + ((largeFieldIndex + 1) << 2)));
                                } else {
                                    quickSkipSerializer.skip(kryo, input, sizeArray[varLenIndex] & 0xFF);
                                }
                            } else {
                                //if is in the default type serializer,we known how to skip this field.
                                sessionKryo.knownTypes.code2Serializer[fieldTypeCode[i]].read(input, sessionKryo.yummyObject);
                            }
                        }
                        if (fieldTypeCode[i] == KnownTypes.NOT_KNOWN) {
                            if (sizeArray[varLenIndex] == LONGER_FIELD) {
                                largeFieldIndex++;
                            }
                            varLenIndex++;
                        }
                    }
                }

                //skip longer field
                if (largerFieldNum > 0) {
                    //[field number][field1][field2]...
                    //each field is 4 bytes.
                    input.skip((largerFieldNum + 1) << 2);
                }
            } else {
                if (readFieldContext.readMetaInfo.sameLayout) {
                    for (int i = 0, n = fields.length; i < n; i++) {
                        fields[i].read(input, object);
                    }
                } else {
                    byte[] fieldTypeCode = readFieldContext.readMetaInfo.fieldsCode;
                    for (int i = 0, n = fields.length; i < n; i++) {
                        CachedField cachedField = fields[i];
                        if (cachedField != null) {
                            fields[i].read(input, object);
                        } else {
                            //if is in the default type serializer,we known how to skip this field.
                            sessionKryo.knownTypes.code2Serializer[fieldTypeCode[i]].read(input, sessionKryo.yummyObject);
                        }
                    }
                }
            }
        } else {
            for (int i = 0, n = fields.length; i < n; i++) {
                fields[i].read(input, object);
            }
        }
        popTypeVariables(pop);
        return object;
    }

    private CachedField[] readFields(Kryo kryo, Input input, ReadFieldContext fieldContext, ReadWriteSessionData sessionData) {
        if (TRACE) trace("kryo", "Read fields for class: " + getType().getName());
        byte flag = input.readByte();
        String[] names = null;
        int length = 0;

        ReadMetaInfo readMetaInfo = null;
        if (flag == fields_flag) {
            length = input.readVarInt(true);
            names = new String[length];
            for (int i = 0; i < length; i++) {
                names[i] = input.readString();
                if (TRACE) trace("kryo", "Read field name: " + names[i]);
            }
            byte[] fieldsCode = new byte[length];
            input.readBytes(fieldsCode);

            String key = getKey(getType(), names, fieldsCode);
            readMetaInfo = ApplicationShareData.readMetaInfoMap.get(key);
            if (readMetaInfo == null) {
                readMetaInfo = buildReadMetaInfo(getType(), names, fieldsCode);
                //add to global
                ApplicationShareData.readMetaInfoMap.put(key, readMetaInfo);
            }
            //local point to global ReadMetaInfo
            sessionData.readNameToMeta.put(getType(), readMetaInfo);
        } else if (flag == no_fields_flag) {
            readMetaInfo = sessionData.readNameToMeta.get(getType());

            //when a array's element type is final.not write class name for element
            //so we can use the cacheFields instead
            if (null == readMetaInfo) {
                CachedField[] fields = getFields();
                kryo.getGraphContext().put(this, fields);
                return fields;
            }
            names = readMetaInfo.fields;
            length = names == null ? 0 : names.length;
        }

        //if layout all same.no need do binary search again.
        CachedField[] fields = readMetaInfo.sameLayout ? getFields() : matchFieldsQuick(length, readMetaInfo);
        kryo.getGraphContext().put(this, fields);
        fieldContext.readMetaInfo = readMetaInfo;
        return fields;
    }

    private ReadMetaInfo buildReadMetaInfo(Class type, String[] names, byte[] fieldsCode) {
        int length = names.length;
        ReadMetaInfo readMetaInfo = new ReadMetaInfo(type, fieldsCode);
        readMetaInfo.fields = names;
        //readMetaInfo.fieldsDefaultType = fieldsDefaultType;
        readMetaInfo.varLenFieldNum = getVarLenFieldNum(fieldsCode);
        CachedField[] allFields = getFields();
        int foundMatchFieldNum = 0;
        readMetaInfo.fieldIndexMap = new int[length];
        outer:
        for (int i = 0; i < names.length; i++) {
            String schemaName = names[i];
            readMetaInfo.fieldIndexMap[i] = -1;
            for (int ii = 0, nn = allFields.length; ii < nn; ii++) {
                if (allFields[ii].getName().equals(schemaName)) {
                    foundMatchFieldNum++;
                    readMetaInfo.fieldIndexMap[i] = ii;
                    continue outer;
                }
            }
            if (TRACE) trace("kryo", "Unknown field will be skipped: " + schemaName);
        }

        if (length == allFields.length && foundMatchFieldNum == length) {
            readMetaInfo.sameLayout = true;
        }
        return readMetaInfo;
    }

    private int getVarLenFieldNum(byte[] fieldsCode) {
        int varLenFieldNum = 0;
        for (int i = 0; i < fieldsCode.length; i++) {
            if (fieldsCode[i] == KnownTypes.NOT_KNOWN) {
                varLenFieldNum++;
            }
        }
        return varLenFieldNum;
    }

    private CachedField[] matchFieldsQuick(int length, ReadMetaInfo classFieldPair) {
        CachedField[] fields = new CachedField[length];
        CachedField[] allFields = getFields();
        for (int i = 0; i < length; i++) {
            if (classFieldPair.fieldIndexMap[i] != -1) {
                fields[i] = allFields[classFieldPair.fieldIndexMap[i]];
            }
        }
        return fields;
    }

    public FastCachedCompatibleFieldSerializer.CachedCompatibleFieldSerializerConfig getCompatibleFieldSerializerConfig() {
        return config;
    }

    public static class CachedCompatibleFieldSerializerConfig extends FieldSerializerConfig {
        @Override
        public CachedCompatibleFieldSerializerConfig clone() {
            return (CachedCompatibleFieldSerializerConfig) super.clone(); // Clone is ok as we have only primitive fields.
        }
    }

    private static class QuickSkipSerializer extends Serializer {
        @Override
        public void write(Kryo kryo, Output output, Object o) {
            throw new KryoException("Should not reach here!");
        }

        @Override
        public Object read(Kryo kryo, Input input, Class aClass) {
            //add a fake object,so that reference id is same as write side
            kryo.reference(new Object());
            return null;
        }

        public void skip(Kryo kryo, Input input, int skipLen) {
            int before = input.position();
            kryo.readObjectOrNull(input, Object.class, this);
            int after = input.position();
            int skipCount = skipLen - (after - before);
            if (skipCount > 0) {
                input.skip(skipCount);
            }
        }
    }

    private String getKey(Class type, String[] fields, byte[] fieldsCode) {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(fields);
        result = 31 * result + Arrays.hashCode(fieldsCode);
        return type.getName() + result;
    }

    private static class ReadFieldContext {
        ReadMetaInfo readMetaInfo;
    }

}
