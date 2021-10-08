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

import com.alibaba.kryo.serializer.annotation.ApplicationLevelShare;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationLevelShare
class ApplicationShareData {
    static final Map<Class, FieldsDefaultType> writtenField = new ConcurrentHashMap<>();

    static final Map<String, ReadMetaInfo> readMetaInfoMap = new ConcurrentHashMap<>();
}
