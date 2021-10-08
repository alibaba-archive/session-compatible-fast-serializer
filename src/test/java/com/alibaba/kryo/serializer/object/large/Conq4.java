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
import java.util.Map;

public class Conq4 implements java.io.Serializable {
    private static final long serialVersionUID = -9179820418020133138L;

    List c7ksz86or2lzazicrq3fd;
    Map c628sxp;
    boolean c633mn5;
    Cyyqle11 c6348;
    C9fvwjn6jj636 c637pi521m;
    public List getC7ksz86or2lzazicrq3fd() { return this.c7ksz86or2lzazicrq3fd; }
    public void setC7ksz86or2lzazicrq3fd(List c7ksz86or2lzazicrq3fd) { this.c7ksz86or2lzazicrq3fd = c7ksz86or2lzazicrq3fd; }
    public Map getC628sxp() { return this.c628sxp; }
    public void setC628sxp(Map c628sxp) { this.c628sxp = c628sxp; }
    public boolean getC633mn5() { return this.c633mn5; }
    public void setC633mn5(boolean c633mn5) { this.c633mn5 = c633mn5; }
    public Cyyqle11 getC6348() { return this.c6348; }
    public void setC6348(Cyyqle11 c6348) { this.c6348 = c6348; }
    public C9fvwjn6jj636 getC637pi521m() { return this.c637pi521m; }
    public void setC637pi521m(C9fvwjn6jj636 c637pi521m) { this.c637pi521m = c637pi521m; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Conq4 conq4 = (Conq4) o;

        if (c633mn5 != conq4.c633mn5) return false;
        if (c7ksz86or2lzazicrq3fd != null ? !c7ksz86or2lzazicrq3fd.equals(conq4.c7ksz86or2lzazicrq3fd) : conq4.c7ksz86or2lzazicrq3fd != null)
            return false;
        if (c628sxp != null ? !c628sxp.equals(conq4.c628sxp) : conq4.c628sxp != null) return false;
        if (c6348 != null ? !c6348.equals(conq4.c6348) : conq4.c6348 != null) return false;
        return c637pi521m != null ? c637pi521m.equals(conq4.c637pi521m) : conq4.c637pi521m == null;
    }

    @Override
    public int hashCode() {
        int result = c7ksz86or2lzazicrq3fd != null ? c7ksz86or2lzazicrq3fd.hashCode() : 0;
        result = 31 * result + (c628sxp != null ? c628sxp.hashCode() : 0);
        result = 31 * result + (c633mn5 ? 1 : 0);
        result = 31 * result + (c6348 != null ? c6348.hashCode() : 0);
        result = 31 * result + (c637pi521m != null ? c637pi521m.hashCode() : 0);
        return result;
    }
}
