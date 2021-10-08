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
package com.alibaba.kryo.serializer.object.small.class2;

import com.alibaba.kryo.serializer.object.misc.Foo;

import java.io.Serializable;

public class Hometown<T1, T2> extends Foo<T1> implements Serializable {

    private String province;

    private String address;

    private String city;

    private T1 t1;

    private int age;

    private long sales;

    private T2 t2;

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public T1 getT1() {
        return t1;
    }

    public void setT1(T1 t1) {
        this.t1 = t1;
    }

    public T2 getT2() {
        return t2;
    }

    public void setT2(T2 t2) {
        this.t2 = t2;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public long getSales() {
        return sales;
    }

    public void setSales(long sales) {
        this.sales = sales;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Hometown<?, ?> hometown = (Hometown<?, ?>) o;

        if (age != hometown.age) return false;
        if (sales != hometown.sales) return false;
        if (province != null ? !province.equals(hometown.province) : hometown.province != null) return false;
        if (address != null ? !address.equals(hometown.address) : hometown.address != null) return false;
        if (city != null ? !city.equals(hometown.city) : hometown.city != null) return false;
        if (t1 != null ? !t1.equals(hometown.t1) : hometown.t1 != null) return false;
        return t2 != null ? t2.equals(hometown.t2) : hometown.t2 == null;
    }

    @Override
    public int hashCode() {
        int result = province != null ? province.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (t1 != null ? t1.hashCode() : 0);
        result = 31 * result + age;
        result = 31 * result + (int) (sales ^ (sales >>> 32));
        result = 31 * result + (t2 != null ? t2.hashCode() : 0);
        return result;
    }
}
