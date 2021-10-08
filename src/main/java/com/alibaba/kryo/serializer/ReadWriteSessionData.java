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

import com.alibaba.kryo.serializer.annotation.SessionLevelShare;
import com.esotericsoftware.kryo.kryo5.util.IdentityObjectIntMap;
import com.esotericsoftware.kryo.kryo5.util.IntMap;
import com.esotericsoftware.kryo.kryo5.util.ObjectMap;

import java.util.HashSet;
import java.util.Set;

@SessionLevelShare
public class ReadWriteSessionData {
    final IdentityObjectIntMap<Class> writeClassToNameId = new IdentityObjectIntMap<>();
    int writeNextNameId = 0;

    final IntMap<Class> readNameIdToClass = new IntMap<>();
    final ObjectMap<Class, ReadMetaInfo> readNameToMeta = new ObjectMap();

    final Set<Class> toWrittenFieldClassSet = new HashSet<>();

    ReadWriteSessionData() {
    }

    void resetWriteData() {
        toWrittenFieldClassSet.clear();
    }
}
