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
import com.esotericsoftware.kryo.kryo5.Registration;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.util.DefaultClassResolver;

import static com.esotericsoftware.kryo.kryo5.minlog.Log.*;
import static com.esotericsoftware.kryo.kryo5.util.Util.className;
import static com.esotericsoftware.kryo.kryo5.util.Util.pos;

class CachedClassResolver extends DefaultClassResolver {
    SessionKryo sessionKryo;

    public CachedClassResolver(SessionKryo sessionKryo) {
        this.sessionKryo = sessionKryo;
    }

    protected void writeName(Output output, Class type, Registration registration) {
        ReadWriteSessionData sessionData = sessionKryo.sessionData;
        output.writeByte(1); // NAME + 2
        int nameId = sessionData.writeClassToNameId.get(type, -1);
        if (nameId != -1) {
            if (TRACE)
                trace("kryo", "Write class name reference " + nameId + ": " + className(type) + pos(output.position()));
            output.writeVarInt(nameId, true);
            return;
        }

        // Only write the class name the first time encountered in object graph.
        if (TRACE) trace("kryo", "Write class name: " + className(type) + pos(output.position()));
        nameId = sessionData.writeNextNameId++;
        sessionData.writeClassToNameId.put(type, nameId);
        sessionData.toWrittenFieldClassSet.add(type);
        output.writeVarInt(nameId, true);
        if (registration.isTypeNameAscii())
            output.writeAscii(type.getName());
        else
            output.writeString(type.getName());
    }

    protected Registration readName(Input input) {
        ReadWriteSessionData sessionData = sessionKryo.sessionData;
        int nameId = input.readVarInt(true);
        Class typeClass = sessionData.readNameIdToClass.get(nameId);
        if (typeClass == null) {
            // Only read the class name the first time encountered in object graph.
            String className = input.readString();
            try {
                typeClass = Class.forName(className, false, kryo.getClassLoader());
            } catch (ClassNotFoundException ex) {
                // Fallback to Kryo's class loader.
                try {
                    typeClass = Class.forName(className, false, Kryo.class.getClassLoader());
                } catch (ClassNotFoundException ex2) {
                    throw new CachedKryoClassNotFoundException("Unable to find class: " + className, ex, className);
                }
            }
            sessionData.readNameIdToClass.put(nameId, typeClass);
            if (TRACE) trace("kryo", "Read class name: " + className + pos(input.position()));
        } else {
            if (TRACE)
                trace("kryo", "Read class name reference " + nameId + ": " + className(typeClass) + pos(input.position()));
        }
        return kryo.getRegistration(typeClass);
    }
}

