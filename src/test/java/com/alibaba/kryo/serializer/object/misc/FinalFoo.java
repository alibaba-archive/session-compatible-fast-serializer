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
package com.alibaba.kryo.serializer.object.misc;

public final class FinalFoo {
    private long a;
    private String b;
    private int c;

    public FinalFoo(long a, String b, int c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public long getA() {
        return a;
    }

    public void setA(long a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public int getC() {
        return c;
    }

    public void setC(int c) {
        this.c = c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FinalFoo finalFoo = (FinalFoo) o;

        if (a != finalFoo.a) return false;
        if (c != finalFoo.c) return false;
        return b != null ? b.equals(finalFoo.b) : finalFoo.b == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (a ^ (a >>> 32));
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + c;
        return result;
    }
}
