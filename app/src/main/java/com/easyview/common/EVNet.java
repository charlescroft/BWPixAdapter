package com.easyview.common;

/* loaded from: classes.dex */
public class EVNet {
    public static native int Login(String str, int i, String str2, String str3);

    public static native int Read(int i, byte[] bArr, int i2, int i3, int i4);

    static {
        try {
            System.loadLibrary("evnet");
        } catch (UnsatisfiedLinkError ule) {
            System.out.println("loadLibrary evnet lib," + ule.getMessage());
        }
    }
}
