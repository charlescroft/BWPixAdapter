package com.easyview.struct;

import struct.StructClass;
import struct.StructField;

@StructClass
/* loaded from: classes.dex */
public class EV_CAPS_STRUCT {

    @StructField(order = 1)
    public byte[] caps = new byte[8];

    @StructField(order = 0)
    public int size;
}
