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

import java.io.Serializable;

public class People implements Serializable {

    private String name;

    private Hometown<String, String> hometown1;

    private Hometown<String, String> hometown2;

    private Height height;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Hometown getHometown1() {
        return hometown1;
    }

    public Hometown getHometown2() {
        return hometown2;
    }

    public People(String name) {
        this.name = name;
    }

    public static People create() {
        Hometown hometown = new Hometown<String, String>();
        hometown.setProvince("ZheJiang");
        hometown.setAddress("Hangzhou");
        hometown.setT1("org.apache.commons.lang3.reflect.FieldUtils.writeDeclaredFieldorg.apache.commons.lang3.reflect.FieldUtils.writeDeclaredFieldorg.apache.commons.lang3.reflect.FieldUtils.writeDeclaredFieldorg.apache.commons.lang3.reflect.FieldUtils.writeDeclaredFieldorg.apache.commons.lang3.reflect.FieldUtils.writeDeclaredField");
        hometown.setAge(100);
        hometown.setSales(123131);
        hometown.setT2("Yeah!");
        People people = new People("who?");
        people.setName("Who am i");
        people.setHometown1(hometown);
        people.setHometown2(hometown);
        people.setHeight(Height.LOW);
        return people;
    }

    public void setHometown1(Hometown<String, String> hometown1) {
        this.hometown1 = hometown1;
    }

    public void setHometown2(Hometown<String, String> hometown2) {
        this.hometown2 = hometown2;
    }

    public Height getHeight() {
        return height;
    }

    public void setHeight(Height height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        People people = (People) o;

        if (name != null ? !name.equals(people.name) : people.name != null) return false;
        if (hometown1 != null ? !hometown1.equals(people.hometown1) : people.hometown1 != null) return false;
        if (hometown2 != null ? !hometown2.equals(people.hometown2) : people.hometown2 != null) return false;
        return height == people.height;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (hometown1 != null ? hometown1.hashCode() : 0);
        result = 31 * result + (hometown2 != null ? hometown2.hashCode() : 0);
        result = 31 * result + (height != null ? height.hashCode() : 0);
        return result;
    }
}
