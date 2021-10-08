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
package com.alibaba.kryo.serializer.object.large;

public class Cd14xps7l1 implements java.io.Serializable {
    private static final long serialVersionUID = -5092092635509480938L;

    Object c2b;
    public Object getC2b() { return this.c2b; }
    public void setC2b(Object c2b) { this.c2b = c2b; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cd14xps7l1 that = (Cd14xps7l1) o;

        return c2b != null ? c2b.equals(that.c2b) : that.c2b == null;
    }

    @Override
    public int hashCode() {
        return c2b != null ? c2b.hashCode() : 0;
    }
}
