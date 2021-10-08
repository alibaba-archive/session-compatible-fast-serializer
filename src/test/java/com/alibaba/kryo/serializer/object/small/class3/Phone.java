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

public class Phone {
    public String brand;
    public String color;
    public int weight;
    public Network network;

    public static Phone create() {
        Phone phone = new Phone();
        phone.brand = "Nokia";
        phone.color = "sliver";
        phone.weight = 100;
        Network2G network2G = new Network2G();
        network2G.mhz = 1800;
        phone.network = network2G;
        return phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Phone phone = (Phone) o;

        if (weight != phone.weight) return false;
        if (brand != null ? !brand.equals(phone.brand) : phone.brand != null) return false;
        if (color != null ? !color.equals(phone.color) : phone.color != null) return false;
        return network != null ? network.equals(phone.network) : phone.network == null;
    }

    @Override
    public int hashCode() {
        int result = brand != null ? brand.hashCode() : 0;
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + weight;
        result = 31 * result + (network != null ? network.hashCode() : 0);
        return result;
    }
}
