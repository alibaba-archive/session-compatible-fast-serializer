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

public class CreateOption {

    /**
     * an object's super class contain same name as subclass.
     * it should set to true.
     */
    boolean supportSameFieldNameWithSuper;

    /**
     * -1 mean no limit
     */
    int maxOutputBufferSize = -1;

    private CreateOption() {
    }

    public static CreateOption of() {
        return new CreateOption();
    }

    public CreateOption supportSameFieldNameWithSuper() {
        this.supportSameFieldNameWithSuper = true;
        return this;
    }

    public CreateOption maxOutputBufferSize(int maxOutputBufferSize) {
        this.maxOutputBufferSize = maxOutputBufferSize;
        return this;
    }
}
