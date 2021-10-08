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

import java.util.List;

public class Cjw9 implements java.io.Serializable {
    private static final long serialVersionUID = -1380421186222792009L;

    Cyyqle11 c12o7m;
    long c43cou;
    List c44n8;
    public Cyyqle11 getC12o7m() { return this.c12o7m; }
    public void setC12o7m(Cyyqle11 c12o7m) { this.c12o7m = c12o7m; }
    public long getC43cou() { return this.c43cou; }
    public void setC43cou(long c43cou) { this.c43cou = c43cou; }
    public List getC44n8() { return this.c44n8; }
    public void setC44n8(List c44n8) { this.c44n8 = c44n8; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cjw9 cjw9 = (Cjw9) o;

        if (c43cou != cjw9.c43cou) return false;
        if (c12o7m != null ? !c12o7m.equals(cjw9.c12o7m) : cjw9.c12o7m != null) return false;
        return c44n8 != null ? c44n8.equals(cjw9.c44n8) : cjw9.c44n8 == null;
    }

    @Override
    public int hashCode() {
        int result = c12o7m != null ? c12o7m.hashCode() : 0;
        result = 31 * result + (int) (c43cou ^ (c43cou >>> 32));
        result = 31 * result + (c44n8 != null ? c44n8.hashCode() : 0);
        return result;
    }
}
