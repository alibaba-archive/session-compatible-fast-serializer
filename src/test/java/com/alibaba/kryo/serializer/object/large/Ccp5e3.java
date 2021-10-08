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

public class Ccp5e3 implements java.io.Serializable {
    private static final long serialVersionUID = -8293916219661274831L;

    Conq4 c5ez;
    public Conq4 getC5ez() { return this.c5ez; }
    public void setC5ez(Conq4 c5ez) { this.c5ez = c5ez; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Ccp5e3 ccp5e3 = (Ccp5e3) o;

        return c5ez != null ? c5ez.equals(ccp5e3.c5ez) : ccp5e3.c5ez == null;
    }

    @Override
    public int hashCode() {
        return c5ez != null ? c5ez.hashCode() : 0;
    }
}
