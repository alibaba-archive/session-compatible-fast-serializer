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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

/**
 * yummy object used when skip some unknown fields.it consume data,but it will be discard.
 */
public class YummyObject {
    public boolean z1;
    public Boolean z2;
    public byte b1;
    public Byte b2;
    public char c1;
    public Character c2;
    public short s1;
    public Short s2;
    public int i1;
    public Integer i2;
    public long l1;
    public Long l2;
    public float f1;
    public Float f2;
    public double d1;
    public Double d2;
    public String string1;
    public BigInteger bigInteger1;
    public BigDecimal bigDecimal1;
    public Date date1;
    public Timestamp timestamp1;
    public java.sql.Date javaSqlDate1;
    public Time time1;
    public Currency currency1;
    public StringBuffer stringBuffer1;
    public StringBuilder stringBuilder1;
    public TimeZone timezone1;
    public Calendar calendar1;
    public Locale locale1;
    public Charset charset1;
    public URL url1;
}
