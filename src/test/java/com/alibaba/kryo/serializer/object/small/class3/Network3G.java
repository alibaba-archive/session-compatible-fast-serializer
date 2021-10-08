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
package com.alibaba.kryo.serializer.object.small.class3;

public class Network3G extends Network{
    public String foo;
    public int bar;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Network3G network3G = (Network3G) o;

        if (bar != network3G.bar) return false;
        return foo != null ? foo.equals(network3G.foo) : network3G.foo == null;
    }

    @Override
    public int hashCode() {
        int result = foo != null ? foo.hashCode() : 0;
        result = 31 * result + bar;
        return result;
    }
}
